package com.gienyne.industrialedgegateway.persistence.ingestion;

import com.gienyne.industrialedgegateway.persistence.domain.Device;
import com.gienyne.industrialedgegateway.persistence.domain.DeviceMetric;
import com.gienyne.industrialedgegateway.persistence.domain.EntityStatus;
import com.gienyne.industrialedgegateway.persistence.domain.EdgeNode;
import com.gienyne.industrialedgegateway.persistence.domain.EventType;
import com.gienyne.industrialedgegateway.persistence.repository.DeviceMetricRepository;
import com.gienyne.industrialedgegateway.persistence.repository.DeviceRepository;
import com.gienyne.industrialedgegateway.persistence.repository.EdgeNodeRepository;
import org.eclipse.tahu.protobuf.SparkplugBProto.Payload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Objects;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;


@Service
public class SparkplugMessageProcessor{

    private static final Logger log = LoggerFactory.getLogger(SparkplugMessageProcessor.class);

    private static final String BDSEQ_METRIC = "bdSeq";
    private static final String REBIRTH_METRIC = "Node Control/Rebirth";

    private final IngestionService ingestionService;
    private final DeviceRepository deviceRepository;
    private final DeviceMetricRepository deviceMetricRepository;
    private final EdgeNodeRepository edgeNodeRepository;
    private final EventService eventService;
    private final SparkplugCommandPublisher commandPublisher;

    // True during the resynchronization window after an MQTT (re)connect.
    private volatile boolean synchronizing = false;


    public SparkplugMessageProcessor(IngestionService ingestionService, DeviceRepository deviceRepository, DeviceMetricRepository deviceMetricRepository, EdgeNodeRepository edgeNodeRepository, EventService eventService, SparkplugCommandPublisher commandPublisher){

        this.ingestionService = ingestionService;
        this.deviceRepository = deviceRepository;
        this.deviceMetricRepository = deviceMetricRepository;
        this.edgeNodeRepository = edgeNodeRepository;
        this.eventService = eventService;
        this.commandPublisher = commandPublisher;

    }


    public boolean isSynchronizing(){
        return synchronizing;
    }


    // ---- Synchronization -------------------------------------------------------------------

    /**
     * Previous ONLINE states are no longer confirmed after reconnect.
     * Keep known OFFLINE states unchanged.
     */
    @Transactional
    public void beginSynchronization(){

        synchronizing = true;

        int nodes = edgeNodeRepository.replaceStatus(EntityStatus.ONLINE, EntityStatus.UNKNOWN);
        int devices = deviceRepository.replaceStatus(EntityStatus.ONLINE, EntityStatus.UNKNOWN);

        log.info("Synchronization started: {} Edge Node(s) and {} device(s) set to UNKNOWN", nodes, devices);

    }


    /**
     * Ends the synchronization window.
     *
     * UNKNOWN states are deliberately preserved because the absence of a Birth is not proof of OFFLINE.
     */
    public void finishSynchronization(){

        synchronizing = false;

        long unresolvedDevices = deviceRepository.countByStatus(EntityStatus.UNKNOWN);

        log.info("Synchronization window closed: {} device(s) still UNKNOWN " + "(waiting for a Birth or Death)", unresolvedDevices);

    }


    // ---- Dispatch -------------------------------------------------------------------------------

    @Transactional
    public void process(SparkplugTopic topic, Payload payload){

        Instant time = payload.hasTimestamp() ? Instant.ofEpochMilli(payload.getTimestamp()) : Instant.now();

        switch (topic.messageType()){

            case "NBIRTH" -> onNodeBirth(topic, payload, time);

            case "NDEATH" -> onNodeDeath(topic, payload, time);

            case "NCMD" -> onNodeCommand(topic, payload, time);

            case "DBIRTH" -> onDeviceBirth(topic, payload, time);

            case "DDATA" -> onDeviceData(topic, payload);

            case "DDEATH" -> onDeviceDeath(topic, time);

            default -> log.debug("Ignoring Sparkplug message type {}", topic.messageType());

        }
    }

    // ---- Edge Node ----------------------------------------------------------------------
    private void onNodeBirth(SparkplugTopic topic, Payload payload, Instant time){

        long bdSeq = readBdSeq(payload);


        if(bdSeq < 0 || bdSeq > 255){
            log.warn("Ignoring NBIRTH with an invalid bdSeq");
            return;
        }


        EdgeNode node = edgeNodeRepository.findByGroupIdAndEdgeNodeId(topic.groupId(), topic.edgeNodeId()).orElseGet(
            () -> edgeNodeRepository.save(new EdgeNode(topic.groupId(), topic.edgeNodeId()))
        );

        /**
         * Same bdSeq on a node that was not proven OFFLINE means:
         * this is the same Edge Node session.
         *
         * This covers:
         *    - backend restart + rebirth;
         *    - in-session rebirth.
         *
         * Therefore it is confirmation, not a new EDGE_NODE_ONLINE occurrence.
         */
        boolean sameSession = Objects.equals(node.getBdSeq(), bdSeq) && node.getStatus() != EntityStatus.OFFLINE;

        node.setBdSeq(bdSeq);
        node.setStatus(EntityStatus.ONLINE);

        if (!sameSession){
            record(time, EventType.EDGE_NODE_ONLINE, topic, null, "Edge Node Online");
        }
    }


    private void onNodeDeath(SparkplugTopic topic, Payload payload, Instant time){

        long bdSeq = readBdSeq(payload);

        EdgeNode node = edgeNodeRepository.findByGroupIdAndEdgeNodeId(topic.groupId(), topic.edgeNodeId()).orElse(null);

        /**
         * A stale NDEATH from an older session must not bring down the current session.
         */
        if(node == null || !Objects.equals(node.getBdSeq(), bdSeq) || node.getStatus() == EntityStatus.OFFLINE){

            log.info("Ignoring NDEATH (bdSeq {}): no matching live session", bdSeq);
            return;
        }

        node.setStatus(EntityStatus.OFFLINE);

        record(time, EventType.EDGE_NODE_OFFLINE, topic, null, "Edge Node Offline");


        /**
         * NDEATH is proof that every device belonging to this Edge Node
         * is no longer reachable through that session.
         */
        for( Device device : deviceRepository.findByGroupIdAndEdgeNodeId(topic.groupId(), topic.edgeNodeId())){

            EntityStatus previous = device.getStatus();

            device.setStatus(EntityStatus.OFFLINE);

            if (previous == EntityStatus.ONLINE){
                record(time, EventType.DEVICE_OFFLINE, topic, device.getDeviceId(), "Device Offline");
            }
        }
    }


    private void onNodeCommand(SparkplugTopic topic, Payload payload, Instant time){

        boolean rebirth = payload.getMetricsList().stream().anyMatch(

            metric -> REBIRTH_METRIC.equals(metric.getName()) &&
            metric.getValueCase() == Payload.Metric.ValueCase.BOOLEAN_VALUE
            && metric.getBooleanValue()

        );

        if (rebirth){

            record(time, EventType.REBIRTH_REQUESTED, topic, null, "Rebirth Requested");

        }
    }


    // ---- Device ----------------------------------------------------------------------------

    private void onDeviceBirth(SparkplugTopic topic, Payload payload, Instant time){

        /**
         * Read the previous status BEFORE ingest().
         *
         * null means that this device has never been seen before.
         * This is different from UNKNOWN, which is a real synchronization status.
         */
        EntityStatus previous = deviceRepository.findById(topic.deviceId()).map(Device::getStatus).orElse(null);

        /**
         * DBIRTH is allowed to create the Device and store its Birth values.
         */
        ingestionService.ingest(topic, payload);

        Device device = deviceRepository.findById(topic.deviceId()).orElseThrow();

        // DBIRTH always confirms that the device is ONLINE.
        device.setStatus(EntityStatus.ONLINE);

        // DBIRTH also give what metrics the device declares.
        syncDeviceMetrics(device.getDeviceId(), payload);


        /**
         * Event only for:
         *  - a brand-new device;
         *  - a real OFFLINE -> ONLINE transition.
         *
         * UNKNOWN -> ONLINE is only synchronization.
         */
        if(previous == null || previous == EntityStatus.OFFLINE){

            record(time, EventType.DEVICE_ONLINE, topic, device.getDeviceId(), "Device Online");

        }
    }


    private void onDeviceData(SparkplugTopic topic, Payload payload){

        EntityStatus status = deviceRepository.findById(topic.deviceId()).map(Device::getStatus).orElse(EntityStatus.UNKNOWN);

        /**
         * DDATA is accepted only for a confirmed ONLINE device.
         */
        if(status != EntityStatus.ONLINE){

            boolean requested = commandPublisher.requestRebirth(topic.groupId(), topic.edgeNodeId());

            log.debug("Dropped DDATA from '{}' (status {}), rebirth {}", topic.deviceId(), status, requested  ? "requested" : "skipped (throttled or disconnected)");

            // Drop DDATA until DBIRTH confirms the device is ONLINE.
            return;
        }

        // Only confirmed ONLINE devices reach IngestionService.
        ingestionService.ingest(topic, payload);
    }


    private void onDeviceDeath(SparkplugTopic topic, Instant time){

        deviceRepository.findById(topic.deviceId()).ifPresent(

             device -> {

                EntityStatus previous = device.getStatus();
                device.setStatus(EntityStatus.OFFLINE);

                if(previous == EntityStatus.ONLINE){
                    record(time, EventType.DEVICE_OFFLINE, topic, device.getDeviceId(), "Device Offline");
                }
             }
        );
    }



    // ---- Device metrics --------------------------------------------------------------------

    private void syncDeviceMetrics(String deviceId, Payload payload){

        Map<String, DeviceMetric> known = deviceMetricRepository.findByDeviceId(deviceId).stream().collect(

            Collectors.toMap( DeviceMetric::getMetricName, Function.identity())

        );

        for(Payload.Metric metric : payload.getMetricsList()){

            String datatype = datatypeOf(metric);

            if(REBIRTH_METRIC.equals(metric.getName()) || datatype == null){
                continue;
            }

            String unit = ingestionService.extractUnit(deviceId, metric);

            DeviceMetric existing = known.get(metric.getName());

            if(existing == null){

                deviceMetricRepository.save(
                    new DeviceMetric(deviceId, metric.getName(), datatype, unit)
                );

            }
            else{

                existing.update(datatype, unit);
            }
        }
    }


    //--------------------------------------------------------


    private void record(Instant time, EventType type, SparkplugTopic topic, String deviceId, String message){

        eventService.record(time, type, topic.groupId(), topic.edgeNodeId(), deviceId, message);

    }

    private static long readBdSeq(Payload payload){

        for (Payload.Metric metric : payload.getMetricsList()){

            if(BDSEQ_METRIC.equals(metric.getName()) && metric.getValueCase() == Payload.Metric.ValueCase.LONG_VALUE){
                return metric.getLongValue();
            }
        }

        return -1L;
    }


    private static String datatypeOf(Payload.Metric metric){

        return switch (metric.getValueCase()){
            case BOOLEAN_VALUE -> "Boolean";
            case INT_VALUE     -> "Integer";
            case DOUBLE_VALUE  -> "Double";
            case STRING_VALUE  -> "String";

            default -> null;

        };


    }
}
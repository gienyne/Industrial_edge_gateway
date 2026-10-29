package com.gienyne.industrialedgegateway.persistence.ingestion;


import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.gienyne.industrialedgegateway.persistence.domain.Device;
import com.gienyne.industrialedgegateway.persistence.domain.DeviceMetric;
import com.gienyne.industrialedgegateway.persistence.domain.EdgeNode;
import com.gienyne.industrialedgegateway.persistence.domain.EntityStatus;
import com.gienyne.industrialedgegateway.persistence.domain.EventType;
import com.gienyne.industrialedgegateway.persistence.repository.DeviceMetricRepository;
import com.gienyne.industrialedgegateway.persistence.repository.DeviceRepository;
import com.gienyne.industrialedgegateway.persistence.repository.EdgeNodeRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.eclipse.tahu.protobuf.SparkplugBProto.Payload;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;



@ExtendWith(MockitoExtension.class)
class SparkplugMessageProcessorTest{

    private static final String GROUP = "SFM";
    private static final String NODE = "IndustrialEdgeGateway";
    private static final String DEVICE = "Bewaesserungspumpe";

    @Mock
    private IngestionService ingestionService;

    @Mock
    private DeviceRepository deviceRepository;

    @Mock
    private DeviceMetricRepository deviceMetricRepository;

    @Mock
    private EdgeNodeRepository edgeNodeRepository;

    @Mock
    private EventService eventService;

    @Mock
    private SparkplugCommandPublisher commandPublisher;

    private SparkplugMessageProcessor processor;


    @BeforeEach
    void setUp(){

        processor = new SparkplugMessageProcessor(ingestionService,
            deviceRepository,
            deviceMetricRepository,
            edgeNodeRepository,
            eventService,
            commandPublisher
        );
    }


    private static SparkplugTopic topic(String messageType, String deviceId){
        return new SparkplugTopic(GROUP, messageType, NODE, deviceId);
    }

    private static Payload payload(Long seq, Payload.Metric... metrics){

        Payload.Builder builder = Payload.newBuilder().setTimestamp(1_700_000_000_000L);

        if(seq != null){
            builder.setSeq(seq);
        }

        for(Payload.Metric metric : metrics){
            builder.addMetrics(metric);
        }

        return builder.build();
    }

    private static Payload.Metric bdSeqMetric(long value){
        return Payload.Metric.newBuilder().setName("bdSeq").setLongValue(value).build();
    }

    private static Payload.Metric booleanMetric(String name, boolean value){
        return Payload.Metric.newBuilder().setName(name).setBooleanValue(value).build();
    }

    private static Device deviceWithStatus(EntityStatus status){

        Device device = new Device(DEVICE, GROUP, NODE, Instant.now());
        device.setStatus(status);

        return device;
    }


    private static EdgeNode nodeWith(long bdSeq, EntityStatus status){

        EdgeNode node = new EdgeNode(GROUP, NODE);
        node.setBdSeq(bdSeq);
        node.setStatus(status);

        return node;
    }


    private void stubOnlineDevice(){

        lenient().when(deviceRepository.findById(DEVICE)).thenReturn(Optional.of(deviceWithStatus(EntityStatus.ONLINE)));

    }

    private void verifyNoSequenceGap(){

        verify(eventService, never()).record(any(), eq(EventType.SEQUENCE_GAP), any(), any(), any(), any());
        verify(commandPublisher, never()).requestRebirth(any(), any());

    }



    // --- seq -------------------------------------------------------

    @Test
    void messagesInOrder_doNotCreateAGapEvent(){

        stubOnlineDevice();

        processor.process(topic("NBIRTH", null), payload(0L));
        processor.process(topic("DDATA", DEVICE), payload(1L));
        processor.process(topic("DDATA", DEVICE), payload(2L));

        verifyNoSequenceGap();

    }


    @Test
    void missingSequenceNumber_requestsRebirth(){

        stubOnlineDevice();

        processor.process(topic("NBIRTH", null), payload(0L));
        processor.process(topic("DDATA", DEVICE), payload(1L));
        processor.process(topic("DDATA", DEVICE), payload(3L));

        ArgumentCaptor<String> message = ArgumentCaptor.forClass(String.class);

        verify(eventService).record(any(Instant.class), eq(EventType.SEQUENCE_GAP), eq(GROUP), eq(NODE), isNull(), message.capture());
        verify(commandPublisher).requestRebirth(GROUP, NODE);

        assertThat(message.getValue()).contains("expected 2").contains("received 3");

    }


    @Test
    void sequence255CanBeFollowedBy0(){

        stubOnlineDevice();

        processor.process(topic("DDATA", DEVICE), payload(255L));
        processor.process(topic("DDATA", DEVICE), payload(0L));

        verifyNoSequenceGap();
    }


    @Test
    void missingNumbersAcrossWrapAround_areCounted(){

        stubOnlineDevice();

        processor.process(topic("DDATA", DEVICE), payload(254L));
        processor.process(topic("DDATA", DEVICE), payload(1L));

        ArgumentCaptor<String> message = ArgumentCaptor.forClass(String.class);

        verify(eventService).record(any(Instant.class), eq(EventType.SEQUENCE_GAP), eq(GROUP), eq(NODE), isNull(), message.capture());

        assertThat(message.getValue()).contains("expected 255").contains("received 1").contains("2 message").contains("possibly lost");

    }


    @Test
    void duplicateSequenceNumber_isIgnored(){

        stubOnlineDevice();

        processor.process(topic("DDATA", DEVICE), payload(5L));
        processor.process(topic("DDATA", DEVICE), payload(5L));

        verifyNoSequenceGap();

    }


    @Test
    void firstSequenceNumber_doesNotCreateAGap(){

        stubOnlineDevice();

        processor.process(topic("DDATA", DEVICE), payload(7L));

        verifyNoSequenceGap();

    }


    @Test
    void nodeBirth_resetsTheSequence(){

        stubOnlineDevice();

        processor.process(topic("DDATA", DEVICE), payload(10L));
        processor.process(topic("NBIRTH", null), payload(0L));
        processor.process(topic("DDATA", DEVICE), payload(1L));

        verifyNoSequenceGap();

    }


    @Test
    void reconnect_resetsTheSequence(){

        stubOnlineDevice();

        processor.process(topic("DDATA", DEVICE), payload(5L));
        processor.beginSynchronization(); // MQTT reconnected: messages may have been missed
        processor.process(topic("DDATA", DEVICE), payload(9L));

        verifyNoSequenceGap();

    }


    @Test
    void backendCommands_doNotAffectTheSequence(){

        stubOnlineDevice();

        processor.process(topic("DDATA", DEVICE), payload(1L));
        processor.process(topic("NCMD", null), payload(99L));
        processor.process(topic("DDATA", DEVICE), payload(2L));

        verifyNoSequenceGap();

    }


    @Test
    void messageWithoutSeq_doesNotCreateAGap(){

        stubOnlineDevice();

        processor.process(topic("DDATA", DEVICE), payload(1L));
        processor.process(topic("DDATA", DEVICE), payload(null));
        processor.process(topic("DDATA", DEVICE), payload(2L));

        verifyNoSequenceGap();
    }


    // --- edge node --------------------------------------------

    @Test
    void newNodeBirth_setsNodeOnline(){

        EdgeNode node = nodeWith(6, EntityStatus.OFFLINE);
        when(edgeNodeRepository.findByGroupIdAndEdgeNodeId(GROUP, NODE)).thenReturn(Optional.of(node));

        processor.process(topic("NBIRTH", null), payload(null, bdSeqMetric(7)));

        assertThat(node.getBdSeq()).isEqualTo(7L);
        assertThat(node.getStatus()).isEqualTo(EntityStatus.ONLINE);

        verify(eventService).record(any(Instant.class), eq(EventType.EDGE_NODE_ONLINE), eq(GROUP), eq("IndustrialEdgeGateway"), isNull(), eq("Edge Node Online"));

    }


    @Test
    void repeatedNodeBirth_doesNotCreateAnEvent(){

        EdgeNode node = nodeWith(7, EntityStatus.ONLINE);
        when(edgeNodeRepository.findByGroupIdAndEdgeNodeId(GROUP, NODE)).thenReturn(Optional.of(node));

        processor.process(topic("NBIRTH", null), payload(null, bdSeqMetric(7)));

        assertThat(node.getStatus()).isEqualTo(EntityStatus.ONLINE);

        verify(eventService, never()).record(any(), eq(EventType.EDGE_NODE_ONLINE), any(), any(), any(), any());

    }


    @Test
    void nodeBirthWithoutBdSeq_isIgnored(){

        processor.process(topic("NBIRTH", null), payload(null));

        verifyNoInteractions(edgeNodeRepository);
        verifyNoInteractions(eventService);

    }


    @Test
    void nodeDeath_setsNodeAndDevicesOffline(){

        EdgeNode node = nodeWith(5, EntityStatus.ONLINE);
        Device device = deviceWithStatus(EntityStatus.ONLINE);

        when(edgeNodeRepository.findByGroupIdAndEdgeNodeId(GROUP, NODE)).thenReturn(Optional.of(node));
        when(deviceRepository.findByGroupIdAndEdgeNodeId(GROUP, NODE)).thenReturn(List.of(device));

        processor.process(topic("NDEATH", null), payload(null, bdSeqMetric(5)));

        assertThat(node.getStatus()).isEqualTo(EntityStatus.OFFLINE);
        assertThat(device.getStatus()).isEqualTo(EntityStatus.OFFLINE);

        verify(eventService).record(any(Instant.class), eq(EventType.EDGE_NODE_OFFLINE), eq(GROUP), eq(NODE), isNull(), eq("Edge Node Offline"));
        verify(eventService).record(any(Instant.class), eq(EventType.DEVICE_OFFLINE), eq(GROUP), eq(NODE), eq(DEVICE), eq("Device Offline"));

    }


    @Test
    void oldNodeDeath_isIgnored(){

        EdgeNode node = nodeWith(5, EntityStatus.ONLINE);
        when(edgeNodeRepository.findByGroupIdAndEdgeNodeId(GROUP, NODE)).thenReturn(Optional.of(node));

        processor.process(topic("NDEATH", null), payload(null, bdSeqMetric(4)));

        assertThat(node.getStatus()).isEqualTo(EntityStatus.ONLINE);

        verifyNoInteractions(eventService);

    }


    // ---- device -------------------------------------------------

    @Test
    void newDeviceBirth_setsDeviceOnlineAndStoresMetrics(){

        Device created = deviceWithStatus(EntityStatus.UNKNOWN);

        // First lookup (before ingest): unknown device. Second lookup (after ingest): the created device.#
        when(deviceRepository.findById(DEVICE)).thenReturn(Optional.empty(), Optional.of(created));

        processor.process(topic("DBIRTH", DEVICE), payload(null, booleanMetric("pumpActive", true)));

        assertThat(created.getStatus()).isEqualTo(EntityStatus.ONLINE);

        verify(ingestionService).ingest(any(), any());
        verify(deviceMetricRepository).save(any(DeviceMetric.class));
        verify(eventService).record(any(Instant.class), eq(EventType.DEVICE_ONLINE), eq(GROUP), eq(NODE), eq(DEVICE), eq("Device Online"));

    }


    @Test
    void deviceBirth_afterOffline_setsDeviceOnline(){

        Device device = deviceWithStatus(EntityStatus.OFFLINE);
        when(deviceRepository.findById(DEVICE)).thenReturn(Optional.of(device));

        processor.process(topic("DBIRTH", DEVICE), payload(null, booleanMetric("pumpActive", true)));

        assertThat(device.getStatus()).isEqualTo(EntityStatus.ONLINE);

        verify(eventService).record(any(Instant.class), eq(EventType.DEVICE_ONLINE), any(), any(), eq(DEVICE), any());

    }


    @Test
    void deviceBirth_afterUnknown_doesNotCreateAnEvent(){

        Device device = deviceWithStatus(EntityStatus.UNKNOWN);
        when(deviceRepository.findById(DEVICE)).thenReturn(Optional.of(device));

        processor.process(topic("DBIRTH", DEVICE), payload(null, booleanMetric("pumpActive", true)));

        assertThat(device.getStatus()).isEqualTo(EntityStatus.ONLINE);

        verify(eventService, never()).record(any(), eq(EventType.DEVICE_ONLINE), any(), any(), any(), any());

    }


    @Test
    void dataFromUnknownDevice_isIgnoredAndRebirthIsRequested(){

        when(deviceRepository.findById(DEVICE)).thenReturn(Optional.of(deviceWithStatus(EntityStatus.UNKNOWN)));

        processor.process(topic("DDATA", DEVICE), payload(null, booleanMetric("pumpActive", true)));

        verify(ingestionService, never()).ingest(any(), any());
        verify(commandPublisher).requestRebirth(GROUP, NODE);


    }


    @Test
    void dataFromOnlineDevice_isStored(){

        when(deviceRepository.findById(DEVICE)).thenReturn(Optional.of(deviceWithStatus(EntityStatus.ONLINE)));
        processor.process(topic("DDATA", DEVICE), payload(null, booleanMetric("pumpActive", true)));

        verify(ingestionService).ingest(any(), any());
        verify(commandPublisher, never()).requestRebirth(any(), any());

    }


    @Test
    void deviceDeath_setsDeviceOffline(){

        Device device = deviceWithStatus(EntityStatus.ONLINE);
        when(deviceRepository.findById(DEVICE)).thenReturn(Optional.of(device));

        processor.process(topic("DDEATH", DEVICE), payload(null));

        assertThat(device.getStatus()).isEqualTo(EntityStatus.OFFLINE);

        verify(eventService).record(any(Instant.class), eq(EventType.DEVICE_OFFLINE), any(), any(), eq(DEVICE), eq("Device Offline"));

    }


    @Test
    void deviceDeathOfUnknownDevice_doesNotCreateAnEvent(){

        Device device = deviceWithStatus(EntityStatus.UNKNOWN);
        when(deviceRepository.findById(DEVICE)).thenReturn(Optional.of(device));

        processor.process(topic("DDEATH", DEVICE), payload(null));

        assertThat(device.getStatus()).isEqualTo(EntityStatus.OFFLINE);

        verify(eventService, never()).record(any(), eq(EventType.DEVICE_OFFLINE), any(), any(), any(), any());

    }



    // --- Synchronization ------------------------------------

    @Test
    void synchronization_setsOnlineStatesToUnknown(){

        processor.beginSynchronization();

        assertThat(processor.isSynchronizing()).isTrue();

        verify(edgeNodeRepository).replaceStatus(EntityStatus.ONLINE, EntityStatus.UNKNOWN);
        verify(deviceRepository).replaceStatus(EntityStatus.ONLINE, EntityStatus.UNKNOWN);

        processor.finishSynchronization();

        assertThat(processor.isSynchronizing()).isFalse();

    }
}
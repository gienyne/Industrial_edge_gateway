package com.gienyne.industrialedgegateway.persistence.ingestion;


import com.gienyne.industrialedgegateway.persistence.domain.Device;
import com.gienyne.industrialedgegateway.persistence.domain.Measurement;
import com.gienyne.industrialedgegateway.persistence.repository.DeviceRepository;
import com.gienyne.industrialedgegateway.persistence.repository.MeasurementRepository;
import org.eclipse.tahu.protobuf.SparkplugBProto.Payload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.time.Instant;
import java.util.List;


@Service
public class IngestionService{

    private static final Logger log = LoggerFactory.getLogger(IngestionService.class);

    private static final String REBIRTH_METRIC_NAME = "Node Control/Rebirth";

    private final DeviceRepository deviceRepository;
    private final MeasurementRepository measurementRepository;


    public IngestionService(DeviceRepository deviceRepository, MeasurementRepository measurementRepository){

        this.deviceRepository = deviceRepository;
        this.measurementRepository = measurementRepository;

    }

    @Transactional
    public void ingest(SparkplugTopic topic, Payload payload){

        if(!topic.isDataMessage()){
                return;
        }

        Instant now = Instant.now();

        Device device = deviceRepository.findById(topic.deviceId()).map(
            existing->{ 
                existing.touch(now);
                return existing;
            }).orElseGet(() -> new Device(
                topic.deviceId(),
                topic.groupId(),
                topic.edgeNodeId(),
                now
            ));

        deviceRepository.save(device);

        List<Measurement> measurements = payload.getMetricsList().stream().filter(
            metric -> !REBIRTH_METRIC_NAME.equals(metric.getName())).map(
            metric -> toMeasurement(topic.deviceId(), metric)).filter(
            measurement -> measurement != null).toList();

        measurementRepository.saveAll(measurements);

    }
            
        private Measurement toMeasurement( String deviceId, Payload.Metric metric){

            Instant time = Instant.ofEpochMilli(metric.getTimestamp());
            String name = metric.getName();
            String unit = extractUnit(deviceId, metric);

            return switch (metric.getValueCase()){

                case BOOLEAN_VALUE -> Measurement.ofBoolean(
                    time,
                    deviceId,
                    name,
                    unit,
                    metric.getBooleanValue()
                );

                case INT_VALUE -> Measurement.ofInteger(
                    time,
                    deviceId,
                    name,
                    unit,
                    metric.getIntValue()
                );

                case DOUBLE_VALUE -> Measurement.ofDouble(
                    time,
                    deviceId,
                    name,
                    unit,
                    metric.getDoubleValue()
                );

                case STRING_VALUE -> Measurement.ofString(
                    time,
                    deviceId,
                    name,
                    unit,
                    metric.getStringValue()
                );

                default -> {
                    log.warn (
                        "Skipping metric '{}' on device '{}': unsupported value case {}",
                        name,
                        deviceId,
                        metric.getValueCase()
                    );

                    yield null;

                }
            };
        }


        private String extractUnit(String deviceId, Payload.Metric metric){

            if(!metric.hasProperties()){
                return null;
            }

            Payload.PropertySet properties = metric.getProperties();

            for (int i = 0; i < properties.getKeysCount(); i++){

                if ("unit".equals(properties.getKeys(i)) && i < properties.getValuesCount()){

                    Payload.PropertyValue value = properties.getValues(i);

                    if (value.hasStringValue()){
                        return value.getStringValue();
                    }

                    log.warn("Unit property for metric '{}' on device '{}' is not a string",  metric.getName(), deviceId);

                    return null;
                }
            }

            return null;
        }

}

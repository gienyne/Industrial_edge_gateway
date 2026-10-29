package com.gienyne.industrialedgegateway.persistence.metric;


import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;


import com.gienyne.industrialedgegateway.persistence.domain.Device;
import com.gienyne.industrialedgegateway.persistence.domain.DeviceMetric;
import com.gienyne.industrialedgegateway.persistence.domain.Measurement;
import com.gienyne.industrialedgegateway.persistence.metric.models.api.ApiHistoryPoint;
import com.gienyne.industrialedgegateway.persistence.metric.models.api.ApiMetricHistoryResponse;
import com.gienyne.industrialedgegateway.persistence.repository.DeviceMetricRepository;
import com.gienyne.industrialedgegateway.persistence.repository.DeviceRepository;
import com.gienyne.industrialedgegateway.persistence.repository.MeasurementRepository;
import com.gienyne.industrialedgegateway.persistence.shared.error.ResourceNotFoundException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;


@ExtendWith(MockitoExtension.class)
class MetricHistoryServiceTest{

    private static final String DEVICE = "pump";
    private static final String METRIC = "tankLevel";

    private static final Instant TO = Instant.parse("2026-10-22T12:00:00Z");
    private static final Instant FROM = TO.minusSeconds(3600);


    @Mock
    private DeviceRepository deviceRepository;

    @Mock
    private DeviceMetricRepository deviceMetricRepository;

    @Mock
    private MeasurementRepository measurementRepository;


    private MetricHistoryService service;


    @BeforeEach
    void setUp(){
        service = new MetricHistoryService(deviceRepository, deviceMetricRepository, measurementRepository);
    }



    private void stubKnownMetric(String dataType, String unit){

        when(deviceRepository.findById(DEVICE)).thenReturn(Optional.of(new Device(DEVICE, "SFM", "IndustrialEdgeGateway", Instant.now())));

        when(deviceMetricRepository.findByDeviceId(DEVICE)).thenReturn(List.of(new DeviceMetric(DEVICE, METRIC, dataType, unit)));

    }


    private static MeasurementRepository.NumericBucket bucket(long millis, double average, double minimum, double maximum, long count){

        return new MeasurementRepository.NumericBucket(){

            @Override
            public Long getBucketTimestampMillis(){
                return millis;
            }

            @Override
            public Double getAverageValue(){
                return average;
            }

            @Override
            public Double getMinimumValue(){
                return minimum;
            }

            @Override
            public Double getMaximumValue(){
                return maximum;
            }

            @Override
            public Long getSampleCount(){
                return count;
            }

        };

    }


    private static MeasurementRepository.StateChange stateChange(long millis, String value){

        return new MeasurementRepository.StateChange(){

            @Override
            public Long getTimestampMillis(){
                return millis;
            }

            @Override
            public String getValue(){
                return value;
            }

        };
    }


    private static void assertStatus(Throwable thrown, int expected){

        assertThat(thrown).isInstanceOfSatisfying(ResponseStatusException.class,
            exception -> assertThat(exception.getStatusCode().value()).isEqualTo(expected));


    }


    @Test
    void getHistory_withABlankDeviceOrMetric_isRejected(){

        assertThatThrownBy(() -> service.getHistory(" ", METRIC, FROM, TO))
            .satisfies(thrown -> assertStatus(thrown, 400));


        assertThatThrownBy(() -> service.getHistory(DEVICE, "", FROM, TO))
            .satisfies(thrown -> assertStatus(thrown, 400));

        
        verifyNoInteractions(deviceRepository, deviceMetricRepository, measurementRepository);

    }


    @Test
    void getHistory_withMissingDates_isRejected(){

        assertThatThrownBy(() -> service.getHistory(DEVICE, METRIC, null, TO))
            .satisfies(thrown -> assertStatus(thrown, 400));

        
        assertThatThrownBy(() -> service.getHistory(DEVICE, METRIC, FROM, null))
            .satisfies(thrown -> assertStatus(thrown, 400));


        verifyNoInteractions(deviceRepository, deviceMetricRepository, measurementRepository);

    }


    @Test
    void getHistory_withAStartAfterTheEnd_isRejected(){

        assertThatThrownBy(() -> service.getHistory(DEVICE, METRIC, TO, FROM))
            .satisfies(thrown -> assertStatus(thrown, 400));


        verifyNoInteractions(deviceRepository, deviceMetricRepository, measurementRepository);

    }


    @Test
    void getHistory_forAnUnknownDevice_isNotFound(){

        when(deviceRepository.findById(DEVICE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getHistory(DEVICE, METRIC, FROM, TO))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Device was not found");

    }


    @Test
    void getHistory_forAMetricTheDeviceDoesNotHave_isNotFound(){

        when(deviceRepository.findById(DEVICE)).thenReturn(Optional.of(new Device(DEVICE, "SFM", "IndustrialEdgeGateway", Instant.now())));

        when(deviceMetricRepository.findByDeviceId(DEVICE)).thenReturn(List.of(new DeviceMetric(DEVICE, "pumpActive", "Boolean", null)));

        assertThatThrownBy(() -> service.getHistory(DEVICE, METRIC, FROM, TO))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Metric was not found");

    }


    @Test
    void getHistory_forAnUnsupportedDataType_isRejectedWith422(){

        stubKnownMetric("kilogram", null);

        assertThatThrownBy(() -> service.getHistory(DEVICE, METRIC, FROM, TO))
            .satisfies(thrown -> assertStatus(thrown, 422));

    }


    @Test
    void numericHistory_returnsBucketAverages_withMinMax_andRealStatistics(){

        stubKnownMetric("Double", "L");

        when(measurementRepository.countByDeviceIdAndMetricNameAndTimeBetween(DEVICE, METRIC, FROM, TO)).thenReturn(8L);

        // One hour -> the service picks 10-second buckets (about 500 points at most).
        when(measurementRepository.aggregateNumericMeasurements(DEVICE, METRIC, FROM, TO, 10)).thenReturn(List.of(
            bucket(1_000L, 10.0, 8.0, 12.0, 2L),
            bucket(2_000L, 20.0, 18.0, 22.0, 6L)
        ));


        when(measurementRepository.findFirstByDeviceIdAndMetricNameAndTimeBetweenOrderByTimeDesc(DEVICE, METRIC, FROM, TO))
            .thenReturn(Optional.of(Measurement.ofDouble(TO, DEVICE, METRIC, "L", 21.5)));

        
        ApiMetricHistoryResponse response = service.getHistory(DEVICE, METRIC, FROM, TO);


        assertThat(response.aggregation()).isEqualTo("time_bucket_average");
        assertThat(response.bucketSeconds()).isEqualTo(10L);
        assertThat(response.dataType()).isEqualTo("Double");
        assertThat(response.unit()).isEqualTo("L");
        assertThat(response.measurementCount()).isEqualTo(8L);


        // Each point: the average of the bucket, plus the real lowest and highest value inside it.

        assertThat(response.points()).hasSize(2);

        ApiHistoryPoint first = response.points().get(0);

        assertThat(first.timestamp()).isEqualTo(Instant.ofEpochMilli(1_000L));

        assertThat(first.value()).isEqualTo(10.0);
        assertThat(first.min()).isEqualTo(8.0);
        assertThat(first.max()).isEqualTo(12.0);

        // Statistics are computed from the buckets: lowest, highest, and an average weighted by sample count.

        assertThat(response.statistics().min()).isEqualTo(8.0);
        assertThat(response.statistics().max()).isEqualTo(22.0);
        assertThat(response.statistics().average()).isEqualTo(17.5); // (10*2 + 20*6) / 8

        assertThat(response.statistics().latest()).isEqualTo(21.5);

    }


    @Test
    void numericHistory_withoutData_hasNoPointsAndNoStatistics(){

        stubKnownMetric("Integer", "-");

        when(measurementRepository.aggregateNumericMeasurements(DEVICE, METRIC, FROM, TO, 10)).thenReturn(List.of());

        when(measurementRepository.findFirstByDeviceIdAndMetricNameAndTimeBetweenOrderByTimeDesc(DEVICE, METRIC, FROM, TO))
            .thenReturn(Optional.empty());


        ApiMetricHistoryResponse response = service.getHistory(DEVICE, METRIC, FROM, TO);

        assertThat(response.points()).isEmpty();
        assertThat(response.unit()).isNull(); // "-" means "no unit"
        assertThat(response.statistics().min()).isNull();
        assertThat(response.statistics().max()).isNull();
        assertThat(response.statistics().average()).isNull();
        assertThat(response.statistics().latest()).isNull();

    }


    @Test
    void longPeriods_useBiggerBuckets(){

        Instant start = TO.minusSeconds(7 * 24 * 3600L); // 7 days: 604800 s / 500 points -> 1210 s -> 1800 s buckets

        stubKnownMetric("Double", "L");

        when(measurementRepository.aggregateNumericMeasurements(DEVICE, METRIC, start, TO, 1800)).thenReturn(List.of());

        when(measurementRepository.findFirstByDeviceIdAndMetricNameAndTimeBetweenOrderByTimeDesc(DEVICE, METRIC, start, TO))
            .thenReturn(Optional.empty());

        
        ApiMetricHistoryResponse response = service.getHistory(DEVICE, METRIC, start, TO);

        assertThat(response.bucketSeconds()).isEqualTo(1800L);
        
    }


    @Test
    void booleanHistory_returnsStateChanges_withoutStatistics(){

        stubKnownMetric("Boolean", null);

        long beforeStart = FROM.minusSeconds(60).toEpochMilli();
        long afterStart = FROM.plusSeconds(120).toEpochMilli();

        when(measurementRepository.findStateChanges(DEVICE, METRIC, FROM, TO)).thenReturn(List.of(stateChange(beforeStart, "false"), stateChange(afterStart, "true")));

        ApiMetricHistoryResponse response = service.getHistory(DEVICE, METRIC, FROM, TO);


        assertThat(response.aggregation()).isEqualTo("state_changes");
        assertThat(response.bucketSeconds()).isNull();
        assertThat(response.points()).hasSize(2);


        // A state from before the period is drawn from the start of the period.

        assertThat(response.points().get(0).timestamp()).isEqualTo(FROM);
        assertThat(response.points().get(0).value()).isEqualTo(false);
        assertThat(response.points().get(1).timestamp()).isEqualTo(Instant.ofEpochMilli(afterStart));
        assertThat(response.points().get(1).value()).isEqualTo(true);

        assertThat(response.points().get(0).min()).isNull();
        assertThat(response.points().get(0).max()).isNull();

        assertThat(response.statistics().min()).isNull();
        assertThat(response.statistics().latest()).isEqualTo(true);

    }


    @Test
    void stringHistory_keepsTheTextValues(){

        stubKnownMetric("String", null);

        when(measurementRepository.findStateChanges(DEVICE, METRIC, FROM, TO)).thenReturn(List.of(
            stateChange(FROM.plusSeconds(10).toEpochMilli(), "PP black")
        ));

        ApiMetricHistoryResponse response = service.getHistory(DEVICE, METRIC, FROM, TO);

        assertThat(response.points().get(0).value()).isEqualTo("PP black");
        assertThat(response.statistics().latest()).isEqualTo("PP black");

    }


}

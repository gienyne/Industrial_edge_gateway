package com.gienyne.industrialedgegateway.persistence.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.gienyne.industrialedgegateway.persistence.domain.Measurement;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;


public interface MeasurementRepository extends JpaRepository<Measurement, Long>{

    boolean existsByDeviceIdAndMetricNameAndTime(String deviceId, String metricName, Instant time);

    // Finds the latest value for one metric.
    Optional<Measurement> findFirstByDeviceIdAndMetricNameOrderByTimeDesc(String deviceId, String metricName);

    // Finds the 20 newest measurements for one device.
    List<Measurement> findTop20ByDeviceIdOrderByTimeDesc(String deviceId);

    // Finds measurements for one device and metric within a time range.
    //List<Measurement> findByDeviceIdAndMetricNameAndTimeBetweenOrderByTimeAsc(String deviceId, String metricName, Instant from, Instant to);


    // Number of raw measurements in the requested time range.
    long countByDeviceIdAndMetricNameAndTimeBetween(String deviceId, String metricName, Instant from, Instant to);

    //Finds the latest measurement within the requested time range.
    Optional<Measurement> findFirstByDeviceIdAndMetricNameAndTimeBetweenOrderByTimeDesc(String deviceId, String metricName, Instant from, Instant to);

    interface NumericBucket{

        Long getBucketTimestampMillis();
        Double getAverageValue();
        Double getMinimumValue();
        Double getMaximumValue();
        Long getSampleCount();

    }


    interface StateChange{
        Long getTimestampMillis();
        String getValue();
    }


    @Query(value = """
        WITH bucketed_measurements AS (
            SELECT time_bucket(make_interval(secs => CAST(:bucketSeconds AS INTEGER)), m.time) AS bucket_time,
                   COALESCE(m.value_double, m.value_int::DOUBLE PRECISION) AS numeric_value
            FROM measurements m
            WHERE m.device_id = :deviceId
              AND m.metric_name = :metricName
              AND m.time >= :from
              AND m.time <= :to
              AND m.datatype IN('Integer', 'Double')
            )
        SELECT CAST(EXTRACT(EPOCH FROM bucket_time) * 1000 AS BIGINT) AS "bucketTimestampMillis",
            AVG(numeric_value) AS "averageValue",
            MIN(numeric_value) AS "minimumValue",
            MAX(numeric_value) AS "maximumValue",
            COUNT(*) AS "sampleCount"
        FROM bucketed_measurements
        GROUP BY bucket_time
        ORDER BY bucket_time ASC
        """, nativeQuery = true)
    List<NumericBucket> aggregateNumericMeasurements(
        @Param("deviceId") String deviceId,
        @Param("metricName") String metricName,
        @Param("from") Instant from,
        @Param("to") Instant to,
        @Param("bucketSeconds") int bucketSeconds
    );



    @Query(value = """
        WITH previous_measurement AS(
           SELECT m.id, m.time,
                  CASE
                    WHEN m.datatype = 'Boolean' THEN m.value_bool::TEXT
                    WHEN m.datatype = 'String' THEN m.value_string
                  END AS state_value
            FROM measurements m
            WHERE m.device_id = :deviceId
              AND m.metric_name = :metricName
              AND m.time < :from
              AND m.datatype IN('Boolean', 'String')
            ORDER BY m.time DESC, m.id DESC
            LIMIT 1 
        ), range_measurements AS(
               SELECT m.id, m.time,
                      CASE
                        WHEN m.datatype = 'Boolean' THEN m.value_bool::TEXT
                        WHEN m.datatype = 'String' THEN m.value_string
                      END AS state_value
                FROM measurements m
                WHERE m.device_id = :deviceId
                  AND m.metric_name = :metricName
                  AND m.time >= :from
                  AND m.time <= :to
                  AND m.datatype IN('Boolean', 'String')
         ), combined_measurements AS(
                SELECT id, time, state_value
                FROM previous_measurement
                UNION ALL
                SELECT id, time, state_value
                FROM range_measurements
         ), ordered_measurements AS(
                SELECT id, time, state_value,
                    LAG(state_value) OVER (ORDER BY time, id) AS previous_value
                FROM combined_measurements
            )
            SELECT CAST(EXTRACT(EPOCH FROM time) * 1000 AS BIGINT) AS "timestampMillis", state_value AS value
            FROM ordered_measurements
            WHERE time < :from OR previous_value IS DISTINCT FROM state_value
            ORDER BY time, id
            """, nativeQuery = true)
    List<StateChange> findStateChanges(
        @Param("deviceId") String deviceId,
        @Param("metricName") String metricName,
        @Param("from") Instant from,
        @Param("to") Instant to
    );


}
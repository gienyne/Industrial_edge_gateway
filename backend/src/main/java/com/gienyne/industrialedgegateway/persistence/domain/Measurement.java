package com.gienyne.industrialedgegateway.persistence.domain;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;


import java.time.Instant;


@Entity
@Table(name = "measurements")
public class Measurement {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "measurements_id_seq")
    @SequenceGenerator(
        name = "measurements_id_seq",
        sequenceName = "measurements_id_seq",
        allocationSize = 100
    )
    private Long id;

    @Column(name = "time", nullable = false)
    private Instant time;

    @Column(name = "device_id", nullable = false)
    private String deviceId;

    @Column(name = "metric_name", nullable = false)
    private String metricName;

    @Column(name = "datatype", nullable = false)
    private String datatype;

    @Column(name = "unit")
    private String unit;

    @Column(name = "value_bool")
    private Boolean valueBool;

    @Column(name = "value_int")
    private Integer valueInt;

    @Column(name = "value_double")
    private Double valueDouble;

    @Column(name = "value_string")
    private String valueString;

    protected Measurement(){
        // required by JPA
    }


    private static Measurement base(Instant time, String deviceId, String metricName, String datatype, String unit){

        Measurement m = new Measurement();

        m.time = time;
        m.deviceId = deviceId;
        m.metricName = metricName;
        m.datatype = datatype;
        m.unit = unit;

        return m;
    }


    public static Measurement ofBoolean(Instant time, String deviceId, String metricName, String unit, boolean value){
        Measurement m = base(time, deviceId, metricName, "Boolean", unit);
        m.valueBool = value;
        return m;
    }

    public static Measurement ofInteger(Instant time, String deviceId, String metricName, String unit, int value){
        Measurement m = base(time, deviceId, metricName, "Integer", unit);
        m.valueInt = value;
        return m;
    }

    public static Measurement ofDouble(Instant time, String deviceId, String metricName, String unit, double value){
        Measurement m = base(time, deviceId, metricName, "Double", unit);
        m.valueDouble = value;
        return m;
    }

    public static Measurement ofString(Instant time, String deviceId, String metricName, String unit, String value){
        Measurement m = base(time, deviceId, metricName, "String", unit);
        m.valueString = value;
        return m;
    }


    public Long getId(){
        return id;
    }

    public Instant getTime(){
        return time;
    }

    public String getDeviceId(){
        return deviceId;
    }

    public String getMetricName(){
        return metricName;
    }

    public String getDatatype(){
        return datatype;
    }

    public String getUnit(){
        return unit;
    }

}
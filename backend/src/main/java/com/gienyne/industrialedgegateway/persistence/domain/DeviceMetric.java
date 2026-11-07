package com.gienyne.industrialedgegateway.persistence.domain;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;


@Entity
@Table(name = "device_metrics")
public class DeviceMetric{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "device_id", nullable = false)
    private String deviceId;

    @Column(name = "metric_name", nullable = false)
    private String metricName;

    @Column(name = "datatype", nullable = false)
    private String datatype;

    @Column(name = "unit")
    private String unit;

    protected DeviceMetric(){

    }

    public DeviceMetric(String deviceId, String metricName, String datatype, String unit){

        this.deviceId = deviceId;
        this.metricName = metricName;
        this.datatype = datatype;
        this.unit = unit;

    }

    public void update(String datatype, String unit){

        this.datatype = datatype;
        this.unit = unit;

    }

    public Long getId() {
        return id;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public String getMetricName() {
        return metricName;
    }

    public String getDatatype() {
        return datatype;
    }

    public String getUnit() {
        return unit;
    }
}
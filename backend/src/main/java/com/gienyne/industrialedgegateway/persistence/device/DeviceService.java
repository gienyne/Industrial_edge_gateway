package com.gienyne.industrialedgegateway.persistence.device;


import com.gienyne.industrialedgegateway.persistence.device.models.api.ApiDeviceDetailsResponse;
import com.gienyne.industrialedgegateway.persistence.device.models.api.ApiDeviceSummaryResponse;
import com.gienyne.industrialedgegateway.persistence.device.models.api.ApiMeasurementResponse;
import com.gienyne.industrialedgegateway.persistence.domain.Device;
import com.gienyne.industrialedgegateway.persistence.domain.DeviceMetric;
import com.gienyne.industrialedgegateway.persistence.repository.DeviceMetricRepository;
import com.gienyne.industrialedgegateway.persistence.repository.DeviceRepository;
import com.gienyne.industrialedgegateway.persistence.repository.MeasurementRepository;
import com.gienyne.industrialedgegateway.persistence.shared.error.ResourceNotFoundException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;


@Service
@Transactional(readOnly = true)
public class DeviceService{

    private final DeviceRepository deviceRepository;
    private final DeviceMetricRepository deviceMetricRepository;
    private final MeasurementRepository measurementRepository;


    public DeviceService(DeviceRepository deviceRepository, DeviceMetricRepository deviceMetricRepository, MeasurementRepository measurementRepository){

        this.deviceRepository = deviceRepository;
        this.deviceMetricRepository = deviceMetricRepository;
        this.measurementRepository = measurementRepository;

    }


    // Returns all devices with their metric counts.
    public List<ApiDeviceSummaryResponse> listDevices(){

        Map<String, Long> metricCounts = deviceMetricRepository.metricCountPerDevice().stream()
        .collect(Collectors.toMap(DeviceMetricRepository.MetricCount::getDeviceId, DeviceMetricRepository.MetricCount::getTotal));

        return deviceRepository.findAll(Sort.by("deviceId")).stream()
        .map(device -> new ApiDeviceSummaryResponse(
            device.getDeviceId(),
            device.getStatus(),
            device.getLastSeenAt(),
            metricCounts.getOrDefault(device.getDeviceId(), 0L)
        )).toList();

    }


    // Returns device details and the latest value of each known metric.
    public ApiDeviceDetailsResponse getDevice(String deviceId){

        Device device = findDevice(deviceId);

        List<ApiMeasurementResponse> currentValues = deviceMetricRepository.findByDeviceId(deviceId).stream()
        .sorted(Comparator.comparing(DeviceMetric::getMetricName))
        .map(metric -> measurementRepository.findFirstByDeviceIdAndMetricNameOrderByTimeDesc(deviceId, metric.getMetricName()))
        .flatMap(Optional::stream)
        .map(ApiMeasurementResponse::from)
        .toList();

        return new ApiDeviceDetailsResponse(device.getDeviceId(), device.getStatus(), device.getGroupId(), device.getEdgeNodeId(), device.getLastSeenAt(), currentValues);
    }


    // Returns the 20 most recent measurements for one device.
    public List<ApiMeasurementResponse> recentMeasurements(String deviceId){

        findDevice(deviceId);

        return measurementRepository.findTop20ByDeviceIdOrderByTimeDesc(deviceId).stream()
        .map(ApiMeasurementResponse::from)
        .toList();

    }


    // Finds a device or reports that it does not exist.
    private Device findDevice(String deviceId){

        return deviceRepository.findById(deviceId).orElseThrow(() -> new ResourceNotFoundException("Device was not found"));

    }
}
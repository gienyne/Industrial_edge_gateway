package com.gienyne.industrialedgegateway.persistence.device;


import com.gienyne.industrialedgegateway.persistence.device.models.api.ApiDeviceDetailsResponse;
import com.gienyne.industrialedgegateway.persistence.device.models.api.ApiDeviceSummaryResponse;
import com.gienyne.industrialedgegateway.persistence.device.models.api.ApiMeasurementResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


import java.util.List;


@RestController
@RequestMapping("/api/devices")
public class DeviceController{

    private final DeviceService deviceService;


    public DeviceController(DeviceService deviceService){
        this.deviceService = deviceService;
    }

    // Returns the list of devices.
    @GetMapping
    public List<ApiDeviceSummaryResponse> listDevices(){
        return deviceService.listDevices();
    }


    // Returns the details of one device.
    @GetMapping("/{deviceId}")
    public ApiDeviceDetailsResponse getDevice(@PathVariable("deviceId") String deviceId){

        return deviceService.getDevice(deviceId);

    }


    // Returns the 20 most recent measurements of one device.
    @GetMapping("/{deviceId}/measurements/recent")
        public List<ApiMeasurementResponse> recentMeasurements(@PathVariable("deviceId") String deviceId){

            return deviceService.recentMeasurements(deviceId);

        }

}
#include "Gatewayapplication.h"
#include "BirthPublicationPolicy.h"
#include <algorithm>
#include <thread>
#include <chrono>
#include <iostream>

Gatewayapplication::Gatewayapplication(const GatewayApplicationConfig& config, std::vector<std::unique_ptr<IConnector>> connectors) : config_(config), encoder_(config.encoderConfig), publisher_(config.mqttConfig, encoder_), connectors_(std::move(connectors))
{

}

void Gatewayapplication::runDiscoveryWindow()
{
    /**
     * Devices are discovered before the Sparkplug session is established.
     * This allows the initial birth sequence to contain all devices known at startup.
     */
    auto deadline = std::chrono::steady_clock::now() + config_.discoveryWindow;

    while(std::chrono::steady_clock::now() < deadline){
        
        for(const auto& connector: connectors_){

            std::vector<DeviceData> allData = connector->collectData();

            for(const auto& data : allData){

                //Keep only the latest state of each device.
                lastKnownState_[data.deviceId] = data;

            }
        }
        
        std::this_thread::sleep_for(std::chrono::milliseconds(200));
    }
}

bool Gatewayapplication::publishBirthSequence()
{
    /**
     * A Sparkplug birth sequence starts with the Node Birth.
     * Device Birth messages follow for all currently known devices.
     */
    if(!publisher_.publish(encoder_.encodeNodeBirth())){

        std::cerr << "GatewayApplication: NBIRTH publish failed" << std::endl;
        return false;

    }

    for(const auto& [deviceId, data] : lastKnownState_){

        if(!publisher_.publish(encoder_.encodeDeviceBirth(data))){

            std::cerr << "GatewayApplication: DBIRTH publish failed for '" << deviceId << "'" << std::endl;
            return false;
        }

        birthedDevices_.insert(deviceId);
    }

    return true;

}

bool Gatewayapplication::initialize()
{
    /**
     * Connectors must be ready before device discovery starts.
     */
    for(const auto& connector : connectors_){

        if(!connector->initialize()){

            std::cerr << "GatewayApplication: connector '" << connector->name() << "' initialization failed" << std::endl;
            return false;

        }
    }

    std::cout << "GatewayApplication: discovering devices (" << config_.discoveryWindow.count() << "ms ) ..." << std::endl;

    runDiscoveryWindow();

    std::cout << "GatewApplication: " << lastKnownState_.size() << "device(s) discovered" << std::endl;

    /**
     * The birth publication can be controlled independently from
     * the application lifecycle, for example by a startup policy managed by a primary host application
     */
    while(!isAllowedToPublishBirth()){
        std::this_thread::sleep_for(std::chrono::milliseconds(200));
    }

    /**
     * The MQTT connection is established only after discovery.
     * This allows the initial Sparkplug birth sequence to be
     * published with the devices already known to the gateway.
     */
    if(!publisher_.initialize()){

        std::cerr << "GatewayApplication: MQTT connection failed" << std::endl;
        return false;
    }

    // Publish NBIRTH and all DBIRTH messages.
    return publishBirthSequence();

}

void Gatewayapplication::pollOnce()
{

    std::vector<DeviceData> batch;

    /**
     * Collect one complete batch before processing it.
     * 
     * This is important when a new device appears: the gateway
     * must first know about the complete batch before deciding
     * whether a new birth sequence is required.
     */
    for(const auto& connector : connectors_){
        
        std::vector<DeviceData> allData = connector->collectData();

        // Append the connector's data to the common batch.
        batch.insert(batch.end(), allData.begin(), allData.end());


    }

    /**
     * Check whether at least one device in the batch
     * has never had a DBIRTH published by this gateway.
     */
    bool hasNewDevice = std::any_of(batch.begin(), batch.end(), [this](const DeviceData& data){

        return birthedDevices_.find(data.deviceId) == birthedDevices_.end();

    });

    // A new device requires a new birth sequence.
    if(hasNewDevice){
        
        for(const auto& data : batch){

            // Update the known state before rebuilding the complete birth sequence.
            lastKnownState_[data.deviceId] = data;
        
        }

        std::cout << "GatewayApplication: new device detected, triggering rebirth" << std::endl;

        if(!publishBirthSequence()){

            std::cerr << "GatewayApplication: rebirth sequence failed" << std::endl;

        }

        return;
    }

    // No new device was detected.Process every DeviceData normally.
    for(const auto& data : batch){
        
        handleDeviceData(data);

    }
}

void Gatewayapplication::handleDeviceData(const DeviceData& data)
{

    // Determine which metrics changed.
    DeviceData changed = filterChangedMetrics(lastKnownState_[data.deviceId], data);

    // Nothing changed -> no DDATA needs to be published.
    if(!changed.metrics.empty()){

        // Encode the changed metrics as a Sparkplug DDATA and publish the resulting message.
        if(publisher_.publish(encoder_.encodeDeviceData(changed))){
            
            // Only update our known state after successful publication.
            lastKnownState_[data.deviceId] = data;

        }
        else{

            std::cerr << "GatewayApplication: DDATA publish failed for '" << data.deviceId << "'" << std::endl;

        }
    }
}

DeviceData Gatewayapplication::filterChangedMetrics(const DeviceData& previous, const DeviceData& incoming) const
{
    DeviceData result;

    // Keep the device identity and timestamp.
    result.deviceId = incoming.deviceId;
    result.timestamp = incoming.timestamp;

    for(const auto& metric : incoming.metrics){

        // Search for the same metric in the previous state.
        auto it = std::find_if(previous.metrics.begin(), previous.metrics.end(), [&metric](const Metric& m)

    {
        return m.name == metric.name;
    });


    bool changed = (it == previous.metrics.end()) || !(it->value == metric.value);

    // Keep only changed metrics.
    if (changed){
        result.metrics.push_back(metric);
    }

    }

    return result;

}

void Gatewayapplication::shutdown()
{

    // Explicitly publish NDEATH before disconnecting.
    publisher_.publish(encoder_.encodeNodeDeath());

    // Perform a clean MQTT disconnection.
    publisher_.disconnect();
    
}
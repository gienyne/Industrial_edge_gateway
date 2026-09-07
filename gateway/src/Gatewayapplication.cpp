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
     * 
     * During this window, the Gateway listens for incoming device data
     * so that it can build an initial list of devices and their known state.
     * 
     * A device is considered "seen" as soon as a message is received,
     * even if that message contains no usable metrics.
     */
    auto deadline = std::chrono::steady_clock::now() + config_.discoveryWindow;

    while(std::chrono::steady_clock::now() < deadline){
        
        for(const auto& connector: connectors_){

            std::vector<DeviceData> allData = connector->collectData();

            for(const auto& data : allData){

                // Receiving a message means that the device is still communicating.
                // This is used later for device timeout detection.
                lastSeenAt_[data.deviceId] = std::chrono::steady_clock::now();


                /**
                 * No metrics means that the message did not provide usable application data.
                 * 
                 * We therefore keep the liveness information above, but do
                 * not create or overwrite the device's known application state.
                 */
                if(data.metrics.empty()){
                    continue;
                }

                // Store the most recent valid state received during discovery.
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

        // Remember that this device has been declared through DBIRTH.
        birthedDevices_.insert(deviceId);
    }

    return true;

}

bool Gatewayapplication::initialize()
{
    
    for(const auto& connector : connectors_){

        if(!connector->initialize()){

            std::cerr << "GatewayApplication: connector '" << connector->name() << "' initialization failed; continuing Gateway startup"  << std::endl;
            
        }
    }

    std::cout << "GatewayApplication: discovering devices (" << config_.discoveryWindow.count() << "ms ) ..." << std::endl;


    /**
     * Discover devices before establishing the Sparkplug session.
     * This allows the initial NBIRTH/DBIRTH sequence to describe
     * the devices already known at startup.
     */
    runDiscoveryWindow();


    std::cout << "GatewApplication: " << lastKnownState_.size() << "device(s) discovered" << std::endl;

    /**
     * The birth publication can be controlled independently from
     * the application lifecycle
     * 
     * For example for future extension, a primary host application may decide when the
     * Gateway is allowed to announce itself on Sparkplug.
     */
    while(!isAllowedToPublishBirth()){
        std::this_thread::sleep_for(std::chrono::milliseconds(200));
    }

   
    if(!publisher_.initialize()){

        std::cerr << "GatewayApplication: MQTT connection failed" << std::endl;
        return false;
    }

    // Publish NBIRTH followed by DBIRTH for all known devices.
    return publishBirthSequence();

}

void Gatewayapplication::pollOnce()
{

    if(publisher_.consumeRebirthRequest()){
        
        std::cout << "GatewayApplication: NCMD Rebirth requested, republishing birth sequence" << std::endl;

        if(!publishBirthSequence()){

            std::cerr << "GatewayApplication: rebirth sequence (NCMD-triggered) failed" << std::endl;

        }

        return;
    }

    std::vector<DeviceData> batch;

    /**
     * Collect one complete batch from all connectors before processing it.
     * 
     * This is important when a new device appears: 
     * the gateway must first know about the complete batch before deciding
     * whether a new birth sequence is required.
     */
    for(const auto& connector : connectors_){
        
        std::vector<DeviceData> allData = connector->collectData();

        // Append the connector's data to the common batch.
        batch.insert(batch.end(), allData.begin(), allData.end());


    }


    /**
     * Determine whether the current batch requires a new birth sequence.
     * 
     * A rebirth is required when:
     *   1. A device provides valid metrics but has never received DBIRTH.
     *   2. A device provides a metric that was not present in its previous state.
     * 
     * Messages without metrics do not trigger a rebirth because they
     * provide liveness information, but no new Sparkplug application data.
     */
    bool needsRebirth = std::any_of(batch.begin(), batch.end(), [this](const DeviceData& data)

    {

        if(data.metrics.empty()){
            return false;
        }

        auto it = birthedDevices_.find(data.deviceId);

        if(it == birthedDevices_.end()){
            return true; // new device requiring DBIRTH.
        }

        return hasNewMetric(lastKnownState_.at(data.deviceId), data);

    });


    /**
     * Any received message updates the device's liveness timestamp,
     * even when the message contains no usable metrics.
     * 
     * This prevents a device that is still communicating but temporarily
     * unable to provide valid measurements from being considered offline.
     */
    for(const auto& data : batch){
        lastSeenAt_[data.deviceId] = std::chrono::steady_clock::now();
    }

    if(needsRebirth){

        /**
         * Before publishing the new birth sequence, update the stored
         * state with all valid data from the batch.
         * 
         * Empty-metric messages are intentionally ignored here because
         * they contain no new application state.
         */
        for(const auto& data : batch){

            if(!data.metrics.empty()){
                lastKnownState_[data.deviceId] = data;
            }
            
        }

        std::cout << "GatewayApplication: new device or metric detected, triggering rebirth" << std::endl;

        if(!publishBirthSequence()){
            std::cerr << "GatewayApplication: rebirth sequence failed" << std::endl;
        }

        return;
    }
    
    for(const auto& data : batch){
        handleDeviceData(data);
    }

    checkDeviceTimeouts();

}


void Gatewayapplication::checkDeviceTimeouts()
{
    auto now = std::chrono::steady_clock::now();

    std::vector<std::string> toRemove;

    /**
     * Only devices that were previously declared through DBIRTH are checked for timeout.
     */
    for(const auto& deviceId : birthedDevices_){

        auto it = lastSeenAt_.find(deviceId);

        if(it == lastSeenAt_.end()){
            continue;
        }

        // If no message has been received from the device for longer
        // than the configured timeout, consider the device offline.
        if((now - it->second) > config_.deviceTimeout){
            toRemove.push_back(deviceId);
        }
    }

    for(const auto& deviceId : toRemove){

        std::cout << "GatewayApplication: device '" << deviceId << "' timed out, publishing DDEATH" << std::endl;

        if(!publisher_.publish(encoder_.encodeDeviceDeath(deviceId))){

            std::cerr << "GatewayApplication: DDEATH publish failed for '" << deviceId << "'" << std::endl;

        }

        /**
         * Remove the device from the current Gateway state.
         * 
         * If the device comes back later with valid metrics, it will be
         * treated as a new device and a new DBIRTH/rebirth will be required.
         */
        birthedDevices_.erase(deviceId);
        lastKnownState_.erase(deviceId);
        lastSeenAt_.erase(deviceId);
    }

}

void Gatewayapplication::handleDeviceData(const DeviceData& data)
{

    // A message without metrics contains no application data to publish.
    // Its liveness has already been updated in pollOnce().
    if(data.metrics.empty()){
        return;
    }

    auto stateIt = lastKnownState_.find(data.deviceId);

    if(stateIt == lastKnownState_.end()){
        return;
    }

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


    /**
     * A metric is considered changed when:
     * 
     *   - it did not exist in the previous state, or
     *   - its value is different from the previous value.
     * 
     * The first condition is a safety mechanism: if a metric is not present
     * in the previous state, it is still included in the filtered result
     * instead of being silently discarded.
     * 
     * Under normal operation, a new metric should already have been detected
     * by hasNewMetric() and should have triggered a rebirth.
     */
    bool changed = (it == previous.metrics.end()) || !(it->value == metric.value);

    // Keep only changed metrics.
    if (changed){
        result.metrics.push_back(metric);
    }

    }

    return result;

}


bool Gatewayapplication::hasNewMetric(const DeviceData& previous, const DeviceData& incoming) const
{
    /**
     * Checks whether the incoming device data contains a metric that was
     * not present in the previously known device state.
     * 
     * A new metric changes the device's metric definition, so the Gateway
     * must publish a new birth sequence before normal DDATA processing can continue.
     */
    for(const auto& metric : incoming.metrics){

            bool found = std::any_of(previous.metrics.begin(), previous.metrics.end(), [&metric](const Metric& m)
        {
            return m.name == metric.name;
        });
 
            if(!found){
                 return true;
            }
    }

    return false;
}


void Gatewayapplication::shutdown()
{

    // Explicitly publish NDEATH before disconnecting.
    publisher_.publish(encoder_.encodeNodeDeath());

    // Perform a clean MQTT disconnection.
    publisher_.disconnect();
    
}
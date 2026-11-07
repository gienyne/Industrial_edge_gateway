package com.gienyne.industrialedgegateway.persistence.ingestion;


import jakarta.annotation.PreDestroy;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.tahu.protobuf.SparkplugBProto.Payload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


@Component
public class SparkplugCommandPublisher{

    private static final Logger log = LoggerFactory.getLogger(SparkplugCommandPublisher.class);

    private static final String REBIRTH_METRIC = "Node Control/Rebirth";
    private static final int SPARKPLUG_BOOLEAN = 11;
    private static final long MIN_INTERVAL_MS = 10_000;

    private final Map<String, Long> lastRebirthRequest = new HashMap<>();

    private final ExecutorService publisherThread = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "sparkplug-command-publisher");
        thread.setDaemon(true);
        return thread;
    });

    private volatile MqttClient client;

    /**
     * Called by SparkplugMqttListener once its MQTT client exists.
     */
    void attach(MqttClient client){
        this.client = client;
    }


    public synchronized boolean requestRebirth(String groupId, String edgeNodeId){

        MqttClient current = client;

        if (current == null || !current.isConnected()){
            return false;
        }

        String key = groupId + "/" + edgeNodeId;
        long now = System.currentTimeMillis();
        Long last = lastRebirthRequest.get(key);

        if(last != null && now - last < MIN_INTERVAL_MS){
            return false;
        }

        lastRebirthRequest.put(key, now);

        String topic = "spBv1.0/" + groupId + "/NCMD/" + edgeNodeId;

        byte[] bytes = Payload.newBuilder().setTimestamp(now).addMetrics(
            Payload.Metric.newBuilder()
            .setName(REBIRTH_METRIC)
            .setTimestamp(now)
            .setDatatype(SPARKPLUG_BOOLEAN)
            .setBooleanValue(true)
        ).build().toByteArray();


        publisherThread.execute(() -> {
            try{
                current.publish(topic, bytes, 0, false);
                log.info("Rebirth requested for {}", key);
            }
            catch(MqttException e){
                log.error("Failed to publish rebirth request for {}: {}", key, e.getMessage());
                forgetRequest(key, now); // a failed request must not block the next attempt
            }
        });

        return true;
    }


    private synchronized void forgetRequest(String key, long timestamp){
        lastRebirthRequest.remove(key, timestamp);
    }

    @PreDestroy
    void shutdown(){
        publisherThread.shutdownNow();
    }

}

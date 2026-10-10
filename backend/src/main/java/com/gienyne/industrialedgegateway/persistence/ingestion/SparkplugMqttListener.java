package com.gienyne.industrialedgegateway.persistence.ingestion;


import com.google.protobuf.InvalidProtocolBufferException;
import jakarta.annotation.PreDestroy;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.eclipse.tahu.protobuf.SparkplugBProto.Payload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;


@Component
public class SparkplugMqttListener implements MqttCallbackExtended{

    private static final Logger log = LoggerFactory.getLogger(SparkplugMqttListener.class);

    private static final String TOPIC_FILTER = "spBv1.0/#";

    private final String brokerAddress;
    private final String groupId;
    private final String edgeNodeId;
    private final long syncWindowSeconds;


    private final SparkplugMessageProcessor processor;
    private final SparkplugCommandPublisher commandPublisher;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "sparkplug-sync-window");
        thread.setDaemon(true);
        return thread;
    });

    private ScheduledFuture<?> pendingWindowEnd;

    private long synchronizationGeneration = 0;

    private volatile MqttClient client;

    public SparkplugMqttListener(

        @Value("${gateway.mqtt.broker-address}")
        String brokerAddress,

        @Value("${gateway.sparkplug.group-id}")
        String groupId,

        @Value("${gateway.sparkplug.edge-node-id}")
        String edgeNodeId,

        @Value("${gateway.sparkplug.sync-window-seconds:15}")
        long syncWindowSeconds,

        SparkplugMessageProcessor processor,
        SparkplugCommandPublisher commandPublisher
    ){

        this.brokerAddress = brokerAddress;
        this.groupId = groupId;
        this.edgeNodeId = edgeNodeId;
        this.syncWindowSeconds = syncWindowSeconds;
        this.processor = processor;
        this.commandPublisher = commandPublisher;

    }


    /**
     * Connects to MQTT when the Spring Boot application is ready.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void connect() throws MqttException{

        client = new MqttClient(brokerAddress, "persistence-service-ingestion", new MemoryPersistence());

        client.setCallback(this);

        // The command publisher uses this same MQTT client.
        commandPublisher.attach(client);

        MqttConnectOptions options = new MqttConnectOptions();
        options.setCleanSession(true);
        options.setAutomaticReconnect(true);

        client.connect(options);
    }



    /**
     * Called after the initial connection and each automatic reconnection.
     * Runs on Paho's callback thread: it must return immediately and never block on MQTT calls.
     */
    @Override
    public void connectComplete(boolean reconnect, String serverURI){

        long generation = startSynchronizationWindow();

        scheduler.execute(() -> synchronize(generation, reconnect, serverURI));

    }


    private synchronized boolean isCurrentGeneration(long generation){

        return generation == synchronizationGeneration;

    }

    private void synchronize(long generation, boolean reconnect, String serverURI){

        if(!isCurrentGeneration(generation)){
            log.debug("Skipping obsolete synchronization task {}", generation);
            return;
        }

        boolean synchronizationStarted = false;

        try{

            processor.beginSynchronization();
            synchronizationStarted = true;

            MqttClient current = client;

            if(current == null || !current.isConnected()){
                log.warn("MQTT client is not connected; synchronization cannot proceed");
                return;
            }

            // Subscribe again because the session is clean.
            current.subscribe(TOPIC_FILTER, 0);

            if(!isCurrentGeneration(generation)){
                log.debug("Skipping obsolete synchronization task {}", generation);
                return;
            }

            log.info("{} to {} on {}", reconnect ? "Re-subscribed" : "Subscribed", TOPIC_FILTER, serverURI);

            // Ask the configured Edge Node to resend NBIRTH and DBIRTH messages.
            boolean requested = commandPublisher.requestRebirth(groupId, edgeNodeId);

            if(!requested){
                log.debug("Rebirth request was not sent for {}/{}", groupId, edgeNodeId);
            }
        }
        catch(MqttException | RuntimeException e){
            log.error("Failed to start synchronization after MQTT connection: {}", e.getMessage(), e);
        }
        finally{

            // If synchronization started, schedule its end even if a later step fails.
            if(synchronizationStarted){
                scheduleEndOfSynchronizationWindow(generation);
            }
        }

    }

    /**
     * Starts a new sync window and cancels the previous scheduled end.
     */
    private synchronized long startSynchronizationWindow(){

        synchronizationGeneration++;

        if(pendingWindowEnd != null){
            pendingWindowEnd.cancel(false);
            pendingWindowEnd = null;
        }

        return synchronizationGeneration;
    }


    private synchronized void scheduleEndOfSynchronizationWindow(long generation){

        if(generation != synchronizationGeneration){
            return;
        }

        pendingWindowEnd = scheduler.schedule(() -> finishSynchronizationWindow(generation), syncWindowSeconds, TimeUnit.SECONDS);

    }


    /**
     * Ignore this task if a newer synchronization window has started.
     */
    private synchronized void finishSynchronizationWindow(long generation){

        if(generation != synchronizationGeneration){
            log.debug("Ignoring obsolete synchronization window {}", generation);
            return;
        }

        pendingWindowEnd = null;

        try{
            processor.finishSynchronization();
        }
        catch(RuntimeException e){
            log.error("Failed to finish synchronization: {}", e.getMessage(), e);
        }
    }


    /**
     * Called by Paho whenever a subscribed MQTT message arrives.
     */
    @Override
    public void messageArrived(String topicString, MqttMessage message){

        final SparkplugTopic topic;

        try{
            topic = SparkplugTopic.parse(topicString);
        }
        catch(RuntimeException e){
            log.warn("Ignoring invalid Sparkplug topic: {}", topicString);
            return;
        }

        if(topic == null){
            log.warn("Ignoring message on unexpected topic: {}", topicString);
            return;
        }

        boolean isBirth = "NBIRTH".equals(topic.messageType()) || "DBIRTH".equals(topic.messageType());

        final Payload payload;

        try{
            payload = Payload.parseFrom(message.getPayload());
        }
        catch(InvalidProtocolBufferException e){
            log.error("Failed to decode Sparkplug payload on topic {}: {}", topicString, e.getMessage());
            return;
        }

        if(isBirth){
            log.info("Received {} on {}", topic.messageType(), topicString);
        }


        try{
            processor.process(topic, payload);
            if(isBirth){
                log.info("Successfully processed {} on {}", topic.messageType(), topicString);
            }
        }
        catch(RuntimeException e){
            log.error("Failed to process message on topic {}: {}", topicString, e.getMessage(), e);
        }
    }

    /**
     * Called when the MQTT connection is lost.
     * Paho handles the automatic reconnection.
     */
    @Override
    public void connectionLost(Throwable cause){
        log.error("MQTT connection lost; Paho will attempt to reconnect", cause);
    }


    @Override
    public void deliveryComplete(IMqttDeliveryToken token){

    }


    /**
     * Releases the scheduler and MQTT client during application shutdown.
     */
    @PreDestroy
    public synchronized void shutdown(){

        synchronizationGeneration++;

        scheduler.shutdownNow();

        if(pendingWindowEnd != null){
            pendingWindowEnd.cancel(false);
            pendingWindowEnd = null;
        }

        MqttClient current = client;

        if(current == null){
            return;
        }

        try{
            if(current.isConnected()){
                current.disconnect();
            }
        }
        catch(MqttException e){
            log.warn("Error while disconnecting the MQTT client", e);
        }

        try{
            current.close();
        }
        catch(MqttException e){
            log.warn("Error while closing the MQTT client", e);
        }
    }

}
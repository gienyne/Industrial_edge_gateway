package com.gienyne.industrialedgegateway.persistence.ingestion;


import com.google.protobuf.InvalidProtocolBufferException;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
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


@Component
public class SparkplugMqttListener implements MqttCallback{

    private static final Logger log = LoggerFactory.getLogger(SparkplugMqttListener.class);

    private static final String TOPIC_FILTER = "spBv1.0/#";

    private final String brokerAddress;
    private final IngestionService ingestionService;

    private MqttClient client;

    public SparkplugMqttListener(@Value("${gateway.mqtt.broker-address}") String brokerAddress, IngestionService ingestionService){

        this.brokerAddress = brokerAddress;
        this.ingestionService = ingestionService;

    }

    @EventListener(ApplicationReadyEvent.class)
    public void connectAndSubscribe() throws MqttException{

        client = new MqttClient(brokerAddress, "persistence-service-ingestion", new MemoryPersistence());

        client.setCallback(this);

        MqttConnectOptions options = new MqttConnectOptions();
        options.setCleanSession(true);

        client.connect(options);

        client.subscribe(TOPIC_FILTER, 0);

        log.info("Subscribed to {} on {}", TOPIC_FILTER, brokerAddress);

    }

    @Override
    public void messageArrived(String topicString, MqttMessage message){

        SparkplugTopic topic = SparkplugTopic.parse(topicString);

        if (topic == null){
            log.warn("Ignoring message on unexpected topic: {}", topicString);
            return;
        }

        Payload payload;

        try{
            payload = Payload.parseFrom(message.getPayload());
        }
        catch(InvalidProtocolBufferException e){
            log.error("Failed to decode Sparkplug payload on topic {}: {}", topicString, e.getMessage());
            return;
        }

        try{
            ingestionService.ingest(topic, payload);
        }
        catch(RuntimeException e){
            log.error("Failed to ingest message on topic {}: {}", topicString, e.getMessage(), e);

        }
    }

    @Override
    public void connectionLost(Throwable cause){
        log.error("MQTT connection lost: {}", cause.getMessage(), cause);
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token){
        // The backend does not publish MQTT messages.
    }
}
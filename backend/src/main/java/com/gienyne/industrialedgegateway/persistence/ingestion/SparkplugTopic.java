package com.gienyne.industrialedgegateway.persistence.ingestion;


/**
 * Parsed Sparkplug B topic: spBv1.0/<group>/<messageType>/<edgeNode>/[deviceId]
 * 
 * is null for Node-level messages (NBIRTH, NDEATH), which have no device segment
 */
public record SparkplugTopic (String groupId, String messageType, String edgeNodeId, String deviceId){

    public static SparkplugTopic parse(String topic){

        String[] parts = topic.split("/");

        if (parts.length < 4){
            return null;
        }

        String deviceId = parts.length > 4 ? parts[4] : null;
        return new SparkplugTopic(parts[1], parts[2], parts[3], deviceId);
    }

    public boolean isDeviceLevel(){
        return deviceId != null;
    }

    public boolean isDataMessage(){
        return isDeviceLevel() && ("DBIRTH".equals(messageType) || "DDATA".equals(messageType));
    }

}
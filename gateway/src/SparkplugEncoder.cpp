#include "SparkplugEncoder.h"
#include <chrono>
#include <stdexcept>

using org::eclipse::tahu::protobuf::Payload;

namespace{
    constexpr const char* NBIRTH = "NBIRTH";
    constexpr const char* NDEATH = "NDEATH";
    constexpr const char* DBIRTH = "DBIRTH";
    constexpr const char* DDATA  = "DDATA";
    constexpr const char* DDEATH = "DDEATH";
    constexpr const char* NCMD   = "NCMD";

    constexpr const char* BDSEQ_METRIC = "bdSeq";
    constexpr const char* REBIRTH_METRIC = "Node Control/Rebirth";
}


SparkplugEncoder::SparkplugEncoder(const SparkplugEncodeConfig& config) : config_(config), bdSeq_(0), seq_(0)
{

}


std::uint64_t SparkplugEncoder::nowMillisUtc() const
{
    using namespace std::chrono;
    return duration_cast<milliseconds>(system_clock::now().time_since_epoch()).count();
}


std::string SparkplugEncoder::buildNodeTopic(const std::string& message_type) const {

    return config_.namespaceId + "/" + config_.groupId + "/" + message_type + "/" + config_.edgeNodeId;

}


std::string SparkplugEncoder::buildDeviceTopic(const std::string& message_type, const std::string& deviceId) const {

    return config_.namespaceId + "/" + config_.groupId + "/" + message_type + "/" + config_.edgeNodeId + "/" + deviceId;

}


std::uint32_t SparkplugEncoder::toSparkplugDataType(MetricDataType type) const
{
    switch (type)
    {
    case MetricDataType::Boolean : return 11;
    case MetricDataType::Integer : return 3;
    case MetricDataType::Double : return 10;
    case MetricDataType::String : return 12;
    }

    throw std::invalid_argument("SparplugEncoder: unknown MetricDataType");
}


void SparkplugEncoder::appendMetric(Payload& payload,  const Metric& metric, bool includeDatatype) const
{
    Payload::Metric* out = payload.add_metrics();

    out->set_name(metric.name);
    out->set_timestamp(metric.timestamp);

    if(includeDatatype){
        out->set_datatype(toSparkplugDataType(metric.datatype));
    }

    switch (metric.datatype)
    {
    case MetricDataType::Boolean: 
        out->set_boolean_value(std::get<bool>(metric.value));
        break;
    
    case MetricDataType::Integer:
        out->set_int_value(static_cast<std::uint32_t>(std::get<int>(metric.value)));
        break;

    case MetricDataType::Double:
        out->set_double_value(std::get<double>(metric.value));
        break;
    
    case MetricDataType::String:
        out->set_string_value(std::get<std::string>(metric.value));
        break;
    }
}


void SparkplugEncoder::appendBdSeqMetric(Payload& payload) const
{
    Payload::Metric* metric = payload.add_metrics();
    metric->set_name(BDSEQ_METRIC);
    metric->set_timestamp(nowMillisUtc());
    metric->set_datatype(8);
    metric->set_long_value(bdSeq_);

}


void SparkplugEncoder::appendRebirthMetric(Payload& payload) const
{
    Payload::Metric* metric = payload.add_metrics();
    metric->set_name(REBIRTH_METRIC);
    metric->set_timestamp(nowMillisUtc());
    metric->set_datatype(11);
    metric->set_boolean_value(false);
}


void SparkplugEncoder::assignAndAdvanceSeq(Payload& payload)
{
    payload.set_seq(seq_);

    if(seq_ == 255){
        seq_ = 0;
    }
    else{
        ++seq_;
    }
}


SparkplugPayload SparkplugEncoder::serialize(const Payload& payload, const std::string& topic, int qos, bool retain) const
{
    SparkplugPayload result;
    result.topic = topic;
    result.qos = qos;
    result.retain = retain;

    result.payload.resize(payload.ByteSizeLong());
    payload.SerializeToArray(result.payload.data(), static_cast<int>(result.payload.size()));

    return result;
}




void SparkplugEncoder::onNewSession(){
    bdSeq_ = (bdSeq_ == 255) ? 0 : bdSeq_ + 1;
    seq_ = 0;
}


SparkplugPayload SparkplugEncoder::encodeNodeBirth()
{
    Payload payload;
    payload.set_timestamp(nowMillisUtc());

    appendBdSeqMetric(payload);
    appendRebirthMetric(payload);

    assignAndAdvanceSeq(payload);

    return serialize(payload, buildNodeTopic(NBIRTH), /*qos=*/0, /*retain*/false);

}


SparkplugPayload SparkplugEncoder::encodeNodeDeath()
{
    Payload payload;
    appendBdSeqMetric(payload);

    return serialize(payload, buildNodeTopic(NDEATH), /*qos=*/1, /*retain=*/false);
}


std::string SparkplugEncoder::nodeCommandTopic() const
{
    return  buildNodeTopic(NCMD);
}


SparkplugPayload SparkplugEncoder::buildWillPayload()
{
    Payload payload;
    appendBdSeqMetric(payload);

    return serialize(payload, buildNodeTopic(NDEATH), /*qos=*/1, /*retain=*/false);
}


SparkplugPayload SparkplugEncoder::encodeDeviceBirth(const DeviceData& deviceData)
{
    Payload payload;
    payload.set_timestamp(nowMillisUtc());

    for(const auto& metric : deviceData.metrics){
        appendMetric(payload, metric, /*includeDatatype=*/true);
    }

    assignAndAdvanceSeq(payload);

    return serialize(payload, buildDeviceTopic(DBIRTH, deviceData.deviceId), /*qos=*/0, /*retain=*/false);
}


SparkplugPayload SparkplugEncoder::encodeDeviceData(const DeviceData& deviceData)
{
    Payload payload;
    payload.set_timestamp(nowMillisUtc());

    for(const auto& metric : deviceData.metrics){
        appendMetric(payload, metric, /*includeDatatype=*/false);
    }

    assignAndAdvanceSeq(payload);

    return serialize(payload, buildDeviceTopic(DDATA, deviceData.deviceId), /*qos=*/0, /*retain=*/false);
}


SparkplugPayload SparkplugEncoder::encodeDeviceDeath(const std::string& deviceId)
{
    Payload payload;
    payload.set_timestamp(nowMillisUtc());

    assignAndAdvanceSeq(payload);

    return serialize(payload, buildDeviceTopic(DDEATH, deviceId), /*qos=*/0, /*retain=*/false);
}
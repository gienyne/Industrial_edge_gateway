#ifndef ICONNECTOR_H
#define ICONNECTOR_H

#include "DeviceData.h"

/**
 * @brief Interface for data source connectors.
 */
class IConnector
{

    public:
        
        /**
         * @brief Virtual destructor for derived connectors.
         */
        virtual ~IConnector() = default;

        /**
         * @brief Initialize the connector.
         */
        virtual bool initialize() = 0;

        /**
         * @brief Collect available device data
         */
        virtual std::vector<DeviceData> collectData() = 0;

        /**
         * @brief Return the connector name.
         */
        virtual const char* name() const = 0;

};

#endif
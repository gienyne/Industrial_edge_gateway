#ifndef BDSEQMANAGER_H
#define BDSEQMANAGER_H

#include <cstdint>
#include <string>


/**
 * @brief Manages the Sparkplug birth/death sequence number (bdSeq).
 * 
 * The bdSeq value identifies the current Edge Node session.
 * It is persisted to a local file so that a new session can use
 * the next value after a gateway restart.
 */
class BdSeqManager
{

    public:
       
        /**
         * @brief Creates a bdSeq manager using the specified persistence file
         * 
         * @param filePath Path to the file used to persist the bdSeq value.
         */
        explicit BdSeqManager(std::string filePath);

        /**
         * @brief Returns the bdSeq value for the next gateway session.
         * 
         * The value is read from the persistence file, incremented and
         * written back to the file. The value wraps from 255 to 0,
         * according to the Sparkplug specification.
         * 
         * @return The bdSeq value assigned to the current session.
         */
        std::uint64_t nextSessionBdSeq() const;


        /**
         * @brief Persists a bdSeq value as the last successfully used session
         * 
         * will be called only after the bdSeq value returned by
         * nextSessionBdSeq() has actually been used for a successful MQTT CONNECT.
         * 
         * @param bdSeq The bdSeq value to persist.
         */
        void commitSessionBdSeq(std::uint64_t bdSeq);

    
    private:

        // Path of the file used to persist the bdSeq value.
        std::string filePath_;

};


#endif
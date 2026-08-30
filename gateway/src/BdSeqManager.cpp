#include "BdSeqManager.h"

#include <fstream>
#include <iostream>

namespace
{
    // Sparkplug bdSeq values range from 0 to 255.
    constexpr std::uint64_t BDSEQ_WRAP_THRESHOLD = 255;
}


BdSeqManager::BdSeqManager(std::string filePath) : filePath_(std::move(filePath))
{

}



std::uint64_t BdSeqManager::nextSessionBdSeq()
{
    std::uint64_t bdSeq = 0;

    // Open the persistence file for reading.
    std::ifstream in (filePath_);

    if(!in){

        // The file does not exist or could not be opened
        // Start the first session with bdSeq = 0.
        std::ofstream out (filePath_, std::ios::trunc);

        if(!out || !(out << bdSeq)){

            std::cerr << "BdSeqManager: failed to save bdSeq=" << bdSeq << " to '" << filePath_ << std::endl;

        }

        return bdSeq;
    }

    std::uint64_t previous = 0;

    // Read the previously persisted bdSeq value.
    if(!(in >> previous)){

        // The file exists but does not contain a valid bdSeq value.
        // Fall back to 0 for the current session.
        std::cerr << "BdSeqManager: failed to read bdSeq from '" << filePath_
        << "' - using 0 for this session" << std::endl;

    }

    else{

        // Increment the session number and wrap from 255 back to 0.
        bdSeq = (previous >= BDSEQ_WRAP_THRESHOLD) ? 0 : previous + 1;

    }

    // Replace the previous persisted value with the new one.
    std::ofstream out(filePath_, std::ios::trunc);

    if(!out || !(out << bdSeq)){

        std::cerr << "BdSeqManager: failed to save bdSeq=" << bdSeq << " to '" << filePath_
        << "' - it will NOT carry over to the next restart" << std::endl;

    }

    return bdSeq;

}
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


std::uint64_t BdSeqManager::nextSessionBdSeq() const
{
    std::ifstream in (filePath_);

    if(!in){
        return 0;
    }

    std::uint64_t previous = 0;

    if(!(in >> previous)){
        std::cerr << "BdSeqManager: failed to read bdSeq from '" << filePath_ << "' - using 0 as the candidate for this session" << std::endl;
        return 0;
    }

    return (previous >= BDSEQ_WRAP_THRESHOLD) ? 0 : previous + 1;

}


void BdSeqManager::commitSessionBdSeq(std::uint64_t bdSeq)
{
    std::ofstream out(filePath_, std::ios::trunc);

    if(!out || !(out << bdSeq)){
        std::cerr << "BdSeqManager: failed to save bdSeq=" << bdSeq << " to '" << filePath_ << std::endl;
    }
}

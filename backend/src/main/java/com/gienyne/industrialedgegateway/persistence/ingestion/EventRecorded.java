package com.gienyne.industrialedgegateway.persistence.ingestion;


import com.gienyne.industrialedgegateway.persistence.domain.Event;

/**
 * Published by EventService right after an Event was saved.
 */
public record EventRecorded(Event event){

}

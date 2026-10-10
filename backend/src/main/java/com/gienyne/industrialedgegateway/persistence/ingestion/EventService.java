package com.gienyne.industrialedgegateway.persistence.ingestion;

import com.gienyne.industrialedgegateway.persistence.domain.Event;
import com.gienyne.industrialedgegateway.persistence.domain.EventType;
import com.gienyne.industrialedgegateway.persistence.repository.EventRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.time.Instant;


@Service
public class EventService{

    private final EventRepository eventRepository;
    private final ApplicationEventPublisher publisher;

    public EventService(EventRepository eventRepository, ApplicationEventPublisher publisher){

        this.eventRepository = eventRepository;
        this.publisher = publisher;

    }

    @Transactional
    public Event record(Instant time, EventType type, String groupId, String edgeNodeId, String deviceId, String message){

        Event saved = eventRepository.save(new Event(time, type, groupId, edgeNodeId, deviceId, message));
        publisher.publishEvent(new EventRecorded(saved));

        return saved;
    }
}
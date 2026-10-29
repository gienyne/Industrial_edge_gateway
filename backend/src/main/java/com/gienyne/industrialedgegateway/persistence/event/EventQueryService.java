package com.gienyne.industrialedgegateway.persistence.event;


import com.gienyne.industrialedgegateway.persistence.domain.Event;
import com.gienyne.industrialedgegateway.persistence.domain.EventSeverity;
import com.gienyne.industrialedgegateway.persistence.domain.EventType;
import com.gienyne.industrialedgegateway.persistence.event.models.api.ApiEventListResponse;
import com.gienyne.industrialedgegateway.persistence.event.models.api.ApiEventResponse;
import com.gienyne.industrialedgegateway.persistence.repository.EventRepository;

import jakarta.persistence.criteria.Predicate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;


import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


@Service
@Transactional(readOnly = true)
public class EventQueryService{

    // Maximum number of events returned on one page.
    private static final int MAX_PAGE_SIZE = 100;

    // Sorts events from newest to oldest.
    private static final Sort NEWEST_FIRST = Sort.by(
        Sort.Order.desc("time"),
        Sort.Order.desc("id")
    );

    private final EventRepository eventRepository;

    public EventQueryService(EventRepository eventRepository){
        this.eventRepository = eventRepository;
    }

    
    // Searches events using optional filters
    public ApiEventListResponse search(EventType type, EventSeverity severity, String deviceId, Instant from, Instant to, int page, int size){

        if(from != null && to != null && from.isAfter(to)){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The start date must not be after the end date");
        }

        if(page < 0){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Page must be zero or greater");
        }

        
        // Limits the number of events requested per page.
        if(size < 1 || size > MAX_PAGE_SIZE){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Page size must be between 1 and 100");
        }

        // Builds the search filters.
        Specification<Event> filters = buildFilters(type, severity, deviceId, from, to);

        // Requests the selected page from the database.
        Page<Event> result = eventRepository.findAll(filters, PageRequest.of(page, size, NEWEST_FIRST));

        // Converts database events into API response objects.
        List<ApiEventResponse> events = result.getContent().stream().map(ApiEventResponse::from).toList();

        // Returns the events together with pagination information.
        return new ApiEventListResponse(
            events, 
            result.getNumber(), 
            result.getSize(),
            result.getTotalElements(),
            result.getTotalPages(),
            result.hasNext()
        );
            
    }

    
    // Returns a limited list of events
    public List<ApiEventResponse> find(EventType type, String deviceId, Instant from, Instant to, int limit){

        // Builds the search filters.
        Specification<Event> filters = buildFilters(type, null, deviceId, from, to);

        // Requests the first page with the specified limit.
        Page<Event> result = eventRepository.findAll(filters,  PageRequest.of(0, limit, NEWEST_FIRST));

        // Converts the results into API response objects.
        List<ApiEventResponse> events = result.getContent().stream().map(ApiEventResponse::from).toList();


        return events;

    }

    
    // Builds the filters provided by the user.
    private Specification<Event> buildFilters(EventType type, EventSeverity severity, String deviceId, Instant from, Instant to){

        // Creates a specification containing the requested conditions.
        Specification<Event> filters = (root, query, builder) -> {

            // Stores the conditions that must be applied to the search.
            List<Predicate> predicates = new ArrayList<>();

            // Filters by event type when a type is provided
            if(type != null){
                predicates.add(builder.equal(root.get("type"), type));
            }

            // Filters by severity. keeps the event types that carry the requested severity.
            if(severity != null){
                List<EventType> typesWithSeverity = Arrays.stream(EventType.values())
                    .filter(candidate -> candidate.severity() == severity)
                    .toList();

                predicates.add(root.get("type").in(typesWithSeverity));
            }

            // Filters by device ID when an ID is provided.
            if(deviceId != null && !deviceId.isBlank()){
                predicates.add(builder.equal(root.get("deviceId"), deviceId));
            }

            // Includes events occurring at or after the start date.
            if(from != null){
                predicates.add(builder.greaterThanOrEqualTo(root.<Instant>get("time"), from));
            }

            // Includes events occurring at or before the end date.
            if(to != null){
                predicates.add(builder.lessThanOrEqualTo(root.<Instant>get("time"), to));
            }

            // Combines all provided conditions using AND.
            return builder.and(predicates.toArray(new Predicate[0]));

        };


        return filters;

    }

}
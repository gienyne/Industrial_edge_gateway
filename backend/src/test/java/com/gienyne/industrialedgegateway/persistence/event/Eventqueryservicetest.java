package com.gienyne.industrialedgegateway.persistence.event;


import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.gienyne.industrialedgegateway.persistence.domain.Event;
import com.gienyne.industrialedgegateway.persistence.domain.EventSeverity;
import com.gienyne.industrialedgegateway.persistence.domain.EventType;
import com.gienyne.industrialedgegateway.persistence.event.models.api.ApiEventListResponse;
import com.gienyne.industrialedgegateway.persistence.event.models.api.ApiEventResponse;
import com.gienyne.industrialedgegateway.persistence.repository.EventRepository;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.server.ResponseStatusException;


@ExtendWith(MockitoExtension.class)
class EventQueryServiceTest{

    private static final Instant NOON = Instant.parse("2026-10-22T12:00:00Z");

    @Mock
    private EventRepository eventRepository;

    private EventQueryService service;

    @BeforeEach
    void setUp(){

        service = new EventQueryService(eventRepository);

    }


    private static Event offlineEvent(){
        return new Event(NOON, EventType.DEVICE_OFFLINE, "SFM", "IndustrialEdgeGateway", "pump", "Device Offline");
    }


    private void stubPage(Page<Event> page){
        when(eventRepository.findAll(ArgumentMatchers.<Specification<Event>>any(), any(Pageable.class))).thenReturn(page);
    }


    private static void assertBadRequest(Throwable thrown){

        assertThat(thrown).isInstanceOfSatisfying(ResponseStatusException.class,
            exception -> assertThat(exception.getStatusCode().value()).isEqualTo(400));
    }



    @Test
    void search_withAStartAfterTheEnd_isRejected(){

        assertThatThrownBy(() -> service.search(null, null, null, NOON, NOON.minusSeconds(1), 0, 10))
            .satisfies(EventQueryServiceTest::assertBadRequest);

        verifyNoInteractions(eventRepository);

    }


    @Test
    void search_withANegativePage_isRejected(){

        assertThatThrownBy(() -> service.search(null, null, null, null, null, -1, 10))
            .satisfies(EventQueryServiceTest::assertBadRequest);

        verifyNoInteractions(eventRepository);

    }


    @Test
    void search_withAPageSizeOutsideTheLimits_isRejected(){

        assertThatThrownBy(() -> service.search(null, null, null, null, null, 0, 0))
            .satisfies(EventQueryServiceTest::assertBadRequest);


        assertThatThrownBy(() -> service.search(null, null, null, null, null, 0, 101))
            .satisfies(EventQueryServiceTest::assertBadRequest);


        verifyNoInteractions(eventRepository);

    }


    @Test
    void search_returnsTheEventsAndThePagingInformation(){

        stubPage(new PageImpl<>(List.of(offlineEvent()), PageRequest.of(1, 10), 25));

        ApiEventListResponse response = service.search(null, null, null, null, null, 1, 10);

        assertThat(response.events()).hasSize(1);
        assertThat(response.events().get(0).type()).isEqualTo(EventType.DEVICE_OFFLINE);
        assertThat(response.events().get(0).severity()).isEqualTo(EventSeverity.HIGH);
        assertThat(response.events().get(0).message()).isEqualTo("Device Offline");

        assertThat(response.page()).isEqualTo(1);
        assertThat(response.size()).isEqualTo(10);
        assertThat(response.totalElements()).isEqualTo(25);
        assertThat(response.totalPages()).isEqualTo(3);
        assertThat(response.hasNext()).isTrue();

    }


    @Test
    void search_asksForTheRequestedPage_newestFirst(){

        stubPage(new PageImpl<>(List.of(), PageRequest.of(2, 20), 0));

        service.search(null, null, null, null, null, 2, 20);

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);

        verify(eventRepository).findAll(ArgumentMatchers.<Specification<Event>>any(), pageable.capture());

        assertThat(pageable.getValue().getPageNumber()).isEqualTo(2);
        assertThat(pageable.getValue().getPageSize()).isEqualTo(20);
        assertThat(pageable.getValue().getSort().getOrderFor("time").getDirection()).isEqualTo(Sort.Direction.DESC);
        assertThat(pageable.getValue().getSort().getOrderFor("id").getDirection()).isEqualTo(Sort.Direction.DESC);

    }


    @Test
    void find_returnsTheLatestEventsUpToTheLimit(){

        stubPage(new PageImpl<>(List.of(offlineEvent()), PageRequest.of(0, 5), 1));

        List<ApiEventResponse> events = service.find(null, null, null, null, 5);

        assertThat(events).hasSize(1);
        assertThat(events.get(0).deviceId()).isEqualTo("pump");

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);


        verify(eventRepository).findAll(ArgumentMatchers.<Specification<Event>>any(), pageable.capture());

        assertThat(pageable.getValue().getPageNumber()).isZero();
        assertThat(pageable.getValue().getPageSize()).isEqualTo(5);

    }

}
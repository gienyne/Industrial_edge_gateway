package com.gienyne.industrialedgegateway.persistence.event.models.api;

import java.util.List;

public record ApiEventListResponse(List<ApiEventResponse> events, int page, int size, long totalElements, int totalPages, boolean hasNext){
    
}
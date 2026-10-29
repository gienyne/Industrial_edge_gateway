package com.gienyne.industrialedgegateway.persistence.overview;


import com.gienyne.industrialedgegateway.persistence.overview.models.api.ApiOverviewResponse;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/overview")
public class OverviewController{

    private final OverviewService overviewService;

    public OverviewController(OverviewService overviewService){
        this.overviewService = overviewService;
    }

    @GetMapping
    public ApiOverviewResponse getOverview(){
        return overviewService.getOverview();
    }
    
}
package com.perfumeshop.web;

import com.perfumeshop.dto.crm.CrmEventResponse;
import com.perfumeshop.dto.crm.CrmStatusResponse;
import com.perfumeshop.service.CrmService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/crm")
public class CrmController {

    private final CrmService crmService;

    public CrmController(CrmService crmService) {
        this.crmService = crmService;
    }

    @GetMapping("/status")
    public CrmStatusResponse status() {
        return crmService.status();
    }

    @GetMapping("/events")
    public List<CrmEventResponse> events() {
        return crmService.recentEvents();
    }
}

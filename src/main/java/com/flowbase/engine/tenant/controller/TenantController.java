package com.flowbase.engine.tenant.controller;

import com.flowbase.engine.config.TenantContext;
import com.flowbase.engine.tenant.dto.TenantRequest;
import com.flowbase.engine.tenant.dto.TenantResponse;
import com.flowbase.engine.tenant.service.TenantService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/tenants")
public class TenantController {
    private final TenantService tenantService;
    
    @PostMapping
    public ResponseEntity<TenantResponse> createTenant(@RequestBody TenantRequest request) {
        TenantResponse response = this.tenantService.createTenant(request.name());
        return ResponseEntity
                .created(ServletUriComponentsBuilder
                        .fromCurrentRequest()
                        .path("/me")
                        .build()
                        .toUri()
                ).body(response);
    }
    
    @GetMapping("/me")
    public ResponseEntity<TenantResponse> getTenant() {
        String tenantId = TenantContext.get();
        TenantResponse response = this.tenantService.getTenant(tenantId);
        return ResponseEntity.ok(response);
    }
}

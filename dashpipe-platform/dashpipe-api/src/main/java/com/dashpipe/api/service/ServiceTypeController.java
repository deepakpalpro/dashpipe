package com.dashpipe.api.service;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Global platform catalog (not tenant-scoped). Tenant overrides land in W1-US04.
 */
@RestController
@RequestMapping("/api/v1/service-types")
public class ServiceTypeController {

  private final ServiceTypeService serviceTypeService;

  public ServiceTypeController(ServiceTypeService serviceTypeService) {
    this.serviceTypeService = serviceTypeService;
  }

  @GetMapping
  public List<ServiceTypeResponse> list() {
    return serviceTypeService.listCatalog();
  }
}

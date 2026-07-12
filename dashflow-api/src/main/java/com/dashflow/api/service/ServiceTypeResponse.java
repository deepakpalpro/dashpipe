package com.dashflow.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;

public record ServiceTypeResponse(
    String id, ServiceKind type, String displayName, List<ServiceDefaultResponse> defaults) {

  static ServiceTypeResponse from(ServiceType entity, ObjectMapper mapper) {
    List<ServiceDefaultResponse> defaults =
        entity.getDefaults().stream().map(d -> ServiceDefaultResponse.from(d, mapper)).toList();
    return new ServiceTypeResponse(entity.getId(), entity.getType(), entity.getDisplayName(), defaults);
  }
}

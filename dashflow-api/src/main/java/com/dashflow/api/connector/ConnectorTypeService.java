package com.dashflow.api.connector;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConnectorTypeService {

  private final ConnectorTypeRepository repository;
  private final ObjectMapper objectMapper;

  public ConnectorTypeService(ConnectorTypeRepository repository, ObjectMapper objectMapper) {
    this.repository = repository;
    this.objectMapper = objectMapper;
  }

  @Transactional(readOnly = true)
  public List<ConnectorTypeResponse> listCatalog() {
    return repository.findAllByOrderByTypeAsc().stream()
        .map(ct -> ConnectorTypeResponse.from(ct, objectMapper))
        .toList();
  }
}

package com.dashpipe.api.connector;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Global connector SPI catalog (not tenant-scoped). */
@RestController
@RequestMapping("/api/v1/connector-types")
public class ConnectorTypeController {

  private final ConnectorTypeService connectorTypeService;

  public ConnectorTypeController(ConnectorTypeService connectorTypeService) {
    this.connectorTypeService = connectorTypeService;
  }

  @GetMapping
  public List<ConnectorTypeResponse> list() {
    return connectorTypeService.listCatalog();
  }
}

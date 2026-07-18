package com.dashpipe.api.tenant;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tenant-notes")
public class TenantNoteController {

  private final TenantNoteService tenantNoteService;

  public TenantNoteController(TenantNoteService tenantNoteService) {
    this.tenantNoteService = tenantNoteService;
  }

  @PostMapping
  public ResponseEntity<TenantNoteResponse> create(
      @Valid @RequestBody CreateTenantNoteRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(tenantNoteService.create(request));
  }

  @GetMapping("/{id}")
  public TenantNoteResponse get(@PathVariable String id) {
    return tenantNoteService.get(id);
  }

  @GetMapping
  public List<TenantNoteResponse> list() {
    return tenantNoteService.list();
  }
}

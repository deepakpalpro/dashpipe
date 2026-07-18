package com.dashpipe.api.tenant;

import java.time.Instant;

public record TenantNoteResponse(String id, String tenantId, String title, String body, Instant createdAt) {

  static TenantNoteResponse from(TenantNote note) {
    return new TenantNoteResponse(
        note.getId(), note.getTenantId(), note.getTitle(), note.getBody(), note.getCreatedAt());
  }
}

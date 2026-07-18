package com.dashpipe.api.tenant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateTenantNoteRequest(
    @NotBlank @Size(max = 255) String title, @Size(max = 4000) String body) {}

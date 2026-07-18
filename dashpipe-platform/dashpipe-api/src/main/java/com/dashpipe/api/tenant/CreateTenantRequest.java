package com.dashpipe.api.tenant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateTenantRequest(
    @NotBlank @Size(max = 255) String name,
    @NotBlank @Size(max = 64) String slug,
    TenantStatus status) {}

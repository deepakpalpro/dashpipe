package com.dashpipe.api.tenant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateTenantRequest(
    @NotBlank @Size(max = 255) String name, TenantStatus status) {}

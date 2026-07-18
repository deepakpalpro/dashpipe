package com.dashflow.api.tenant;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TenantService {

  /** Default prepaid credit so new tenants can run until W5 quota/credit is exhausted. */
  public static final BigDecimal DEFAULT_CREDIT_BALANCE = new BigDecimal("100.0000");

  private final TenantRepository tenantRepository;

  public TenantService(TenantRepository tenantRepository) {
    this.tenantRepository = tenantRepository;
  }

  @Transactional
  public TenantResponse create(CreateTenantRequest request) {
    String slug = normalizeSlug(request.slug());
    if (slug.isBlank()) {
      throw new TenantValidationException("slug must not be blank");
    }
    if (tenantRepository.existsBySlug(slug)) {
      throw new TenantConflictException("slug already exists: " + slug);
    }

    Tenant tenant = new Tenant();
    tenant.setId(UUID.randomUUID().toString());
    tenant.setName(request.name().trim());
    tenant.setSlug(slug);
    tenant.setStatus(request.status() == null ? TenantStatus.trial : request.status());
    tenant.setCreditBalance(DEFAULT_CREDIT_BALANCE);

    return TenantResponse.from(tenantRepository.save(tenant));
  }

  @Transactional(readOnly = true)
  public TenantResponse get(String id) {
    return tenantRepository
        .findById(id)
        .map(TenantResponse::from)
        .orElseThrow(() -> new TenantNotFoundException(id));
  }

  @Transactional(readOnly = true)
  public List<TenantResponse> list() {
    return tenantRepository.findAll().stream().map(TenantResponse::from).toList();
  }

  @Transactional
  public TenantResponse update(String id, UpdateTenantRequest request) {
    Tenant tenant =
        tenantRepository.findById(id).orElseThrow(() -> new TenantNotFoundException(id));
    tenant.setName(request.name().trim());
    if (request.status() != null) {
      tenant.setStatus(request.status());
    }
    return TenantResponse.from(tenantRepository.save(tenant));
  }

  private static String normalizeSlug(String slug) {
    return slug == null ? "" : slug.trim().toLowerCase();
  }
}

package com.dashpipe.api.tenant;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import java.util.UUID;
import org.hibernate.Session;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TenantNoteService {

  private final TenantNoteRepository tenantNoteRepository;

  @PersistenceContext private EntityManager entityManager;

  public TenantNoteService(TenantNoteRepository tenantNoteRepository) {
    this.tenantNoteRepository = tenantNoteRepository;
  }

  @Transactional
  public TenantNoteResponse create(CreateTenantNoteRequest request) {
    String tenantId = requireTenantId();
    enableTenantFilter(tenantId);

    TenantNote note = new TenantNote();
    note.setId(UUID.randomUUID().toString());
    note.setTenantId(tenantId);
    note.setTitle(request.title().trim());
    note.setBody(request.body());
    return TenantNoteResponse.from(tenantNoteRepository.save(note));
  }

  @Transactional(readOnly = true)
  public TenantNoteResponse get(String id) {
    String tenantId = requireTenantId();
    enableTenantFilter(tenantId);
    return tenantNoteRepository
        .findFilteredById(id)
        .map(TenantNoteResponse::from)
        .orElseThrow(() -> new TenantNoteNotFoundException(id));
  }

  @Transactional(readOnly = true)
  public List<TenantNoteResponse> list() {
    String tenantId = requireTenantId();
    enableTenantFilter(tenantId);
    return tenantNoteRepository.findAllFiltered().stream().map(TenantNoteResponse::from).toList();
  }

  private static String requireTenantId() {
    String tenantId = TenantContext.getTenantId();
    if (tenantId == null || tenantId.isBlank()) {
      throw new TenantContextRequiredException();
    }
    return tenantId;
  }

  private void enableTenantFilter(String tenantId) {
    Session session = entityManager.unwrap(Session.class);
    var filter = session.getEnabledFilter(TenantFilters.NAME);
    if (filter == null) {
      filter = session.enableFilter(TenantFilters.NAME);
    }
    filter.setParameter(TenantFilters.PARAM_TENANT_ID, tenantId);
  }
}

package com.dashflow.api.tenant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

@Entity
@Table(name = "tenant_notes")
@TenantOwned
// Single global FilterDef for tenantFilter — other @TenantOwned entities only declare @Filter.
@FilterDef(
    name = TenantFilters.NAME,
    parameters = @ParamDef(name = TenantFilters.PARAM_TENANT_ID, type = String.class))
@Filter(name = TenantFilters.NAME, condition = "tenant_id = :" + TenantFilters.PARAM_TENANT_ID)
public class TenantNote {

  @Id
  @Column(length = 36, nullable = false)
  private String id;

  @Column(name = "tenant_id", length = 36, nullable = false)
  private String tenantId;

  @Column(nullable = false)
  private String title;

  @Column(columnDefinition = "TEXT")
  private String body;

  @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
  private Instant createdAt;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getTenantId() {
    return tenantId;
  }

  public void setTenantId(String tenantId) {
    this.tenantId = tenantId;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getBody() {
    return body;
  }

  public void setBody(String body) {
    this.body = body;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}

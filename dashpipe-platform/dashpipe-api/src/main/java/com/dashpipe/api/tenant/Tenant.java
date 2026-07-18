package com.dashpipe.api.tenant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "tenants")
public class Tenant {

  @Id
  @Column(length = 36, nullable = false)
  private String id;

  @Column(nullable = false)
  private String name;

  @Column(nullable = false, length = 64, unique = true)
  private String slug;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private TenantStatus status;

  @Column(name = "credit_balance", nullable = false, precision = 12, scale = 4)
  private BigDecimal creditBalance = BigDecimal.ZERO;

  @Column(name = "quota_config", columnDefinition = "json")
  private String quotaConfig;

  @Column(name = "k8s_namespace", length = 63)
  private String k8sNamespace;

  @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
  private Instant updatedAt;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getSlug() {
    return slug;
  }

  public void setSlug(String slug) {
    this.slug = slug;
  }

  public TenantStatus getStatus() {
    return status;
  }

  public void setStatus(TenantStatus status) {
    this.status = status;
  }

  public BigDecimal getCreditBalance() {
    return creditBalance;
  }

  public void setCreditBalance(BigDecimal creditBalance) {
    this.creditBalance = creditBalance;
  }

  public String getQuotaConfig() {
    return quotaConfig;
  }

  public void setQuotaConfig(String quotaConfig) {
    this.quotaConfig = quotaConfig;
  }

  public String getK8sNamespace() {
    return k8sNamespace;
  }

  public void setK8sNamespace(String k8sNamespace) {
    this.k8sNamespace = k8sNamespace;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }
}

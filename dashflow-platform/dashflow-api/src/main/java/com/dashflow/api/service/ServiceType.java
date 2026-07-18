package com.dashflow.api.service;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "service_types")
public class ServiceType {

  @Id
  @Column(length = 36, nullable = false)
  private String id;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private ServiceKind type;

  @Column(name = "display_name", nullable = false, length = 128)
  private String displayName;

  @OneToMany(mappedBy = "serviceType")
  private List<ServiceDefault> defaults = new ArrayList<>();

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public ServiceKind getType() {
    return type;
  }

  public void setType(ServiceKind type) {
    this.type = type;
  }

  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public List<ServiceDefault> getDefaults() {
    return defaults;
  }

  public void setDefaults(List<ServiceDefault> defaults) {
    this.defaults = defaults;
  }
}

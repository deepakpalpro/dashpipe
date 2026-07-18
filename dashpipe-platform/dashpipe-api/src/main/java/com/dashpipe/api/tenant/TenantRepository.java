package com.dashpipe.api.tenant;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantRepository extends JpaRepository<Tenant, String> {

  boolean existsBySlug(String slug);

  Optional<Tenant> findBySlug(String slug);
}

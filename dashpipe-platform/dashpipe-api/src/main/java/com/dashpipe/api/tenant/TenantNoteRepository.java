package com.dashpipe.api.tenant;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TenantNoteRepository extends JpaRepository<TenantNote, String> {

  /**
   * Prefer JPQL over {@code findById}: Hibernate filters apply to queries, not always to
   * {@code EntityManager.find}.
   */
  @Query("select n from TenantNote n where n.id = :id")
  Optional<TenantNote> findFilteredById(@Param("id") String id);

  @Query("select n from TenantNote n order by n.createdAt desc")
  List<TenantNote> findAllFiltered();
}

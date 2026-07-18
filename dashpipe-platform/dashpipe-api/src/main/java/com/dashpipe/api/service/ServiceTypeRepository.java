package com.dashpipe.api.service;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ServiceTypeRepository extends JpaRepository<ServiceType, String> {

  @Query("select distinct st from ServiceType st left join fetch st.defaults order by st.type")
  List<ServiceType> findAllWithDefaults();

  @Query("select distinct st from ServiceType st left join fetch st.defaults where st.id = :id")
  Optional<ServiceType> findByIdWithDefaults(@Param("id") String id);

  Optional<ServiceType> findByType(ServiceKind type);
}

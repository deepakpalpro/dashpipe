package com.dashpipe.api.connector;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConnectorTypeRepository extends JpaRepository<ConnectorType, String> {

  List<ConnectorType> findAllByOrderByTypeAsc();
}

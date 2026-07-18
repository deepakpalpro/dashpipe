package com.dashpipe.api.observability;

import java.util.List;
import java.util.Optional;

/** Indexes structured pipeline logs for ELK (or local stub). */
public interface PipelineLogIndexer {

  /** Indexes {@code document} into the §7.3 index for its tenant/timestamp. */
  String index(PipelineLogDocument document);

  List<PipelineLogDocument> findByExecutionId(String executionId);

  Optional<PipelineLogDocument> findFirstByExecutionId(String executionId);
}

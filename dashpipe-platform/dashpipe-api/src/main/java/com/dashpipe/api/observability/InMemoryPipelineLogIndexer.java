package com.dashpipe.api.observability;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.stereotype.Component;

/**
 * In-memory ELK stub for Wave 4 CI/local (architecture §7.3). Documents are stored under the same
 * index names Elasticsearch would use. Replace with a real ES client when Compose ELK is required.
 */
@Component
public class InMemoryPipelineLogIndexer implements PipelineLogIndexer {

  private final ConcurrentHashMap<String, CopyOnWriteArrayList<PipelineLogDocument>> byIndex =
      new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String, CopyOnWriteArrayList<PipelineLogDocument>> byExecution =
      new ConcurrentHashMap<>();

  @Override
  public String index(PipelineLogDocument document) {
    if (document == null) {
      throw new IllegalArgumentException("document required");
    }
    String indexName =
        PipelineLogIndexNames.forTenantAndInstant(document.tenantId(), document.timestamp());
    byIndex
        .computeIfAbsent(indexName, k -> new CopyOnWriteArrayList<>())
        .add(document);
    if (document.executionId() != null && !document.executionId().isBlank()) {
      byExecution
          .computeIfAbsent(document.executionId(), k -> new CopyOnWriteArrayList<>())
          .add(document);
    }
    return indexName;
  }

  @Override
  public List<PipelineLogDocument> findByExecutionId(String executionId) {
    if (executionId == null || executionId.isBlank()) {
      return List.of();
    }
    CopyOnWriteArrayList<PipelineLogDocument> docs = byExecution.get(executionId);
    return docs == null ? List.of() : List.copyOf(docs);
  }

  @Override
  public Optional<PipelineLogDocument> findFirstByExecutionId(String executionId) {
    List<PipelineLogDocument> docs = findByExecutionId(executionId);
    return docs.isEmpty() ? Optional.empty() : Optional.of(docs.getFirst());
  }

  /** Test/support helper: documents currently held for an index name. */
  public List<PipelineLogDocument> findByIndex(String indexName) {
    CopyOnWriteArrayList<PipelineLogDocument> docs = byIndex.get(indexName);
    return docs == null ? List.of() : List.copyOf(docs);
  }

  public Map<String, List<PipelineLogDocument>> snapshotByIndex() {
    Map<String, List<PipelineLogDocument>> out = new ConcurrentHashMap<>();
    byIndex.forEach((k, v) -> out.put(k, new ArrayList<>(v)));
    return out;
  }

  public void clear() {
    byIndex.clear();
    byExecution.clear();
  }
}

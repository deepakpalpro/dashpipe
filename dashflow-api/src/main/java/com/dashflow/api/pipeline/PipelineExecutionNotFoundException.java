package com.dashflow.api.pipeline;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class PipelineExecutionNotFoundException extends RuntimeException {

  public PipelineExecutionNotFoundException(String id) {
    super("Pipeline execution not found: " + id);
  }
}

package com.dashpipe.api.pipeline;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class PipelineConflictException extends RuntimeException {

  public PipelineConflictException(String message) {
    super(message);
  }
}

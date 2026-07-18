package com.dashpipe.api.pipeline;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class ExecutionTriggerConverter implements AttributeConverter<ExecutionTrigger, String> {

  @Override
  public String convertToDatabaseColumn(ExecutionTrigger attribute) {
    return attribute == null ? null : attribute.getValue();
  }

  @Override
  public ExecutionTrigger convertToEntityAttribute(String dbData) {
    return ExecutionTrigger.fromValue(dbData);
  }
}

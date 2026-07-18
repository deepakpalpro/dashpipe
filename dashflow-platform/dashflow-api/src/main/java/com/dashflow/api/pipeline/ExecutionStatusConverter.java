package com.dashflow.api.pipeline;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class ExecutionStatusConverter implements AttributeConverter<ExecutionStatus, String> {

  @Override
  public String convertToDatabaseColumn(ExecutionStatus attribute) {
    return attribute == null ? null : attribute.getValue();
  }

  @Override
  public ExecutionStatus convertToEntityAttribute(String dbData) {
    return ExecutionStatus.fromValue(dbData);
  }
}

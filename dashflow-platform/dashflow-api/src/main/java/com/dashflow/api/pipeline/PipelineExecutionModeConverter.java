package com.dashflow.api.pipeline;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class PipelineExecutionModeConverter
    implements AttributeConverter<PipelineExecutionMode, String> {

  @Override
  public String convertToDatabaseColumn(PipelineExecutionMode attribute) {
    return attribute == null ? null : attribute.apiValue();
  }

  @Override
  public PipelineExecutionMode convertToEntityAttribute(String dbData) {
    return dbData == null ? null : PipelineExecutionMode.fromApi(dbData);
  }
}

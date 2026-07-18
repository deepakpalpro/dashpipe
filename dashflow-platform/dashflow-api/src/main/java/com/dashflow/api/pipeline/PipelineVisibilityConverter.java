package com.dashflow.api.pipeline;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class PipelineVisibilityConverter implements AttributeConverter<PipelineVisibility, String> {

  @Override
  public String convertToDatabaseColumn(PipelineVisibility attribute) {
    return attribute == null ? null : attribute.apiValue();
  }

  @Override
  public PipelineVisibility convertToEntityAttribute(String dbData) {
    return dbData == null ? null : PipelineVisibility.fromApi(dbData);
  }
}

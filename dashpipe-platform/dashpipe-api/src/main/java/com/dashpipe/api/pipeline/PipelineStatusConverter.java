package com.dashpipe.api.pipeline;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class PipelineStatusConverter implements AttributeConverter<PipelineStatus, String> {

  @Override
  public String convertToDatabaseColumn(PipelineStatus attribute) {
    return attribute == null ? null : attribute.apiValue();
  }

  @Override
  public PipelineStatus convertToEntityAttribute(String dbData) {
    return dbData == null ? null : PipelineStatus.fromApi(dbData);
  }
}

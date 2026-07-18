package com.dashpipe.api.pipeline;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record ReplacePipelineStepsRequest(@NotEmpty @Valid List<PipelineStepRequest> steps) {}

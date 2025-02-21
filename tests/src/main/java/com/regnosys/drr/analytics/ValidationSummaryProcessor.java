package com.regnosys.drr.analytics;

import java.util.List;

public interface ValidationSummaryProcessor {
    List<ValidationData> processValidation(TransformData transformData);
}


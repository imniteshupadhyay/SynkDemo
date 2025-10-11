package com.playmotech.api.core.validation;

import org.springframework.stereotype.Component;

@Component
public class BulkUploadHistoryValidation {

    public boolean isValidId(Long id) {
        return id != null && id > 0;
    }
}

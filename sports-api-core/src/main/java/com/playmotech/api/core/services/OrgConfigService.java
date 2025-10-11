package com.playmotech.api.core.services;

import com.playmotech.api.core.response.ServiceResponse;

public interface OrgConfigService {

	ServiceResponse setCheckPendingDue(Boolean status);

}

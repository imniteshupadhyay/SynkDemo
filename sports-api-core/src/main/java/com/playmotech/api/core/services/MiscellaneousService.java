package com.playmotech.api.core.services;

import java.util.List;

import com.playmotech.api.core.response.ServiceResponse;

public interface MiscellaneousService {

	ServiceResponse getAllAgeCategories();

	ServiceResponse getAllGenders();

	ServiceResponse getAllSports();

	ServiceResponse getSportsByAcademy(String academyId);

	ServiceResponse getSportsByAcademies(List<String> academyIds);

}

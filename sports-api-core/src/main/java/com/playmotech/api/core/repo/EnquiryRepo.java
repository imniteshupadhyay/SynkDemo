package com.playmotech.api.core.repo;

import java.util.List;

import org.socialsignin.spring.data.dynamodb.repository.EnableScan;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.playmotech.api.core.constants.EnquiryStatus;
import com.playmotech.api.core.dao_postgres.Enquiry;

@Repository
@EnableScan
public interface EnquiryRepo extends CrudRepository<Enquiry, String> {
	List<Enquiry> findByUserProfile_IdAndStatus(String userId, EnquiryStatus enquiryStatus);

	List<Enquiry> findByUserProfile_Id(String userId);

	List<Enquiry> findByAcademy_Id(String academyId);

	List<Enquiry> findByCourse_IdIn(List<String> courseIds);

	List<Enquiry> findByCourse_IdInAndStatus(List<String> courseIds, EnquiryStatus enquiryStatus);

	List<Enquiry> findByAcademy_IdAndStatus(String academyId, EnquiryStatus enquiryStatus);
}

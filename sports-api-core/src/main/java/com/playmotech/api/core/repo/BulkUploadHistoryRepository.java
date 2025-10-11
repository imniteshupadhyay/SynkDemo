package com.playmotech.api.core.repo;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.playmotech.api.core.dao_postgres.BulkUploadHistory;

public interface BulkUploadHistoryRepository extends JpaRepository<BulkUploadHistory, Long>, JpaSpecificationExecutor<BulkUploadHistory> {

    Optional<BulkUploadHistory> findByIdAndDeletedIsFalse(Long id);

}

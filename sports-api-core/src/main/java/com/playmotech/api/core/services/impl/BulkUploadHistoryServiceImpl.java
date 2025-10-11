package com.playmotech.api.core.services.impl;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.playmotech.api.core.dao_postgres.BulkUploadHistory;
import com.playmotech.api.core.mapper.BulkUploadHistoryMapper;
import com.playmotech.api.core.repo.BulkUploadHistoryRepository;
import com.playmotech.api.core.response.ApiResponse;
import com.playmotech.api.core.response.ResponseBuilder;
import com.playmotech.api.core.response.ServiceResponse;
import com.playmotech.api.core.response.dao.BulkUploadHistoryDao;
import com.playmotech.api.core.services.BulkHistoryService;
import com.playmotech.api.core.specification.BulkUploadHistorySpecification;
import com.playmotech.api.core.utils.AcademyDomainUtil;
import com.playmotech.api.core.utils.GenericFilter;
import com.playmotech.api.core.validation.BulkUploadHistoryValidation;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Service
@RequiredArgsConstructor
public class BulkUploadHistoryServiceImpl implements BulkHistoryService {

    private final BulkUploadHistoryRepository bulkUploadHistoryRepository;
    private final BulkUploadHistoryValidation validationUtil;
    private final AcademyDomainUtil academyDomainUtil;

    @Override
    public ServiceResponse getHistory(GenericFilter filter) {
        try {
            log.info(ApiResponse.START_GET_BULK_UPLOAD_HISTORY_LIST.getMessage());

    		// Get user role for current domain
			String userRole = academyDomainUtil.getCurrentUserRoleName(filter.getDomainUrl());
			filter.setUserRole(userRole);

            
            BulkUploadHistorySpecification specification = new BulkUploadHistorySpecification(filter);
            List<BulkUploadHistory> historyList;
            Page<BulkUploadHistory> pageableContent = null;

            if (filter.isPageable()) {
                PageRequest page = PageRequest.of(filter.getCurrentPage() - 1, filter.getPageSize());
                pageableContent = bulkUploadHistoryRepository.findAll(specification, page);
                historyList = pageableContent.getContent();
            } else {
                historyList = bulkUploadHistoryRepository.findAll(specification);
            }

            if (historyList.isEmpty()) {
                log.info(ApiResponse.BULK_UPLOAD_HISTORY_NOT_FOUND.getMessage());
                return ResponseBuilder.success(ApiResponse.BULK_UPLOAD_HISTORY_NOT_FOUND, HttpStatus.OK);
            }

            List<BulkUploadHistoryDao> historyListDao = historyList.stream()
                    .map(BulkUploadHistoryMapper::mapEntityToDao)
                    .collect(Collectors.toList());

            if (filter.isPageable()) {
                return ResponseBuilder.success(historyListDao, ApiResponse.FETCHED_LIST, HttpStatus.OK,
                        pageableContent.getTotalPages(), pageableContent.getTotalElements());
            }
            return ResponseBuilder.success(historyListDao, ApiResponse.FETCHED_LIST, HttpStatus.OK);
        } catch (Exception e) {
            log.error(ApiResponse.ERROR_FETCHING_LIST.getMessage() + e.getMessage());
            return ResponseBuilder.internalServerError(ApiResponse.ERROR_FETCHING_LIST);
        }
    }

    @Override
    public ServiceResponse getHistoryById(Long historyId) {
        try {
            log.info(ApiResponse.START_GET_BULK_UPLOAD_HISTORY.getMessage());

            if (!validationUtil.isValidId(historyId)) {
                return ResponseBuilder.badRequest(ApiResponse.INVALID_REQUEST);
            }
            Optional<BulkUploadHistory> historyOptional = bulkUploadHistoryRepository.findByIdAndDeletedIsFalse(historyId);

            if (historyOptional.isEmpty()) {
                log.info(ApiResponse.BULK_UPLOAD_HISTORY_NOT_FOUND.getMessage());
                return ResponseBuilder.notFound(ApiResponse.BULK_UPLOAD_HISTORY_NOT_FOUND);
            }

            BulkUploadHistoryDao dao = BulkUploadHistoryMapper.mapEntityToDao(historyOptional.get());
            return ResponseBuilder.success(dao, ApiResponse.BULK_UPLOAD_HISTORY_FETCHED);

        } catch (Exception e) {
            log.error(ApiResponse.ERROR_BULK_UPLOAD_HISTORY_FETCHED.getMessage() + e.getMessage());
            return ResponseBuilder.internalServerError(ApiResponse.INTERNAL_SERVER);
        }
    }
}
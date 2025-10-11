package com.playmotech.api.core.mapper;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.BeanUtils;

import com.playmotech.api.core.dao_postgres.PendingPaymentDueView;
import com.playmotech.api.core.response.dao.DuePaymentsViewDao;
import com.playmotech.api.core.response.dao.PaymentDetailsViewDao;
import com.playmotech.api.core.response.dao.PendingPaymentDueViewDao;
import com.playmotech.api.core.views.DuePaymentsView;
import com.playmotech.api.core.views.PaymentDetailsView;

public class PaymentViewMapper {

	// Method to map a single PaymentView entity to PaymentViewDao
	public static PaymentDetailsViewDao mapToDao(PaymentDetailsView paymentView) {
		PaymentDetailsViewDao paymentViewDao = new PaymentDetailsViewDao();
		try {
			BeanUtils.copyProperties(paymentView, paymentViewDao);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return paymentViewDao;
	}

	// Method to map a list of PaymentView entities to a list of PaymentViewDao
	// objects
	public static List<PaymentDetailsViewDao> mapListToDaoList(List<PaymentDetailsView> paymentViews) {
		return paymentViews.stream().map(PaymentViewMapper::mapToDao).collect(Collectors.toList());
	}

	// Method to map a single PaymentView entity to PaymentViewDao
	public static PendingPaymentDueViewDao mapToDao(PendingPaymentDueView pendingView) {
		PendingPaymentDueViewDao pendingViewDao = new PendingPaymentDueViewDao();
		try {
			BeanUtils.copyProperties(pendingView, pendingViewDao);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return pendingViewDao;
	}

	public static DuePaymentsViewDao mapToDueDao(DuePaymentsView pendingView) {
		DuePaymentsViewDao pendingViewDao = new DuePaymentsViewDao();
		try {
			BeanUtils.copyProperties(pendingView, pendingViewDao);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return pendingViewDao;
	}

	public static List<PendingPaymentDueViewDao> mapPendingListToDaoList(List<PendingPaymentDueView> pendingDues) {
		return pendingDues.stream().map(PaymentViewMapper::mapToDao).collect(Collectors.toList());
	}

	public static List<DuePaymentsViewDao> mapDuePaymentsListToDaoList(List<DuePaymentsView> pendingDues) {
		return pendingDues.stream().map(PaymentViewMapper::mapToDueDao).collect(Collectors.toList());
	}
}

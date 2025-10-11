package com.playmotech.api.core.dto;

import java.time.LocalDate;

import lombok.Data;

@Data
// Helper class to hold installment information
public class InstallmentInfo {
	private final int installmentNumber;
	private final Long amount;
	private final LocalDate dueDate;

	public InstallmentInfo(int installmentNumber, long amount, LocalDate dueDate) {
		this.installmentNumber = installmentNumber;
		this.amount = amount;
		this.dueDate = dueDate;
	}

	public int getInstallmentNumber() {
		return installmentNumber;
	}

	public long getAmount() {
		return amount;
	}

	public LocalDate getDueDate() {
		return dueDate;
	}
}
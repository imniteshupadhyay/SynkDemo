package com.playmotech.api.core.dto;

import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class ReceiptDto {
	private String customerName; // Who is paying
	private String myCompanyName; // Who is getting paid
	private LocalDateTime transactionDateTime;
	private String customerLogoPath; // Path to customer company logo
	private List<TransactionItem> items;
	private double totalAmount;
	private String receiptId;

	public static class TransactionItem {
		private String description;
		private double amount;

		public TransactionItem(String description, double amount) {
			this.description = description;
			this.amount = amount;
		}

		// Getters and setters
		public String getDescription() {
			return description;
		}

		public double getAmount() {
			return amount;
		}
	}
}

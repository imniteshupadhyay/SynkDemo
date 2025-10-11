package com.playmotech.api.core.controllers;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;

@RestController
@RequestMapping("/api/csv")
public class CsvCompareController {

	private String formatDate(String inputDate) {
		if (inputDate == null || inputDate.isBlank())
			return null;
		DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
		LocalDate parsedDate = LocalDate.parse(inputDate, inputFormatter);
		LocalDateTime dateTime = parsedDate.atStartOfDay();
		DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
		return dateTime.format(outputFormatter);
	}

	@PostMapping("/compare")
	public ResponseEntity<byte[]> compareCsvFiles(@RequestParam("clientCsv") MultipartFile clientCsv,
			@RequestParam("dbCsv") MultipartFile dbCsv, @RequestParam("outputPath") String outputPath,
			@RequestParam("fileName") String fileName) throws IOException {

		Map<String, String[]> dbData = new HashMap<>();
		StringBuilder sqlBuilder = new StringBuilder();
		List<String[]> missingRows = new ArrayList<>();

		try (CSVReader dbReader = new CSVReader(new InputStreamReader(dbCsv.getInputStream()));
				CSVReader clientReader = new CSVReader(new InputStreamReader(clientCsv.getInputStream()))) {

			// Read DB CSV
			String[] dbHeader = dbReader.readNext();
			int dbIdIdx = Arrays.asList(dbHeader).indexOf("id");
			int dbNameIdx = Arrays.asList(dbHeader).indexOf("display_name");
			int dbPhoneIdx = Arrays.asList(dbHeader).indexOf("phone_number");
			int dbJoinIdx = Arrays.asList(dbHeader).indexOf("joining_date");
			int dbDueIdx = Arrays.asList(dbHeader).indexOf("due_date");

			String[] dbRow;
			while ((dbRow = dbReader.readNext()) != null) {
				String key = dbRow[dbNameIdx].trim().toLowerCase() + "_" + dbRow[dbPhoneIdx].trim();
				dbData.put(key, dbRow);
			}

			// Read Client CSV
			String[] clientHeader = clientReader.readNext();
			int clientNameIdx = Arrays.asList(clientHeader).indexOf("Full Name");
			int clientPhoneIdx = Arrays.asList(clientHeader).indexOf("Phone Number");
			int clientJoinIdx = Arrays.asList(clientHeader).indexOf("Joining Date");
			int clientDueIdx = Arrays.asList(clientHeader).indexOf("Next Due Date");

			// Add header to missingRows list
			missingRows.add(clientHeader);

			String[] clientRow;
			while ((clientRow = clientReader.readNext()) != null) {
				String name = clientRow[clientNameIdx].trim();
				String phone = clientRow[clientPhoneIdx].trim();
				String join = clientRow[clientJoinIdx].trim();
				String due = clientRow[clientDueIdx].trim();
				String key = name.toLowerCase() + "_" + phone;

				if (dbData.containsKey(key)) {
					// Existing record → check for updates
					String[] dbRowEntry = dbData.get(key);
					String id = dbRowEntry[dbIdIdx].trim();
					String dbJoin = dbRowEntry[dbJoinIdx].trim();
					String dbDue = dbRowEntry[dbDueIdx].trim();

					List<String> setParts = new ArrayList<>();
					if (!dbJoin.equalsIgnoreCase(join)) {
						setParts.add(String.format("joining_date = '%s'", formatDate(join)));
					}
					if (!dbDue.equalsIgnoreCase(due)) {
						setParts.add(String.format("due_date = '%s'", formatDate(due)));
					}

					if (!setParts.isEmpty()) {
						sqlBuilder.append(String.format("UPDATE trainee_course_enrollments SET %s WHERE id = '%s';\n",
								String.join(", ", setParts), id));
					}
				} else {
					// Not present in DB → add to missingRows
					missingRows.add(clientRow);
				}
			}

			// Write the SQL file
			Path outputFilePath = Path.of(outputPath, fileName);
			Files.write(outputFilePath, sqlBuilder.toString().getBytes());

			// Write the missingRows to a new CSV file
			String missingFileName = "missing_" + fileName.replace(".sql", ".csv");
			Path missingFilePath = Path.of(outputPath, missingFileName);
			try (CSVWriter writer = new CSVWriter(new OutputStreamWriter(Files.newOutputStream(missingFilePath)))) {
				writer.writeAll(missingRows);
			}

			return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileName)
					.contentType(MediaType.TEXT_PLAIN).body(sqlBuilder.toString().getBytes());

		} catch (Exception e) {
			e.printStackTrace();
			throw new IOException("Failed to process CSV files: " + e.getMessage(), e);
		}
	}

}

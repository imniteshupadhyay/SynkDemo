package com.playmotech.api.core.utils;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Random;

import javax.imageio.ImageIO;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PiePlot;
import org.jfree.data.general.DefaultPieDataset;

public class ChartUtils {

	private static final String ALPHABETS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
	private static final Random RANDOM = new Random();

	public static String generateReceiptNumber() {
		StringBuilder receiptNumber = new StringBuilder();

		// Generate first 2 characters as alphabets
		for (int i = 0; i < 2; i++) {
			receiptNumber.append(ALPHABETS.charAt(RANDOM.nextInt(ALPHABETS.length())));
		}

		// Generate last 4 characters as numbers
		for (int i = 0; i < 6; i++) {
			receiptNumber.append(RANDOM.nextInt(10));
		}

		return receiptNumber.toString();
	}

//	public static void main(String[] args) {
//		System.out.println(generateReceiptNumber());
//	}

	public static void createAndSaveDonutChart(String fileName, int scoreAchieved, int totalScore) {
		int percentage1 = (int) ((scoreAchieved / (double) totalScore) * 100);
		int percentage2 = 100 - percentage1;
		String centerText = String.format("%d/%d", scoreAchieved, totalScore);

		// Create dataset
		DefaultPieDataset dataset = new DefaultPieDataset();
		dataset.setValue("Category A", percentage1); // First color
		dataset.setValue("Category B", percentage2); // Second color

		// Create chart
		JFreeChart chart = createChart(dataset);

		// Customize the chart
		PiePlot plot = (PiePlot) chart.getPlot();
		plot.setCircular(true);
		plot.setLabelGap(0.02);

		// Set two categories with specific colors (green and blue)
		plot.setSectionPaint("Category A", new Color(76, 175, 80)); // Green
		plot.setSectionPaint("Category B", new Color(33, 150, 243)); // Blue

		// Hide category labels
		plot.setLabelGenerator(null);

		// Convert it to a donut chart
		plot.setSectionOutlinesVisible(false);
		plot.setInteriorGap(0.30); // Creates the 'donut' effect

		// Set chart background and plot background to white
		chart.setBackgroundPaint(Color.WHITE);
		plot.setBackgroundPaint(Color.WHITE);

		// Remove plot border
		plot.setOutlineStroke(null);
		plot.setOutlinePaint(null);

		// Save the chart as PNG with text and a white background
		saveChartAsPNG(chart, fileName, centerText);
	}

	private static JFreeChart createChart(DefaultPieDataset dataset) {
		return ChartFactory.createRingChart(null, // No title for the chart
				dataset, // Data
				false, // Exclude legend
				true, false);
	}

	private static void saveChartAsPNG(JFreeChart chart, String fileName, String centerText) {
		File imageFile = new File(fileName);
		try {
			int imageWidth = 300; // Reduced width
			int imageHeight = 300; // Reduced height

			// Create a BufferedImage with a white background
			BufferedImage image = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_RGB);
			Graphics2D g2 = image.createGraphics();

			// Fill background with white color
			g2.setColor(Color.WHITE);
			g2.fillRect(0, 0, image.getWidth(), image.getHeight());

			// Draw the chart onto the image in a reduced rectangle
			chart.draw(g2, new Rectangle(0, 0, imageWidth, imageHeight));

			// Set font and color for the text
			g2.setFont(new Font("Arial", Font.BOLD, 20)); // Adjusted font size
			g2.setColor(Color.BLACK);

			// Measure text size
			FontMetrics metrics = g2.getFontMetrics(g2.getFont());
			int x = (imageWidth - metrics.stringWidth(centerText)) / 2;
			int y = (imageHeight - metrics.getHeight()) / 2 + metrics.getAscent();

			// Draw text in the center of the image
			g2.drawString(centerText, x, y);

			g2.dispose();

			// Save the image
			ImageIO.write(image, "png", imageFile);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}

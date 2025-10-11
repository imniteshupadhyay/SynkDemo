package com.playmotech.api.core.utils;

import java.util.Random;

/**
 * Utility class for geospatial calculations
 */
public class GeoUtils {

	private static final double EARTH_RADIUS_METERS = 6371000;
	private static final Random RANDOM = new Random();

	public static double calculateDistanceInMeters(double lat1, double lon1, double lat2, double lon2) {
		double lat1Rad = Math.toRadians(lat1);
		double lon1Rad = Math.toRadians(lon1);
		double lat2Rad = Math.toRadians(lat2);
		double lon2Rad = Math.toRadians(lon2);

		double dLat = lat2Rad - lat1Rad;
		double dLon = lon2Rad - lon1Rad;

		double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
				+ Math.cos(lat1Rad) * Math.cos(lat2Rad) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
		double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

		return EARTH_RADIUS_METERS * c;
	}

	public static boolean isPointWithinRadius(double pointLat, double pointLon, double centerLat, double centerLon,
			long radiusInMeters) {
		double distance = calculateDistanceInMeters(pointLat, pointLon, centerLat, centerLon);
		return distance <= radiusInMeters;
	}

	/**
	 * Generates a random coordinate within the specified max distance from center
	 */
	public static double[] generateRandomCoordinateNear(double centerLat, double centerLon, double maxDistanceMeters) {
		// Random angle and distance
		double angle = 2 * Math.PI * RANDOM.nextDouble();
		double distance = maxDistanceMeters * (0.5 + RANDOM.nextDouble()); // Between 0.5x and 1.5x of radius

		// Convert distance to degrees (approximation)
		double deltaLat = (distance / EARTH_RADIUS_METERS) * (180 / Math.PI);
		double deltaLon = deltaLat / Math.cos(Math.toRadians(centerLat));

		double lat = centerLat + deltaLat * Math.sin(angle);
		double lon = centerLon + deltaLon * Math.cos(angle);

		return new double[] { lat, lon };
	}

//	public static void main(String[] args) {
//		// Geo-fence center
//		double centerLat = 40.7128;
//		double centerLon = -74.0060;
//
//		// Set radius for both geofence and test point generation
//		long radiusInMeters = 100;
//
//		System.out.printf("Geo-Fence Center: (%.6f, %.6f) | Radius: %d meters%n", centerLat, centerLon,
//				radiusInMeters);
//		System.out.println("--------------------------------------------------------");
//
//		// Generate and test 10 random points around the center
//		for (int i = 1; i <= 10; i++) {
//			double[] point = generateRandomCoordinateNear(centerLat, centerLon, radiusInMeters);
//			double lat = point[0];
//			double lon = point[1];
//
//			double distance = calculateDistanceInMeters(lat, lon, centerLat, centerLon);
//			boolean withinRadius = isPointWithinRadius(lat, lon, centerLat, centerLon, radiusInMeters);
//
//			System.out.printf("Point %2d -> (%.6f, %.6f) | Distance: %6.2f m | Within Radius: %b%n",
//					i, lat, lon, distance, withinRadius);
//		}
//	}
}

package com.ecotrack.service;

import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Random;

@Service
public class TrafficService {

    private final Random random = new Random();

    public int getTraffic(double lat, double lon, String roadType) {
        int freeFlow = getFreeFlowSpeed(roadType);
        int avgSpeed = getHistoricalSpeed(roadType);

        // Calculate congestion %
        double congestion = (1.0 - (double) avgSpeed / freeFlow) * 100.0;

        // Add slight randomness so results aren't identical
        double locationFactor = ((lat * 1000) % 10 + (lon * 1000) % 10) / 2.0; // 0-10
        int varied = (int) Math.max(0, Math.min(100, congestion + locationFactor + random.nextInt(15) - 7));

        return varied;
    }

    private int getFreeFlowSpeed(String roadType) {
        return switch (roadType.toLowerCase()) {
            case "highway" -> 100;
            case "primary" -> 60;
            case "secondary" -> 40;
            case "residential" -> 30;
            default -> 50;
        };
    }

    private int getHistoricalSpeed(String roadType) {
        int hour = LocalTime.now().getHour();
        DayOfWeek day = LocalDate.now().getDayOfWeek();
        boolean isWeekend = (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY);

        // Rush hours apply only on weekdays
        boolean morningPeak = !isWeekend && (hour >= 7 && hour <= 10);
        boolean eveningPeak = !isWeekend && (hour >= 16 && hour <= 19);

        return switch (roadType.toLowerCase()) {
            case "highway" -> morningPeak ? 60 : eveningPeak ? 70 : 90;
            case "primary" -> morningPeak ? 30 : eveningPeak ? 35 : 55;
            case "secondary" -> morningPeak ? 25 : eveningPeak ? 25 : 35;
            case "residential" -> morningPeak ? 15 : eveningPeak ? 20 : 25;
            default -> 40;
        };
    }
}
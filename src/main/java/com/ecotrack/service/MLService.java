package com.ecotrack.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Service
public class MLService {
    private static final Logger logger = LoggerFactory.getLogger(MLService.class);

    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${ml.script.path:python_scripts/health_score.py}")
    private String mlScriptPath;

    @Value("${ml.python.bin:python3}")
    private String pythonBin;

    @Value("${ml.timeout.ms:30000}")
    private long timeoutMs;

    public Map<String, Object> predict(Map<String, Object> inputData) {
        return callPythonScript(inputData, "health_score");
    }

    public Map<String, Object> getHealthAdvice(Map<String, Object> userProfile,
                                               Map<String, Object> routeData,
                                               Map<String, Object> realtimeAqi) {
        Map<String, Object> requestData = new HashMap<>();
        requestData.put("request_type", "health_advice");
        requestData.put("user_profile", userProfile);
        requestData.put("route_data", routeData);
        requestData.put("realtime_aqi", realtimeAqi);

        return callPythonScript(requestData, "health_advice");
    }

    public Map<String, Object> getPollutionAlerts(Map<String, Object> location) {
        Map<String, Object> requestData = new HashMap<>();
        requestData.put("request_type", "pollution_alerts");
        requestData.put("location", location);

        return callPythonScript(requestData, "pollution_alerts");
    }

    private Map<String, Object> callPythonScript(Map<String, Object> inputData, String requestType) {
        long startTime = System.currentTimeMillis();

        try {
            String inputJson = mapper.writeValueAsString(inputData);
            logger.debug("Sending to Python script: {}", inputJson);

            // Set working directory to project root
            ProcessBuilder pb = new ProcessBuilder(pythonBin, mlScriptPath);
            pb.redirectErrorStream(true);
            pb.directory(new File(System.getProperty("user.dir")));

            Process process = pb.start();

            // Write JSON to Python process
            try (BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8))) {
                writer.write(inputJson);
                writer.flush();
            }

            // Read response from Python
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line);
                }
            }

            // Wait for process with timeout
            boolean finished = process.waitFor(timeoutMs, java.util.concurrent.TimeUnit.MILLISECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new RuntimeException("Python script timed out after " + timeoutMs + "ms");
            }

            int exitCode = process.exitValue();
            if (exitCode != 0) {
                logger.error("Python script exited with code {}: {}", exitCode, output);
                throw new RuntimeException("Python script failed with exit code: " + exitCode);
            }

            // Parse response
            Map<String, Object> result = mapper.readValue(output.toString(), Map.class);

            if (!Boolean.TRUE.equals(result.get("success"))) {
                logger.error("Python script returned error: {}", result.get("error"));
                throw new RuntimeException("Python script failed: " + result.get("error"));
            }

            long executionTime = System.currentTimeMillis() - startTime;
            logger.info("Python script executed successfully in {}ms", executionTime);

            return result;

        } catch (Exception e) {
            logger.error("Python script execution failed for {}: {}", requestType, e.getMessage());

            // Return enhanced fallback response
            return createEnhancedFallback(inputData, requestType, e);
        }
    }

    private Map<String, Object> createEnhancedFallback(Map<String, Object> inputData, String requestType, Exception e) {
        Map<String, Object> fallback = new HashMap<>();
        fallback.put("success", false);
        fallback.put("error", e.getMessage());
        fallback.put("fallback", true);
        fallback.put("request_type", requestType);
        fallback.put("timestamp", System.currentTimeMillis());

        if ("health_score".equals(requestType)) {
            // Enhanced fallback calculation
            double aqi = Double.parseDouble(inputData.getOrDefault("aqi", "50").toString());
            double distance = Double.parseDouble(inputData.getOrDefault("distance_km", "5").toString());
            double duration = Double.parseDouble(inputData.getOrDefault("duration_min", "30").toString());

            // More sophisticated fallback formula
            double score = 100 - (aqi * 0.3) - (distance * 0.5) - (duration * 0.2);
            score = Math.max(0, Math.min(100, score));

            fallback.put("score", score);
            fallback.put("calculation_method", "enhanced_rule_based_fallback");
        }

        return fallback;
    }

    public Map<String, Object> getSystemStatus() {
        Map<String, Object> status = new HashMap<>();

        // Check if Python script is accessible
        File scriptFile = new File(mlScriptPath);
        status.put("python_script_exists", scriptFile.exists());
        status.put("python_script_path", mlScriptPath);

        // Check Python executable
        try {
            Process process = new ProcessBuilder(pythonBin, "--version").start();
            int exitCode = process.waitFor();
            status.put("python_available", exitCode == 0);
        } catch (Exception e) {
            status.put("python_available", false);
            status.put("python_error", e.getMessage());
        }

        status.put("timestamp", System.currentTimeMillis());
        return status;
    }
}
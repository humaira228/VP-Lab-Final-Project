package com.ecotrack.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.ResourceAccessException;

import java.util.Map;
import java.util.HashMap;

@Service
public class MLIntegrationService {
    private static final Logger logger = LoggerFactory.getLogger(MLIntegrationService.class);

    @Value("${ml.service.url}")
    private String mlServiceUrl;

    @Value("${ml.service.timeout}")
    private int mlServiceTimeout;

    private final RestTemplate restTemplate;

    public MLIntegrationService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public Map<String, Object> getHealthPrediction(Map<String, Object> features) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(features, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    mlServiceUrl + "/predict",
                    request,
                    Map.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            }
        } catch (ResourceAccessException e) {
            logger.warn("ML service timeout: {}", e.getMessage());
        } catch (Exception e) {
            logger.error("Error calling ML service: {}", e.getMessage());
        }

        return null;
    }
}
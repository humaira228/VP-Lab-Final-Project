package com.ecotrack.service;

import com.ecotrack.dto.AqiResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class AqiService {

    @Value("${aqicn.token}")
    private String token;

    private final RestTemplate restTemplate = new RestTemplate();

    public int getAqi(double lat, double lon) {
        String url = String.format(
                "https://api.waqi.info/feed/geo:%s;%s/?token=%s",
                lat, lon, token
        );

        AqiResponse response = restTemplate.getForObject(url, AqiResponse.class);

        if (response != null && "ok".equalsIgnoreCase(response.status())) {
            return response.data().aqi();
        }

        return -1; // Error fallback
    }
}

package com.ecotrack.controller;

import com.ecotrack.service.AqiService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AqiController {

    private final AqiService aqiService;

    public AqiController(AqiService aqiService) {
        this.aqiService = aqiService;
    }

    @GetMapping("/api/aqi")
    public int getAqi(@RequestParam double lat, @RequestParam double lon) {
        return aqiService.getAqi(lat, lon);
    }
}
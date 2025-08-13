package com.ecotrack.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ORSResponse {
    public List<Feature> features;

    public static class Feature {
        public Geometry geometry;
        public Properties properties;
    }

    public static class Geometry {
        public List<List<Double>> coordinates;
    }

    public static class Properties {
        public Summary summary;
    }

    public static class Summary {
        public double distance;
        public double duration;
    }
}
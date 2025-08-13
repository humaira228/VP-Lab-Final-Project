package com.ecotrack.dto;

import java.util.List;

public class RouteResponse {
    private List<RouteOption> routes;

    public RouteResponse(List<RouteOption> routes) {
        this.routes = routes;
    }

    public List<RouteOption> getRoutes() {
        return routes;
    }

    public void setRoutes(List<RouteOption> routes) {
        this.routes = routes;
    }
}


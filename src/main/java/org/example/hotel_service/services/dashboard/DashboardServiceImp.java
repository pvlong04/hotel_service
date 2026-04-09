package org.example.hotel_service.services.dashboard;

import org.example.hotel_service.dtos.response.DashboardOverviewResponse;
import org.springframework.security.oauth2.jwt.Jwt;

public interface DashboardServiceImp {
    DashboardOverviewResponse getOverview(Jwt jwt);
}


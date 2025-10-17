package com.example.HockeyPredictor.controller;

import com.example.HockeyPredictor.service.SportsRadarService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DataUpdateController {

    private final SportsRadarService sportsRadarService;

    public DataUpdateController(SportsRadarService sportsRadarService) {
        this.sportsRadarService = sportsRadarService;
    }

    @GetMapping("/api/update-data")
    public String updateData() {
        sportsRadarService.updateAllFromSportsRadar();
        return "Data update triggered. Check logs for details.";
    }
}

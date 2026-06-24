package com.estapar.parking.web;

import com.estapar.parking.service.RevenueService;
import com.estapar.parking.web.dto.RevenueResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Map;

/**
 * Exposição dos endpoints de saúde e faturamento da aplicação.
 */
@RestController
@RequiredArgsConstructor
public class RevenueController {

    private final RevenueService revenueService;

    @GetMapping("/")
    public Map<String, String> status() {
        return Map.of(
                "status", "ok",
                "application", "estapar-parking",
                "message", "root endpoint alive"
        );
    }

    @GetMapping("/revenue")
    public RevenueResponse getRevenue(
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam("sector") String sector) {
        return revenueService.calculateRevenue(date, sector);
    }
}

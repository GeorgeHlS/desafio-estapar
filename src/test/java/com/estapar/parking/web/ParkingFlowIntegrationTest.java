package com.estapar.parking.web;

import com.estapar.parking.domain.Sector;
import com.estapar.parking.domain.Spot;
import com.estapar.parking.repository.SectorRepository;
import com.estapar.parking.repository.SpotRepository;
import com.estapar.parking.repository.ParkingSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ParkingFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SectorRepository sectorRepository;

    @Autowired
    private SpotRepository spotRepository;

    @Autowired
    private ParkingSessionRepository parkingSessionRepository;

    private static final double LAT = -23.561684;
    private static final double LNG = -46.655981;

    @BeforeEach
    void setUp() {
        parkingSessionRepository.deleteAll();
        spotRepository.deleteAll();
        sectorRepository.deleteAll();

        Sector sector = sectorRepository.save(Sector.builder()
                .name("A")
                .basePrice(new BigDecimal("10.00"))
                .maxCapacity(1)
                .build());

        spotRepository.save(Spot.builder()
                .externalId(1L)
                .sector(sector)
                .lat(LAT)
                .lng(LNG)
                .occupied(false)
                .build());
    }

    @Test
    void fullFlow_entryParkedExit_andRevenue() throws Exception {
        // ENTRY
        mockMvc.perform(post("/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "license_plate": "ZUL0001",
                                  "entry_time": "2025-01-01T12:00:00.000Z",
                                  "event_type": "ENTRY"
                                }
                                """))
                .andExpect(status().isOk());

        // PARKED
        mockMvc.perform(post("/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "license_plate": "ZUL0001",
                                  "lat": -23.561684,
                                  "lng": -46.655981,
                                  "event_type": "PARKED"
                                }
                                """))
                .andExpect(status().isOk());

        // EXIT (2 horas depois -> 2 * 10.00 = 20.00, sem fator de surge pois ocupação era 0 na entrada)
        mockMvc.perform(post("/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "license_plate": "ZUL0001",
                                  "exit_time": "2025-01-01T14:00:00.000Z",
                                  "event_type": "EXIT"
                                }
                                """))
                .andExpect(status().isOk());

        // REVENUE: ocupação na entrada era 0/1 = 0% -> fator 0.90; 2h * 10 * 0.90 = 18.00
        mockMvc.perform(get("/revenue")
                        .param("date", "2025-01-01")
                        .param("sector", "A"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currency").value("BRL"))
                .andExpect(jsonPath("$.amount").value(closeTo(18.00, 0.001)));
    }

    @Test
    void secondEntryWhileFull_isRejected() throws Exception {
        // Primeira entrada + parked ocupa a única vaga
        mockMvc.perform(post("/webhook").contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"license_plate":"AAA1111","entry_time":"2025-01-01T10:00:00.000Z","event_type":"ENTRY"}
                        """)).andExpect(status().isOk());
        mockMvc.perform(post("/webhook").contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"license_plate":"AAA1111","lat":-23.561684,"lng":-46.655981,"event_type":"PARKED"}
                        """)).andExpect(status().isOk());

        // Segunda entrada com a garagem (1 vaga) lotada -> 409 Conflict
        mockMvc.perform(post("/webhook").contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"license_plate":"BBB2222","entry_time":"2025-01-01T10:05:00.000Z","event_type":"ENTRY"}
                        """)).andExpect(status().isConflict());
    }

    @Test
    void rootEndpoint_returnsStatusOk() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("ok")))
                .andExpect(jsonPath("$.application", is("estapar-parking")));
    }

    @Test
    void revenue_unknownSector_returns422() throws Exception {
        mockMvc.perform(get("/revenue")
                        .param("date", "2025-01-01")
                        .param("sector", "Z"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void entry_duplicateOpenSession_returns422() throws Exception {
        mockMvc.perform(post("/webhook").contentType(MediaType.APPLICATION_JSON)
                .content("{\"license_plate\":\"AAA1111\",\"entry_time\":\"2025-01-01T10:00:00.000Z\",\"event_type\":\"ENTRY\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/webhook").contentType(MediaType.APPLICATION_JSON)
                .content("{\"license_plate\":\"AAA1111\",\"entry_time\":\"2025-01-01T10:05:00.000Z\",\"event_type\":\"ENTRY\"}"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void exit_withoutOpenSession_returns422() throws Exception {
        mockMvc.perform(post("/webhook").contentType(MediaType.APPLICATION_JSON)
                .content("{\"license_plate\":\"NOPLATE\",\"exit_time\":\"2025-01-01T10:00:00.000Z\",\"event_type\":\"EXIT\"}"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void parked_withAlreadyOccupiedSpot_returns422() throws Exception {
        var sector = sectorRepository.findByName("A").orElseThrow();
        spotRepository.save(Spot.builder()
                .externalId(2L)
                .sector(sector)
                .lat(LAT + 0.0001)
                .lng(LNG + 0.0001)
                .occupied(false)
                .build());

        mockMvc.perform(post("/webhook").contentType(MediaType.APPLICATION_JSON)
                .content("{\"license_plate\":\"AAA1111\",\"entry_time\":\"2025-01-01T10:00:00.000Z\",\"event_type\":\"ENTRY\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/webhook").contentType(MediaType.APPLICATION_JSON)
                .content(String.format("{\"license_plate\":\"AAA1111\",\"lat\":%s,\"lng\":%s,\"event_type\":\"PARKED\"}", LAT, LNG)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/webhook").contentType(MediaType.APPLICATION_JSON)
                .content("{\"license_plate\":\"BBB2222\",\"entry_time\":\"2025-01-01T10:05:00.000Z\",\"event_type\":\"ENTRY\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/webhook").contentType(MediaType.APPLICATION_JSON)
                .content(String.format("{\"license_plate\":\"BBB2222\",\"lat\":%s,\"lng\":%s,\"event_type\":\"PARKED\"}", LAT, LNG)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void webhook_invalidPayload_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/webhook").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"event_type\":\"ENTRY\"}"))
                .andExpect(status().isBadRequest());
    }
}

package ru.practicum.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.repository.StatsRepository;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
class StatsIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StatsRepository statsRepository;

    @Test
    void fullIntegrationTest() throws Exception {
        LocalDateTime now = LocalDateTime.now();

        String hitJson = String.format(
                "{\"app\": \"ewm-main-service\", \"uri\": \"/events/1\", \"ip\": \"192.168.1.1\", \"timestamp\": \"%s\"}",
                now.minusHours(1).toString()
        );

        mockMvc.perform(post("/hit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(hitJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.app").value("ewm-main-service"))
                .andExpect(jsonPath("$.uri").value("/events/1"));

        mockMvc.perform(get("/stats")
                        .param("start", now.minusDays(1).toString())
                        .param("end", now.toString())
                        .param("unique", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].app").value("ewm-main-service"))
                .andExpect(jsonPath("$[0].uri").value("/events/1"))
                .andExpect(jsonPath("$[0].hits").value(1));
    }
}
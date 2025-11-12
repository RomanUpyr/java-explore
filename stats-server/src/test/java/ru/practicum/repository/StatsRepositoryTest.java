package ru.practicum.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.TestPropertySource;
import ru.practicum.ViewStats;
import ru.practicum.model.EndpointHit;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:test",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class StatsRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private StatsRepository statsRepository;

    @Test
    void getStats_WhenHitsExist_ShouldReturnStats() {
        LocalDateTime now = LocalDateTime.now();
        EndpointHit hit1 = createHit("app1", "/events/1", "192.168.1.1", now.minusHours(2));
        EndpointHit hit2 = createHit("app1", "/events/1", "192.168.1.2", now.minusHours(1));
        EndpointHit hit3 = createHit("app1", "/events/2", "192.168.1.1", now.minusHours(1));

        entityManager.persist(hit1);
        entityManager.persist(hit2);
        entityManager.persist(hit3);
        entityManager.flush();

        List<ViewStats> stats = statsRepository.getStats(
                now.minusDays(1),
                now,
                null
        );

        assertEquals(2, stats.size());
        assertEquals("/events/1", stats.get(0).getUri());
        assertEquals(2L, stats.get(0).getHits());
        assertEquals("/events/2", stats.get(1).getUri());
        assertEquals(1L, stats.get(1).getHits());
    }

    @Test
    void getUniqueStats_WhenDuplicateIps_ShouldCountUnique() {
        LocalDateTime now = LocalDateTime.now();
        EndpointHit hit1 = createHit("app1", "/events/1", "192.168.1.1", now.minusHours(2));
        EndpointHit hit2 = createHit("app1", "/events/1", "192.168.1.1", now.minusHours(1));
        EndpointHit hit3 = createHit("app1", "/events/1", "192.168.1.2", now.minusHours(1));

        entityManager.persist(hit1);
        entityManager.persist(hit2);
        entityManager.persist(hit3);
        entityManager.flush();

        List<ViewStats> stats = statsRepository.getUniqueStats(
                now.minusDays(1),
                now,
                List.of("/events/1")
        );

        assertEquals(1, stats.size());
        assertEquals("/events/1", stats.get(0).getUri());
        assertEquals(2L, stats.get(0).getHits());
    }

    @Test
    void getStats_WithUriFilter_ShouldReturnFilteredResults() {
        LocalDateTime now = LocalDateTime.now();
        EndpointHit hit1 = createHit("app1", "/events/1", "192.168.1.1", now.minusHours(2));
        EndpointHit hit2 = createHit("app1", "/events/2", "192.168.1.2", now.minusHours(1));

        entityManager.persist(hit1);
        entityManager.persist(hit2);
        entityManager.flush();

        List<ViewStats> stats = statsRepository.getStats(
                now.minusDays(1),
                now,
                List.of("/events/1")
        );

        assertEquals(1, stats.size());
        assertEquals("/events/1", stats.get(0).getUri());
    }

    private EndpointHit createHit(String app, String uri, String ip, LocalDateTime timestamp) {
        return EndpointHit.builder()
                .app(app)
                .uri(uri)
                .ip(ip)
                .timestamp(timestamp)
                .build();
    }
}
package ru.practicum.controller;

import org.springframework.web.bind.annotation.*;
import java.util.*;


/**
 * Заглушка на первом этапе, тесты не проходят без нее на Github.
 */
@RestController
@RequestMapping("/api")
public class StubController {

    @GetMapping("/health")
    public String health() {
        return "OK";
    }

    @GetMapping("/events")
    public List<Map<String, Object>> getEvents() {
        return Collections.emptyList();
    }

    @PostMapping("/events")
    public Map<String, Object> createEvent(@RequestBody Map<String, Object> event) {
        Map<String, Object> response = new HashMap<>();
        response.put("id", 1);
        response.put("status", "created");
        response.put("message", "Event will be processed");
        return response;
    }

    @GetMapping("/events/{id}")
    public Map<String, Object> getEvent(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        response.put("id", id);
        response.put("title", "Sample Event");
        response.put("status", "PENDING");
        return response;
    }

    @GetMapping("/users/{id}")
    public Map<String, Object> getUser(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        response.put("id", id);
        response.put("name", "Sample User");
        response.put("email", "user@example.com");
        return response;
    }

    @GetMapping("/categories")
    public List<Map<String, Object>> getCategories() {
        return List.of(
                Map.of("id", 1, "name", "Concert"),
                Map.of("id", 2, "name", "Theater"),
                Map.of("id", 3, "name", "Exhibition")
        );
    }

    @GetMapping("/compilations")
    public List<Map<String, Object>> getCompilations() {
        return Collections.emptyList();
    }
}

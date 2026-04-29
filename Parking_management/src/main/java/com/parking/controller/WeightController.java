package com.parking.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@RestController
public class WeightController {

    @Value("${ai.model.base.url:http://localhost:5000}")
    private String flaskBaseUrl;

    private final WebClient webClient;

    public WeightController(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    // GET current weights — pass through to Flask
    @GetMapping("/api/admin/weights")
    @PreAuthorize("hasRole('IT_ADMIN')")
    public ResponseEntity<String> getWeights() {
        String result = webClient.get()
                .uri(flaskBaseUrl + "/weights")
                .retrieve()
                .bodyToMono(String.class)
                .block();
        return ResponseEntity.ok(result);
    }

    // POST updated weights — pass through to Flask
    @PostMapping("/api/admin/weights")
    @PreAuthorize("hasRole('IT_ADMIN')")
    public ResponseEntity<String> updateWeights(
            @RequestBody Map<String, Double> weights) {
        String result = webClient.post()
                .uri(flaskBaseUrl + "/weights")
                .bodyValue(weights)
                .retrieve()
                .bodyToMono(String.class)
                .block();
        return ResponseEntity.ok(result);
    }

    @PostMapping("/api/admin/weights/reset")
    @PreAuthorize("hasRole('IT_ADMIN')")
    public ResponseEntity<String> resetWeights() {
        String result = webClient.post()
                .uri(flaskBaseUrl + "/weights/reset")
                .retrieve()
                .bodyToMono(String.class)
                .block();
        return ResponseEntity.ok(result);
    }
}
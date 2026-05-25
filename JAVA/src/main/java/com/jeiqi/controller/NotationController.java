package com.jeiqi.controller;

import com.jeiqi.model.GameNotation;
import com.jeiqi.service.NotationService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/notations")
public class NotationController {

    private final NotationService notationService;

    public NotationController(NotationService notationService) {
        this.notationService = notationService;
    }

    @GetMapping
    public List<GameNotation> listNotations() {
        return notationService.listNotations();
    }

    @GetMapping("/{gameId}")
    public ResponseEntity<GameNotation> getNotation(@PathVariable String gameId) {
        Optional<GameNotation> notation = notationService.getNotation(gameId);
        return notation.map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
}

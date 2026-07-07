package com.jeiqi.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jeiqi.protocol.ProtocolHandler;
import com.jeiqi.repository.GameRepository;
import com.jeiqi.repository.NotationRepository;
import com.jeiqi.service.GameService;
import com.jeiqi.service.MatchmakingService;
import com.jeiqi.service.NotationService;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    public ProtocolHandler protocolHandler(ObjectMapper objectMapper) {
        return new ProtocolHandler(objectMapper);
    }

    @org.springframework.beans.factory.annotation.Value("${game.move-timeout-seconds:60}")
    private int moveTimeoutSeconds;

    @org.springframework.beans.factory.annotation.Value("${game.network-delay-tolerance-ms:5000}")
    private int networkDelayToleranceMs;

    @Bean
    public com.jeiqi.engine.TimerManager timerManager() {
        return new com.jeiqi.engine.TimerManager(moveTimeoutSeconds * 1000L, networkDelayToleranceMs);
    }

    @Bean
    public GameService gameService(GameRepository gameRepository, com.jeiqi.engine.TimerManager timerManager, org.springframework.context.ApplicationEventPublisher eventPublisher) {
        return new GameService(gameRepository, timerManager, eventPublisher);
    }

    @Bean
    public MatchmakingService matchmakingService(GameService gameService) {
        return new MatchmakingService(gameService);
    }

    @Bean
    public NotationService notationService(NotationRepository notationRepository) {
        return new NotationService(notationRepository);
    }
}

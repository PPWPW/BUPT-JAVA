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

    @Bean
    public GameService gameService(GameRepository gameRepository) {
        return new GameService(gameRepository);
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

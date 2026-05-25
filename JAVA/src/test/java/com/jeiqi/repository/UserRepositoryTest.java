package com.jeiqi.repository;

import com.jeiqi.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldSaveAndFindByUsername() {
        User user = new User();
        user.setUsername("player1");
        user.setPasswordHash("hash123");
        userRepository.save(user);

        Optional<User> found = userRepository.findByUsername("player1");
        assertTrue(found.isPresent());
        assertEquals("player1", found.get().getUsername());
        assertEquals("hash123", found.get().getPasswordHash());
    }

    @Test
    void shouldCheckExistsByUsername() {
        assertFalse(userRepository.existsByUsername("nonexistent"));

        User user = new User();
        user.setUsername("player2");
        user.setPasswordHash("hash456");
        userRepository.save(user);

        assertTrue(userRepository.existsByUsername("player2"));
    }

    @Test
    void shouldPersistStats() {
        User user = new User();
        user.setUsername("pro");
        user.setPasswordHash("pw");
        user.setWins(10);
        user.setLosses(5);
        user.setDraws(2);
        user.setTotalGames(17);
        userRepository.save(user);

        Optional<User> found = userRepository.findByUsername("pro");
        assertTrue(found.isPresent());
        assertEquals(10, found.get().getWins());
        assertEquals(5, found.get().getLosses());
        assertEquals(2, found.get().getDraws());
        assertEquals(17, found.get().getTotalGames());
        assertEquals(10.0 / 17.0, found.get().getWinRate(), 0.001);
    }
}

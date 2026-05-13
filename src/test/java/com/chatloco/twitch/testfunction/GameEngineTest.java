package com.chatloco.twitch.testfunction;

import com.chatloco.twitch.application.engine.GameEngine;
import com.chatloco.twitch.domain.model.GameRoom;
import com.chatloco.twitch.domain.model.GameState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

public class GameEngineTest {

    private GameEngine gameEngine;

    @BeforeEach
    void setup() {

        SimpMessagingTemplate messagingTemplate =
                mock(SimpMessagingTemplate.class);

        gameEngine =
                new GameEngine(messagingTemplate);
    }

    @Test
    void shouldResolveRound() {

        GameRoom room = gameEngine.createRoom("Streamer");

        gameEngine.addPlayer(
                room.getRoomId(),
                "carlos"
        );

        gameEngine.addPlayer(
                room.getRoomId(),
                "ana"
        );

        String option =
                room.getOptions().get(0);

        gameEngine.registerVote(
                room.getRoomId(),
                "carlos",
                option
        );

        gameEngine.registerVote(
                room.getRoomId(),
                "ana",
                option
        );

        gameEngine.resolveRound(
                room.getRoomId()
        );

        assertEquals(
                GameState.RESULT,
                room.getState()
        );
    }
}
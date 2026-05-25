package com.chatloco.twitch.testfunction;

import com.chatloco.twitch.application.engine.GameEngine;
import com.chatloco.twitch.application.engine.SituationEngine;
import com.chatloco.twitch.domain.model.GameRoom;
import com.chatloco.twitch.domain.model.GameState;
import com.chatloco.twitch.domain.model.SituationData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

public class GameEngineTest {

    private GameEngine gameEngine;

    @BeforeEach
    void setup() {

        SimpMessagingTemplate messagingTemplate =
                mock(SimpMessagingTemplate.class);

        SituationEngine situationService =
                mock(SituationEngine.class);

        SituationData data = new SituationData();

        data.setSituation("Test Situation");

        data.setOptions(List.of(
                "Option 1",
                "Option 2",
                "Option 3"
        ));

        when(situationService.getRandom())
                .thenReturn(data);

        gameEngine =
                new GameEngine(
                        messagingTemplate,
                        situationService
                );
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
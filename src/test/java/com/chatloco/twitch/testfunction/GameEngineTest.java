package com.chatloco.twitch.testfunction;

import com.chatloco.twitch.application.engine.GameEngine;
import com.chatloco.twitch.application.engine.SituationEngine;
import com.chatloco.twitch.domain.model.*;
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
                new OptionData("Option 1", 1, 1),
                new OptionData("Option 2", 2, -1),
                new OptionData("Option 3", -2, 3)
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

        OptionData option =
                room.getOptions().get(0);

        gameEngine.registerVote(
                room.getRoomId(),
                "carlos",
                option.getText()
        );

        gameEngine.registerVote(
                room.getRoomId(),
                "ana",
                option.getText()
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
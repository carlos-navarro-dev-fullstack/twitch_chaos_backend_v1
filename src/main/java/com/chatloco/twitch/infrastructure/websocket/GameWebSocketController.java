package com.chatloco.twitch.infrastructure.websocket;

import com.chatloco.twitch.application.dto.GameStateResponse;
import com.chatloco.twitch.application.dto.VoteMessage;
import com.chatloco.twitch.application.engine.GameEngine;
import com.chatloco.twitch.domain.model.GameRoom;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.Map;

@Controller
@RequiredArgsConstructor
public class GameWebSocketController {

    private final GameEngine gameEngine;

    private final SimpMessagingTemplate messagingTemplate;

    // =====================================================
    // 🔹 VOTE
    // =====================================================
    @MessageMapping("/vote")
    public void vote(VoteMessage payload) {
        GameRoom room = gameEngine.getRoom(
                payload.getRoomId()
        );

        if (room == null) {
            return;
        }

        gameEngine.registerVote(
                payload.getRoomId(),
                payload.getUsername(),
                payload.getOption()
        );

        messagingTemplate.convertAndSend(
                "/topic/rooms/" + room.getRoomId(),
                GameStateResponse.from(room)
        );
    }

    @MessageMapping("/state")
    public void getState(@Payload Map<String, String> payload) {

        String roomId = payload.get("roomId");

        GameStateResponse state = gameEngine.getGameState(roomId);

        messagingTemplate.convertAndSend(
                "/topic/rooms/" + roomId,
                state
        );
    }
}
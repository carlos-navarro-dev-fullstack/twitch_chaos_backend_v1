package com.chatloco.twitch.presentation.controller;

import com.chatloco.twitch.application.dto.StreamerChoiceRequest;
import com.chatloco.twitch.application.engine.GameEngine;
import com.chatloco.twitch.domain.model.GameRoom;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/game")
@RequiredArgsConstructor
public class GameController {

    private final GameEngine gameEngine;

    private final SimpMessagingTemplate messagingTemplate;

    // =====================================================
    // 🎮 CREATE ROOM
    // =====================================================
    @PostMapping("/create")
    public ResponseEntity<?> createRoom(@RequestParam String username) {

        GameRoom room = gameEngine.createRoom(username);

        System.out.println(room);

        return ResponseEntity.ok(room.getRoomId());
    }

    @PostMapping("/join")
    public ResponseEntity<?> joinRoom(
            @RequestParam String roomId,
            @RequestParam String username
    ) {

        GameRoom room = gameEngine.getRoom(roomId);

        if (room == null) {
            return ResponseEntity.notFound().build();
        }

        gameEngine.addPlayer(roomId, username);

        return ResponseEntity.ok("JOINED");
    }

    // =====================================================
    // Salir
    // =====================================================
    @PostMapping("/leave")
    public ResponseEntity<?> leaveRoom(
            @RequestParam String roomId,
            @RequestParam String username
    ) {

        GameRoom room = gameEngine.getRoom(roomId);

        if (room == null) {
            return ResponseEntity.notFound().build();
        }

        // 🎥 si es streamer → borrar sala
        if (username.equals(room.getStreamer())) {

            gameEngine.removeRoom(roomId, username);

            return ResponseEntity.ok("ROOM CLOSED");
        }

        // 💬 si es viewer/chat → salir normal
        gameEngine.removePlayer(roomId, username);

        return ResponseEntity.ok("PLAYER LEFT");
    }

    @GetMapping("/room/{roomId}")
    public ResponseEntity<?> getRoom(@PathVariable String roomId) {

        GameRoom room = gameEngine.getRoom(roomId);

        if (room == null) {
            return ResponseEntity.status(404).body("ROOM NOT FOUND");
        }

        return ResponseEntity.ok(gameEngine.getGameState(roomId));
    }


    // =====================================================
    // 🧪 TEST WS
    // =====================================================
    @GetMapping("/test-ws")
    public String testWebSocket() {

        messagingTemplate.convertAndSend(
                "/topic/test",
                "Mensaje desde backend 🔥"
        );

        return "Enviado";
    }

    @PostMapping("/room/{roomId}/streamer-choice")
    public ResponseEntity<?> streamerChoice(
            @PathVariable String roomId,
            @RequestBody StreamerChoiceRequest req
    ) {
        gameEngine.setStreamerChoice(roomId, req.getOption());
        return ResponseEntity.ok().build();
    }
}
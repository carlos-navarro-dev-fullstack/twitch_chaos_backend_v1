package com.chatloco.twitch.application.scheduler;

import com.chatloco.twitch.application.engine.GameEngine;
import com.chatloco.twitch.domain.model.GameRoom;
import com.chatloco.twitch.domain.model.GameState;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RoundScheduler {

    private final GameEngine gameEngine;

    // =====================================================
    // ⏱️ GAME LOOP
    // =====================================================
    @Scheduled(fixedRate = 250)
    public void tickRounds() {

        for (GameRoom room : gameEngine.getRooms()) {

            synchronized (gameEngine.getLock(room.getRoomId())) {

                if (room.getState() != GameState.RESULT) {
                    continue;
                }

                long elapsed =
                        System.currentTimeMillis()
                                - room.getResultStartTime();

                if (elapsed < GameEngine.RESULT_TIME) {
                    continue;
                }

                // 🔥 FIN DEL JUEGO
                if (room.getRound() >= GameRoom.MAX_ROUNDS) {

                    room.setGameFinished(true);
                    room.setState(GameState.GAME_OVER);

                    continue;
                }

                gameEngine.startRound(room);
            }
        }
    }
}
package com.chatloco.twitch.application.dto;

import com.chatloco.twitch.domain.model.GameRoom;
import com.chatloco.twitch.domain.model.GameState;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GameStateResponse {

    private String roomId;

    private GameState state;

    private int round;

    private String situation;

    private List<String> options;

    private int reputation;

    private int funa;

    private int players;

    private String chatChoice;

    private String streamerChoice;

    private boolean gameFinished;

    private long roundTimeLeft;

    private Map<String, Integer> votePercentages;

    private Map<String, Integer> voteCounts;

    // =====================================================
    // MAPPER
    // =====================================================
    public static GameStateResponse from(GameRoom room) {

        long timeLeft = 0;

        if (room.getState() == GameState.VOTING) {

            long elapsed =
                    System.currentTimeMillis()
                            - room.getRoundStartTime();

            timeLeft = Math.max(0, 20 - (elapsed / 1000));
        }

        return GameStateResponse.builder()
                .roomId(room.getRoomId())
                .state(room.getState())
                .round(room.getRound())
                .situation(room.getCurrentSituation())
                .options(room.getOptions())
                .reputation(room.getReputation())
                .funa(room.getFuna())
                .players(room.getPlayers().size())
                .chatChoice(room.getChatChoice())
                .streamerChoice(room.getStreamerChoice())
                .gameFinished(room.isGameFinished())
                .votePercentages(room.getVotePercentages())
                .voteCounts(room.getVoteCounts())
                .roundTimeLeft(timeLeft)
                .build();
    }
}
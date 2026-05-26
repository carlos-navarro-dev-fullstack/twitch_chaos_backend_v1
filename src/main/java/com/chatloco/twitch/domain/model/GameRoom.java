package com.chatloco.twitch.domain.model;

import lombok.Data;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Data
public class GameRoom {

    public static final int MAX_ROUNDS = 8;

    private String roomId;

    private boolean resolving = false;


    private GameState state = GameState.WAITING_PLAYERS;

    private int round = 0;

    private String streamer;

    private boolean gameFinished = false;

    private Map<String, Player> players = new ConcurrentHashMap<>();

    private Map<String, OptionData> votes = new ConcurrentHashMap<>();
    private Map<String, Long> lastVoteTime = new ConcurrentHashMap<>();

    private String currentSituation;
    private List<OptionData> options = new ArrayList<>();

    private Map<String, Integer> votePercentages = new HashMap<>();
    private Map<String, Integer> voteCounts = new HashMap<>();

    private OptionData chatChoice;
    private OptionData streamerChoice;

    private int reputation = 100;
    private int funa = 0;

    private long roundStartTime;
    private long resultStartTime;

    private long lastActivityTime = System.currentTimeMillis();

    // =========================
    // GAME FINISH
    // =========================
    public boolean isGameFinished() {
        return gameFinished
                || round > MAX_ROUNDS
                || reputation <= 0
                || funa >= 100;
    }

    // =========================
    // ROUND CONTROL
    // =========================
    public void nextRound() {
        this.round++;
    }
}
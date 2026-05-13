package com.chatloco.twitch.domain.model;

import lombok.Data;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Data
public class GameRoom {

    public static final int MAX_ROUNDS = 8;

    private String roomId;

    private GameState state = GameState.WAITING_PLAYERS;

    private int round = 0;

    private String streamer;

    private boolean gameFinished = false;

    private Map<String, Player> players = new ConcurrentHashMap<>();

    private Map<String, String> votes = new ConcurrentHashMap<>();
    private Map<String, Long> lastVoteTime = new ConcurrentHashMap<>();

    private String currentSituation;
    private List<String> options = new ArrayList<>();

    private Map<String, Integer> votePercentages = new HashMap<>();

    private Map<String, Integer> voteCounts = new HashMap<>();

    private String chatChoice;
    private String streamerChoice;

    private int reputation = 100;
    private int funa = 0;

    private long roundStartTime;
    private long resultStartTime;

    private long lastActivityTime = System.currentTimeMillis();

    public boolean isGameFinished() {

        return gameFinished
                || round >= MAX_ROUNDS
                || reputation <= 0
                || funa >= 100;
    }

    public void nextRound() {
        round++;
    }
}
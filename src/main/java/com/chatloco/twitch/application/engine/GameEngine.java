package com.chatloco.twitch.application.engine;

import com.chatloco.twitch.application.dto.GameStateResponse;
import com.chatloco.twitch.domain.model.GameRoom;
import com.chatloco.twitch.domain.model.GameState;
import com.chatloco.twitch.domain.model.Player;
import com.chatloco.twitch.domain.model.SituationData;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class GameEngine {

    public static final long RESULT_TIME = 5_000;

    // =====================================================
    // 🏠 ROOMS
    // =====================================================
    private final Map<String, GameRoom> rooms = new ConcurrentHashMap<>();

    // =====================================================
    // 🔒 LOCKS
    // =====================================================
    private final Map<String, Object> locks = new ConcurrentHashMap<>();

    // =====================================================
    // 📡 WEBSOCKET
    // =====================================================
    private final SimpMessagingTemplate messagingTemplate;

    private final SituationEngine situationEngine;

    public GameEngine(
            SimpMessagingTemplate messagingTemplate,
            SituationEngine situationService
    ) {
        this.messagingTemplate = messagingTemplate;
        this.situationEngine = situationService;
    }

    // =====================================================
    // 📡 BROADCAST
    // =====================================================
    private void broadcast(GameRoom room) {

        if (room == null) return;

        GameStateResponse state = getGameState(room.getRoomId());

        if (state == null) {
            System.out.println("⚠️ NULL STATE");
            return;
        }

        messagingTemplate.convertAndSend(
                "/topic/rooms/" + room.getRoomId(),
                state
        );
    }

    // =====================================================
    // 🚀 CREATE ROOM
    // =====================================================
    public GameRoom createRoom(String streamerUsername) {

        String roomId;

        do {
            roomId = UUID.randomUUID().toString().substring(0, 6);
        } while (rooms.containsKey(roomId));

        GameRoom room = new GameRoom();
        room.setRoomId(roomId);

        room.setStreamer(streamerUsername);

        rooms.put(roomId, room);

        startRound(room);

        return room;
    }

    // =====================================================
    // 🔍 GET ROOM
    // =====================================================
    public GameRoom getRoom(String roomId) {
        return rooms.get(roomId);
    }

    // =====================================================
    // 👤 ADD PLAYER
    // =====================================================
    public void addPlayer(String roomId, String username) {

        if (username == null || username.isBlank()) return;

        GameRoom room = rooms.get(roomId);
        if (room == null) return;

        synchronized (getLock(roomId)) {

            room.getPlayers().putIfAbsent(
                    username,
                    new Player(username, 0)
            );

            room.setLastActivityTime(System.currentTimeMillis());

            broadcast(room);
        }
    }

    // =====================================================
    // 🎭 START ROUND
    // =====================================================
    public void startRound(GameRoom room) {

        synchronized (getLock(room.getRoomId())) {

            if (room.isGameFinished()) {
                room.setState(GameState.GAME_OVER);
                broadcast(room);
                return;
            }

            if (room.getRound() >= GameRoom.MAX_ROUNDS) {
                room.setGameFinished(true);
                room.setState(GameState.GAME_OVER);
                broadcast(room);
                return;
            }

            room.nextRound();
            room.setState(GameState.VOTING);

            SituationData data =
                    situationEngine.getRandom();

            room.setCurrentSituation(
                    data.getSituation()
            );

            room.setOptions(
                    data.getOptions()
            );

            room.setChatChoice(null);
            room.setStreamerChoice(null);

            room.getVotes().clear();
            room.getLastVoteTime().clear();

            room.getVotePercentages().clear();
            room.getVoteCounts().clear();

            room.setRoundStartTime(System.currentTimeMillis());
            room.setLastActivityTime(System.currentTimeMillis());

            broadcast(room);
        }
    }

    // =====================================================
    // 🗳️ REGISTER VOTE
    // =====================================================
    public void registerVote(String roomId, String username, String option) {

        GameRoom room = rooms.get(roomId);
        if (room == null) return;

        synchronized (getLock(roomId)) {

            if (room.getState() != GameState.VOTING) return;

            boolean isStreamer =
                    username.equalsIgnoreCase(room.getStreamer());

            if (!isStreamer &&
                    !room.getPlayers().containsKey(username)) {
                return;
            }

            if (!room.getOptions().contains(option)) return;

            long now = System.currentTimeMillis();
            Long last = room.getLastVoteTime().get(username);

            if (last != null && now - last < 800) return;

            room.getLastVoteTime().put(username, now);

            // registrar voto
            room.getVotes().put(username, option);

            updateVotePercentages(room);

            room.setLastActivityTime(now);

            // 🔥 streamer cierra ronda
            if (isStreamer) {

                room.setStreamerChoice(option);

                resolveRound(roomId);

                return;
            }

            broadcast(room);
        }
    }

    private void updateVotePercentages(GameRoom room) {

        Map<String, Integer> counts = new HashMap<>();

        for (String vote : room.getVotes().values()) {

            counts.put(
                    vote,
                    counts.getOrDefault(vote, 0) + 1
            );
        }

        int total = room.getVotes().size();

        Map<String, Integer> percentages =
                new HashMap<>();

        for (String option : room.getOptions()) {

            int count =
                    counts.getOrDefault(option, 0);

            int percent =
                    total == 0
                            ? 0
                            : (count * 100) / total;

            percentages.put(option, percent);
        }

        room.setVoteCounts(counts);
        room.setVotePercentages(percentages);
    }

    // =====================================================
    // ⚖️ RESOLVE ROUND
    // =====================================================
    public void resolveRound(String roomId) {

        GameRoom room = rooms.get(roomId);
        if (room == null) return;

        synchronized (getLock(roomId)) {

            if (room.getState() != GameState.VOTING) return;

            String chatWinner = calculateWinner(room);

            room.setChatChoice(chatWinner);

            applyResult(
                    room,
                    chatWinner,
                    room.getStreamerChoice()
            );

            room.setResultStartTime(System.currentTimeMillis());
            room.setState(GameState.RESULT);

            room.setLastActivityTime(System.currentTimeMillis());

            broadcast(room);
        }
    }

    // =====================================================
    // 📊 GAME STATE
    // =====================================================
    public GameStateResponse getGameState(String roomId) {

        GameRoom room = rooms.get(roomId);

        if (room == null) {

            return GameStateResponse.builder()
                    .roomId(roomId)
                    .state(GameState.WAITING_PLAYERS)
                    .round(0)
                    .options(List.of())
                    .reputation(0)
                    .funa(0)
                    .players(0)
                    .gameFinished(false)
                    .roundTimeLeft(0)
                    .build();
        }

        return GameStateResponse.from(room);
    }

    // =====================================================
    // 🧠 APPLY RESULT
    // =====================================================
    private void applyResult(GameRoom room, String chat, String streamer) {

        if (chat == null) return;

        if (chat.equals(streamer)) {
            room.setReputation(room.getReputation() + 5);
            room.setFuna(room.getFuna() - 5);
        } else {
            room.setReputation(room.getReputation() - 10);
            room.setFuna(room.getFuna() + 10);
        }

        room.setReputation(Math.max(0, Math.min(100, room.getReputation())));
        room.setFuna(Math.max(0, Math.min(100, room.getFuna())));
    }

    // =====================================================
    // 🗳️ WINNER
    // =====================================================
    private String calculateWinner(GameRoom room) {

        if (room.getVotes().isEmpty()) return null;

        Map<String, Integer> count = new HashMap<>();

        for (String vote : room.getVotes().values()) {
            count.put(vote, count.getOrDefault(vote, 0) + 1);
        }

        int max = Collections.max(count.values());

        List<String> winners = count.entrySet().stream()
                .filter(e -> e.getValue() == max)
                .map(Map.Entry::getKey)
                .toList();

        return winners.get(ThreadLocalRandom.current().nextInt(winners.size()));
    }

    // =====================================================
    // 🔒 LOCK
    // =====================================================
    private Object getLock(String roomId) {
        return locks.computeIfAbsent(roomId, k -> new Object());
    }

    // =====================================================
    // 📦 GET ALL ROOMS
    // =====================================================
    public Collection<GameRoom> getRooms() {
        return rooms.values();
    }

    // =====================================================
    // 📦 Borrar sala
    // =====================================================
    public void removeRoom(String roomId, String username) {

        GameRoom room = rooms.get(roomId);
        if (room == null) return;

        synchronized (getLock(roomId)) {

            // ❌ solo streamer puede borrar
            if (!username.equals(room.getStreamer())) {
                System.out.println("❌ No autorizado: solo streamer puede cerrar sala");
                return;
            }

            rooms.remove(roomId);
            locks.remove(roomId);

            System.out.println("🧹 ROOM ELIMINADA POR STREAMER: " + roomId);
        }
    }


    public void setStreamerChoice(String roomId, String option) {

        GameRoom room = rooms.get(roomId);
        if (room == null) return;

        synchronized (getLock(roomId)) {

            if (room.getState() != GameState.VOTING) return;

            if (!room.getOptions().contains(option)) return;

            // guardar elección streamer
            room.setStreamerChoice(option);

            // streamer también vota
            room.getVotes().put(room.getStreamer(), option);

            updateVotePercentages(room);

            // terminar ronda automáticamente
            resolveRound(roomId);
        }
    }

    public void removePlayer(String roomId, String username) {

        GameRoom room = rooms.get(roomId);
        if (room == null) return;

        synchronized (getLock(roomId)) {

            room.getPlayers().remove(username);

            broadcast(room);

            if (room.getPlayers().isEmpty()) {
                System.out.println("⚠️ Sala vacía");
            }
        }
    }
}
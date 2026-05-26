package com.chatloco.twitch.application.engine;

import com.chatloco.twitch.application.dto.GameStateResponse;
import com.chatloco.twitch.domain.model.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.*;

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

            if (room.isGameFinished()) return;

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

            System.out.println(room.getRound());

            if (room.isGameFinished()) {
                System.out.println("falleeeee");
                room.setState(GameState.GAME_OVER);
                broadcast(room);
                return;
            }

            int nextRound = room.getRound() + 1;

            room.setRound(nextRound);
            room.setState(GameState.VOTING);

            SituationData data = situationEngine.getRandom();

            room.setCurrentSituation(data.getSituation());
            room.setOptions(data.getOptions());

            room.setVotes(new ConcurrentHashMap<>());
            room.setLastVoteTime(new ConcurrentHashMap<>());
            room.setVoteCounts(new HashMap<>());
            room.setVotePercentages(new HashMap<>());

            room.setStreamerChoice(null);
            room.setChatChoice(null);

            broadcast(room);
        }
    }

    // =====================================================
    // 🗳️ REGISTER VOTE
    // =====================================================
    public void registerVote(String roomId, String username, String optionText) {

        GameRoom room = rooms.get(roomId);
        if (room == null || room.isGameFinished()) return;

        synchronized (getLock(roomId)) {

            if (room.getState() != GameState.VOTING) return;

            boolean isStreamer =
                    username.equalsIgnoreCase(room.getStreamer());

            if (!isStreamer &&
                    !room.getPlayers().containsKey(username)) {
                return;
            }

            // 🔥 buscar opción correctamente
            OptionData selected = room.getOptions().stream()
                    .filter(o -> o.getText().equals(optionText))
                    .findFirst()
                    .orElse(null);

            if (selected == null) return;

            long now = System.currentTimeMillis();
            Long last = room.getLastVoteTime().get(username);

            if (last != null && now - last < 800) return;

            room.getLastVoteTime().put(username, now);

            // 🗳️ guardar voto
            room.getVotes().put(username, selected);

            updateVotePercentages(room);

            room.setLastActivityTime(now);

            // 🔥 streamer cierra ronda
            if (isStreamer) {
                room.setStreamerChoice(selected);
                resolveRound(roomId);
                return;
            }

            broadcast(room);
        }
    }

    private void updateVotePercentages(GameRoom room) {

        Map<OptionData, Integer> counts = new HashMap<>();

        for (OptionData vote : room.getVotes().values()) {
            counts.put(vote, counts.getOrDefault(vote, 0) + 1);
        }

        int total = room.getVotes().size();

        Map<String, Integer> percentages = new HashMap<>();

        for (OptionData option : room.getOptions()) {

            int count = counts.getOrDefault(option, 0);

            int percent = total == 0 ? 0 : (count * 100) / total;

            percentages.put(option.getText(), percent);
        }

        room.setVoteCounts(
                counts.entrySet().stream()
                        .collect(java.util.stream.Collectors.toMap(
                                e -> e.getKey().getText(),
                                java.util.Map.Entry::getValue
                        ))
        );

        room.setVotePercentages(percentages);
    }


    // =====================================================
    // ⚖️ RESOLVE ROUND
    // =====================================================
    public void resolveRound(String roomId) {

        System.out.println("llegue a resolve round");

        GameRoom room = rooms.get(roomId);
        if (room == null) return;

        synchronized (getLock(roomId)) {

            if (room.isGameFinished()) return;

            // 🔒 SOLO ESTE CONTROL
            if (room.isResolving()) return;
            room.setResolving(true);

            try {

                System.out.println("llegue a resolve round2");

                if (!GameState.VOTING.equals(room.getState())) return;

                OptionData chatOption = calculateWinner(room);
                OptionData streamerOption = room.getStreamerChoice();

                if (chatOption == null) chatOption = room.getOptions().get(0);
                if (streamerOption == null) streamerOption = chatOption;

                room.setChatChoice(chatOption);

                applyResult(room, chatOption);
                applyResult(room, streamerOption);

                System.out.println("ssssss");
                checkGameOver(room);

                if (room.isGameFinished()) {
                    System.out.println("GAME OVER");

                    room.setState(GameState.GAME_OVER);
                    broadcast(room);
                    return;
                }

                room.setState(GameState.RESULT);
                room.setResultStartTime(System.currentTimeMillis());

                System.out.println(":3");

                broadcast(room);

            } finally {
                room.setResolving(false);
            }
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

    private void finishGame(GameRoom room, String reason) {

        room.setGameFinished(true);
        room.setState(GameState.GAME_OVER);

        room.setLastActivityTime(System.currentTimeMillis());

        System.out.println("🏁 GAME OVER: " + reason);

        broadcast(room);
    }

    private void checkGameOver(GameRoom room) {
        System.out.println("locas");
        if (room.getReputation() <= 0 || room.getFuna() >= 100) {
            System.out.println("animeeeee");
            room.setGameFinished(true);
            room.setState(GameState.GAME_OVER);
        }

        if (room.getRound() >= GameRoom.MAX_ROUNDS) {
            System.out.println("hola :3");
            room.setGameFinished(true);
            room.setState(GameState.GAME_OVER);
        }
    }

    // =====================================================
    // 🧠 APPLY RESULT
    // =====================================================
    private void applyResult(GameRoom room, OptionData option) {

        room.setFuna(room.getFuna() + option.getFunaImpact());
        room.setReputation(room.getReputation() + option.getRepImpact());

        room.setFuna(Math.max(0, Math.min(100, room.getFuna())));
        room.setReputation(Math.max(0, Math.min(100, room.getReputation())));
    }

    // =====================================================
    // 🗳️ WINNER
    // =====================================================
    private OptionData calculateWinner(GameRoom room) {

        if (room.getVotes().isEmpty()) return null;

        Map<OptionData, Integer> count = new HashMap<>();

        for (OptionData vote : room.getVotes().values()) {
            count.put(vote, count.getOrDefault(vote, 0) + 1);
        }

        int max = Collections.max(count.values());

        List<OptionData> winners = count.entrySet().stream()
                .filter(e -> e.getValue() == max)
                .map(Map.Entry::getKey)
                .toList();

        return winners.get(ThreadLocalRandom.current().nextInt(winners.size()));
    }

    // =====================================================
    // 🔒 LOCK
    // =====================================================
    public Object getLock(String roomId) {
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


    public void setStreamerChoice(String roomId, String optionText) {

        GameRoom room = rooms.get(roomId);
        if (room == null) return;

        synchronized (getLock(roomId)) {

            if (room.isGameFinished()) return;

            if (room.getState() != GameState.VOTING) return;

            if (room.getStreamerChoice() != null) return;

            OptionData selected = room.getOptions().stream()
                    .filter(o -> o.getText().equals(optionText))
                    .findFirst()
                    .orElse(null);

            if (selected == null) return;

            room.setStreamerChoice(selected);


            room.getVotes().put(room.getStreamer(), selected);
        }
    }

    public void removePlayer(String roomId, String username) {

        GameRoom room = rooms.get(roomId);
        if (room == null) return;

        synchronized (getLock(roomId)) {

            if (room.isGameFinished()) return;
            room.getPlayers().remove(username);

            broadcast(room);

            if (room.getPlayers().isEmpty()) {
                System.out.println("⚠️ Sala vacía");
            }
        }
    }
}
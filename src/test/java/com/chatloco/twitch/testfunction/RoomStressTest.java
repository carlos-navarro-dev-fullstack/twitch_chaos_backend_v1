package com.chatloco.twitch.testfunction;

import com.chatloco.twitch.application.dto.JoinMessage;
import com.chatloco.twitch.application.dto.VoteMessage;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.lang.reflect.Type;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.*;

public class RoomStressTest {

    private static final int VIEWERS = 1000;
    private static final int VOTE_INTERVAL_MS = 300;

    private static final BlockingQueue<Runnable> sendQueue =
            new LinkedBlockingQueue<>();

    private static volatile boolean gameOver = false;

    private static StompSession session;

    public static void main(String[] args) throws Exception {

        String roomId = "8f2d49";

        WebSocketStompClient client =
                new WebSocketStompClient(
                        new StandardWebSocketClient()
                );

        MappingJackson2MessageConverter converter =
                new MappingJackson2MessageConverter();

        converter.setStrictContentTypeMatch(false);

        client.setMessageConverter(converter);

        log("🚀 START STRESS TEST");

        // =====================================================
        // 🔥 SINGLE SENDER THREAD
        // =====================================================
        Thread senderThread = new Thread(() -> {

            while (!Thread.currentThread().isInterrupted()) {

                try {

                    if (gameOver) {
                        sendQueue.clear();
                        break;
                    }

                    Runnable task =
                            sendQueue.poll(
                                    100,
                                    TimeUnit.MILLISECONDS
                            );

                    if (task != null && !gameOver) {
                        task.run();
                    }

                } catch (Exception e) {

                    log("❌ SEND ERROR: " + e.getMessage());
                }
            }

            log("🛑 SENDER STOPPED");

        }, "WS-SENDER");

        senderThread.start();

        // =====================================================
        // 🔌 CONNECT
        // =====================================================
        session = client.connectAsync(
                "ws://localhost:8080/ws",
                new StompSessionHandlerAdapter() {}
        ).join();

        log("✅ CONNECTED");

        // =====================================================
        // 📡 SUBSCRIBE
        // =====================================================
        session.subscribe(
                "/topic/rooms/" + roomId,
                new StompFrameHandler() {

                    @Override
                    public Type getPayloadType(
                            StompHeaders headers
                    ) {
                        return Map.class;
                    }

                    @Override
                    public void handleFrame(
                            StompHeaders headers,
                            Object payload
                    ) {

                        if (!(payload instanceof Map<?, ?> map)) {
                            return;
                        }

                        Object state = map.get("state");

                        log(
                                "🎮 ROUND=" + map.get("round")
                                        + " | state=" + state
                                        + " | rep=" + map.get("reputation")
                                        + " | funa=" + map.get("funa")
                                        + " | players=" + map.get("players")
                        );

                        // 🔥 GAME OVER
                        if ("GAME_OVER".equals(String.valueOf(state))) {

                            log("🏁 GAME OVER");

                            gameOver = true;

                            sendQueue.clear();

                            try {

                                if (session.isConnected()) {
                                    session.disconnect();
                                }

                            } catch (Exception ignored) {}

                        }
                    }
                }
        );

        // =====================================================
        // 👑 STREAMER JOIN
        // =====================================================
        enqueue(() ->
                session.send(
                        "/app/join",
                        new JoinMessage(
                                roomId,
                                "streamer"
                        )
                )
        );

        // =====================================================
        // 👥 VIEWERS JOIN
        // =====================================================
        List<String> viewers = new ArrayList<>();

        for (int i = 0; i < VIEWERS; i++) {

            String user = "user_" + i;

            viewers.add(user);

            enqueue(() ->
                    session.send(
                            "/app/join",
                            new JoinMessage(
                                    roomId,
                                    user
                            )
                    )
            );
        }

        log("👥 " + VIEWERS + " viewers queued");

        // =====================================================
        // 🔥 VOTERS
        // =====================================================
        ExecutorService pool =
                Executors.newFixedThreadPool(100);

        for (String user : viewers) {

            pool.submit(() -> {

                Random random = new Random();

                while (
                        !gameOver
                                && !Thread.currentThread().isInterrupted()
                ) {

                    try {

                        Thread.sleep(
                                VOTE_INTERVAL_MS
                                        + random.nextInt(300)
                        );

                    } catch (InterruptedException e) {

                        return;
                    }

                    if (gameOver) {
                        break;
                    }

                    // 🔥 OPCIONES REALES
                    String[] options = {
                            "Pedir disculpas",
                            "Ignorar",
                            "Hacer meme",
                            "Responder serio",
                            "Bromear",
                            "Desaparecer",
                            "Hablar en stream",
                            "Twitlonger",
                            "No responder"
                    };

                    String option =
                            options[random.nextInt(options.length)];

                    enqueue(() -> {

                        if (
                                !gameOver
                                        && session.isConnected()
                        ) {

                            session.send(
                                    "/app/vote",
                                    new VoteMessage(
                                            roomId,
                                            user,
                                            option
                                    )
                            );
                        }
                    });
                }

                log("🛑 " + user + " stopped");
            });
        }

        // =====================================================
        // ⏱ TEST DURATION
        // =====================================================
        Thread.sleep(120000);

        // =====================================================
        // 🛑 SHUTDOWN
        // =====================================================
        gameOver = true;

        sendQueue.clear();

        try {

            if (session.isConnected()) {
                session.disconnect();
            }

        } catch (Exception ignored) {}

        pool.shutdownNow();

        senderThread.interrupt();

        log("🛑 TEST FINISHED");
    }

    // =====================================================
    // 📦 SAFE ENQUEUE
    // =====================================================
    private static void enqueue(Runnable action) {

        if (
                !gameOver
                        && session != null
                        && session.isConnected()
        ) {

            sendQueue.offer(action);
        }
    }

    // =====================================================
    // 🧰 UTILS
    // =====================================================
    private static void log(String msg) {

        System.out.println(
                "[" + LocalTime.now() + "] " + msg
        );
    }
}
package com.chatloco.twitch.testfunction;

import com.chatloco.twitch.application.dto.JoinMessage;
import com.chatloco.twitch.application.dto.VoteMessage;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.lang.reflect.Type;
import java.time.LocalTime;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

public class RoomTestClient {

    static StompSession sessionCarlos;
    static StompSession sessionAna;

    static AtomicBoolean gameOver = new AtomicBoolean(false);

    public static void main(String[] args) throws Exception {

        String roomId = "d8ec42";

        WebSocketStompClient client =
                new WebSocketStompClient(new StandardWebSocketClient());

        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        converter.setStrictContentTypeMatch(false);
        client.setMessageConverter(converter);

        log("🚀 START SIMULATION (CONTROLLED)");

        sessionCarlos = connect(client, "Carlos", roomId);
        sessionAna = connect(client, "Ana", roomId);

        // =====================================================
        // 🎮 STREAMER LOOP (CONTROLLED)
        // =====================================================
        new Thread(() -> {

            Random r = new Random();
            int rounds = 0;

            while (!gameOver.get() && rounds < 8) {

                sleep(4000);

                sessionCarlos.send("/app/end-round",
                        Map.of(
                                "roomId", roomId,
                                "streamerChoice", String.valueOf(r.nextInt(3))
                        ));

                log("🏁 END ROUND → " + rounds);

                rounds++;
            }

            log("🛑 STREAMER STOPPED");

        }).start();

        Thread.sleep(120000);
    }

    // =====================================================
    // CONNECT USER
    // =====================================================
    private static StompSession connect(WebSocketStompClient client, String username, String roomId) {

        return client.connectAsync(
                "ws://localhost:8080/ws",
                new StompSessionHandlerAdapter() {

                    @Override
                    public void afterConnected(StompSession session, StompHeaders headers) {

                        log("✅ CONNECTED: " + username);

                        session.subscribe("/topic/" + roomId, new StompFrameHandler() {

                            @Override
                            public Type getPayloadType(StompHeaders headers) {
                                return Map.class;
                            }

                            @Override
                            public void handleFrame(StompHeaders headers, Object payload) {

                                if (!(payload instanceof Map<?, ?> map)) return;

                                // 🚫 ignore joins
                                if ("JOIN".equals(map.get("type"))) return;

                                // 🏁 GAME OVER
                                if ("GAME_OVER".equals(map.get("type"))) {

                                    log("🏁 GAME OVER DETECTED");
                                    log("🏆 FINAL STATE: " + map.get("finalState"));

                                    gameOver.set(true);
                                    return;
                                }

                                if (map.containsKey("round")) {
                                    log("🎮 UPDATE");
                                }

                                log("📩 " + username + ": " + map);
                            }
                        });

                        // =====================================================
                        // JOIN
                        // =====================================================
                        session.send("/app/join", new JoinMessage(roomId, username));
                        log("👋 " + username + " joined");

                        // =====================================================
                        // VOTE LOOP (CONTROLLED)
                        // =====================================================
                        new Thread(() -> {

                            Random r = new Random();

                            while (!gameOver.get()) {

                                sleep(1500 + r.nextInt(1500));

                                session.send("/app/vote",
                                        new VoteMessage(roomId, username, String.valueOf(r.nextInt(3))));

                                log("🗳️ " + username + " voted");
                            }

                            log("🛑 " + username + " STOPPED VOTING");

                        }).start();
                    }

                    @Override
                    public void handleException(StompSession session,
                                                StompCommand command,
                                                StompHeaders headers,
                                                byte[] payload,
                                                Throwable exception) {
                        log("❌ ERROR: " + exception.getMessage());
                    }

                    @Override
                    public void handleTransportError(StompSession session, Throwable exception) {
                        log("🚨 TRANSPORT ERROR: " + exception.getMessage());
                    }
                }
        ).join();
    }

    // =====================================================
    // UTILS
    // =====================================================
    private static void log(String msg) {
        System.out.println("[" + LocalTime.now() + "] " + msg);
    }

    private static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (Exception ignored) {}
    }
}
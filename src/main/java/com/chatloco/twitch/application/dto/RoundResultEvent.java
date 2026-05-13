package com.chatloco.twitch.application.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class RoundResultEvent extends GameEvent {

    private String roomId;

    private String chatChoice;

    private String streamerChoice;

    private int reputation;

    private int funa;

    public RoundResultEvent(
            String roomId,
            String chatChoice,
            String streamerChoice,
            int reputation,
            int funa
    ) {

        super("ROUND_RESULT");

        this.roomId = roomId;
        this.chatChoice = chatChoice;
        this.streamerChoice = streamerChoice;
        this.reputation = reputation;
        this.funa = funa;
    }
}
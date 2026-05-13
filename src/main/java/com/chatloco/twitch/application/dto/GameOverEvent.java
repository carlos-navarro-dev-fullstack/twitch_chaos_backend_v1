package com.chatloco.twitch.application.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class GameOverEvent extends GameEvent {

    private String roomId;

    private int finalReputation;

    private int finalFuna;

    public GameOverEvent(
            String roomId,
            int finalReputation,
            int finalFuna
    ) {

        super("GAME_OVER");

        this.roomId = roomId;
        this.finalReputation = finalReputation;
        this.finalFuna = finalFuna;
    }
}
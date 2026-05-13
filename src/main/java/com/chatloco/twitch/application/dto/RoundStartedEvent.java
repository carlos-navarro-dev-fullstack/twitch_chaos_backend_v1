package com.chatloco.twitch.application.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class RoundStartedEvent extends GameEvent {

    private String roomId;

    private int round;

    private String situation;

    private List<String> options;

    public RoundStartedEvent(
            String roomId,
            int round,
            String situation,
            List<String> options
    ) {

        super("ROUND_STARTED");

        this.roomId = roomId;
        this.round = round;
        this.situation = situation;
        this.options = options;
    }
}
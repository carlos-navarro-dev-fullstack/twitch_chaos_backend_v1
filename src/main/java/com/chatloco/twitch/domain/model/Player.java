package com.chatloco.twitch.domain.model;

import lombok.Data;

@Data
public class Player {

    private String username;

    private int score;

    public Player(String username, int score) {
        this.username = username;
        this.score = score;
    }
}
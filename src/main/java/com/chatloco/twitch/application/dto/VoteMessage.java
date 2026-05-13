package com.chatloco.twitch.application.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VoteMessage {

    private String roomId;

    private String username;

    private String option;
}
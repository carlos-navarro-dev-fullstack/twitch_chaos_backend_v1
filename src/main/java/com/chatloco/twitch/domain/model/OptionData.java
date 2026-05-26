package com.chatloco.twitch.domain.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OptionData {
    private String text;
    private int funaImpact;
    private int repImpact;
}
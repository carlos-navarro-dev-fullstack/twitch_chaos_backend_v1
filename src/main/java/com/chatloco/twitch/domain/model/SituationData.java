package com.chatloco.twitch.domain.model;

import lombok.Data;

import java.util.List;

@Data
public class SituationData {

    private String situation;

    private List<OptionData> options;
}
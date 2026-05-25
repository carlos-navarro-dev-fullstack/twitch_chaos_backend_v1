package com.chatloco.twitch.application.engine;

import com.chatloco.twitch.domain.model.SituationData;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class SituationEngine {

    private final List<SituationData> situations;

    public SituationEngine() throws Exception {

        ObjectMapper mapper = new ObjectMapper();

        InputStream input =
                getClass().getResourceAsStream("/situations.json");

        situations = Arrays.asList(
                mapper.readValue(
                        input,
                        SituationData[].class
                )
        );
    }

    public SituationData getRandom() {

        return situations.get(
                ThreadLocalRandom.current()
                        .nextInt(situations.size())
        );
    }
}
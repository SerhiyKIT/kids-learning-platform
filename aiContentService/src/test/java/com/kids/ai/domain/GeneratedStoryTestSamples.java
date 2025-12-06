package com.kids.ai.domain;

import java.util.UUID;

public class GeneratedStoryTestSamples {

    public static GeneratedStory getGeneratedStorySample1() {
        return new GeneratedStory().id("id1").topic("topic1").audioUrl("audioUrl1");
    }

    public static GeneratedStory getGeneratedStorySample2() {
        return new GeneratedStory().id("id2").topic("topic2").audioUrl("audioUrl2");
    }

    public static GeneratedStory getGeneratedStoryRandomSampleGenerator() {
        return new GeneratedStory()
            .id(UUID.randomUUID().toString())
            .topic(UUID.randomUUID().toString())
            .audioUrl(UUID.randomUUID().toString());
    }
}

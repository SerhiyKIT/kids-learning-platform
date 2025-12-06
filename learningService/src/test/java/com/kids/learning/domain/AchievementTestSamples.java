package com.kids.learning.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

public class AchievementTestSamples {

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));

    public static Achievement getAchievementSample1() {
        return new Achievement().id(1L).title("title1").iconUrl("iconUrl1");
    }

    public static Achievement getAchievementSample2() {
        return new Achievement().id(2L).title("title2").iconUrl("iconUrl2");
    }

    public static Achievement getAchievementRandomSampleGenerator() {
        return new Achievement().id(longCount.incrementAndGet()).title(UUID.randomUUID().toString()).iconUrl(UUID.randomUUID().toString());
    }
}

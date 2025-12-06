package com.kids.platform.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class StudentTestSamples {

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));
    private static final AtomicInteger intCount = new AtomicInteger(random.nextInt() + (2 * Short.MAX_VALUE));

    public static Student getStudentSample1() {
        return new Student().id(1L).nickname("nickname1").age(1).avatarStyle("avatarStyle1");
    }

    public static Student getStudentSample2() {
        return new Student().id(2L).nickname("nickname2").age(2).avatarStyle("avatarStyle2");
    }

    public static Student getStudentRandomSampleGenerator() {
        return new Student()
            .id(longCount.incrementAndGet())
            .nickname(UUID.randomUUID().toString())
            .age(intCount.incrementAndGet())
            .avatarStyle(UUID.randomUUID().toString());
    }
}

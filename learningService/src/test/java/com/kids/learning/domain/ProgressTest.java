package com.kids.learning.domain;

import static com.kids.learning.domain.LessonTestSamples.*;
import static com.kids.learning.domain.ProgressTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.kids.learning.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class ProgressTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(Progress.class);
        Progress progress1 = getProgressSample1();
        Progress progress2 = new Progress();
        assertThat(progress1).isNotEqualTo(progress2);

        progress2.setId(progress1.getId());
        assertThat(progress1).isEqualTo(progress2);

        progress2 = getProgressSample2();
        assertThat(progress1).isNotEqualTo(progress2);
    }

    @Test
    void lessonTest() {
        Progress progress = getProgressRandomSampleGenerator();
        Lesson lessonBack = getLessonRandomSampleGenerator();

        progress.setLesson(lessonBack);
        assertThat(progress.getLesson()).isEqualTo(lessonBack);

        progress.lesson(null);
        assertThat(progress.getLesson()).isNull();
    }
}

package com.kids.learning.domain;

import static com.kids.learning.domain.LessonTestSamples.*;
import static com.kids.learning.domain.ProgressTestSamples.*;
import static com.kids.learning.domain.SubjectTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.kids.learning.web.rest.TestUtil;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class LessonTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(Lesson.class);
        Lesson lesson1 = getLessonSample1();
        Lesson lesson2 = new Lesson();
        assertThat(lesson1).isNotEqualTo(lesson2);

        lesson2.setId(lesson1.getId());
        assertThat(lesson1).isEqualTo(lesson2);

        lesson2 = getLessonSample2();
        assertThat(lesson1).isNotEqualTo(lesson2);
    }

    @Test
    void progressTest() {
        Lesson lesson = getLessonRandomSampleGenerator();
        Progress progressBack = getProgressRandomSampleGenerator();

        lesson.addProgress(progressBack);
        assertThat(lesson.getProgresses()).containsOnly(progressBack);
        assertThat(progressBack.getLesson()).isEqualTo(lesson);

        lesson.removeProgress(progressBack);
        assertThat(lesson.getProgresses()).doesNotContain(progressBack);
        assertThat(progressBack.getLesson()).isNull();

        lesson.progresses(new HashSet<>(Set.of(progressBack)));
        assertThat(lesson.getProgresses()).containsOnly(progressBack);
        assertThat(progressBack.getLesson()).isEqualTo(lesson);

        lesson.setProgresses(new HashSet<>());
        assertThat(lesson.getProgresses()).doesNotContain(progressBack);
        assertThat(progressBack.getLesson()).isNull();
    }

    @Test
    void subjectTest() {
        Lesson lesson = getLessonRandomSampleGenerator();
        Subject subjectBack = getSubjectRandomSampleGenerator();

        lesson.setSubject(subjectBack);
        assertThat(lesson.getSubject()).isEqualTo(subjectBack);

        lesson.subject(null);
        assertThat(lesson.getSubject()).isNull();
    }
}

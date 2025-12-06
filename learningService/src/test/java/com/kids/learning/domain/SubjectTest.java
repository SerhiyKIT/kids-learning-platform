package com.kids.learning.domain;

import static com.kids.learning.domain.LessonTestSamples.*;
import static com.kids.learning.domain.SubjectTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.kids.learning.web.rest.TestUtil;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class SubjectTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(Subject.class);
        Subject subject1 = getSubjectSample1();
        Subject subject2 = new Subject();
        assertThat(subject1).isNotEqualTo(subject2);

        subject2.setId(subject1.getId());
        assertThat(subject1).isEqualTo(subject2);

        subject2 = getSubjectSample2();
        assertThat(subject1).isNotEqualTo(subject2);
    }

    @Test
    void lessonTest() {
        Subject subject = getSubjectRandomSampleGenerator();
        Lesson lessonBack = getLessonRandomSampleGenerator();

        subject.addLesson(lessonBack);
        assertThat(subject.getLessons()).containsOnly(lessonBack);
        assertThat(lessonBack.getSubject()).isEqualTo(subject);

        subject.removeLesson(lessonBack);
        assertThat(subject.getLessons()).doesNotContain(lessonBack);
        assertThat(lessonBack.getSubject()).isNull();

        subject.lessons(new HashSet<>(Set.of(lessonBack)));
        assertThat(subject.getLessons()).containsOnly(lessonBack);
        assertThat(lessonBack.getSubject()).isEqualTo(subject);

        subject.setLessons(new HashSet<>());
        assertThat(subject.getLessons()).doesNotContain(lessonBack);
        assertThat(lessonBack.getSubject()).isNull();
    }
}

package com.kids.platform.domain;

import static com.kids.platform.domain.ParentTestSamples.*;
import static com.kids.platform.domain.StudentTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.kids.platform.web.rest.TestUtil;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ParentTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(Parent.class);
        Parent parent1 = getParentSample1();
        Parent parent2 = new Parent();
        assertThat(parent1).isNotEqualTo(parent2);

        parent2.setId(parent1.getId());
        assertThat(parent1).isEqualTo(parent2);

        parent2 = getParentSample2();
        assertThat(parent1).isNotEqualTo(parent2);
    }

    @Test
    void studentTest() {
        Parent parent = getParentRandomSampleGenerator();
        Student studentBack = getStudentRandomSampleGenerator();

        parent.addStudent(studentBack);
        assertThat(parent.getStudents()).containsOnly(studentBack);
        assertThat(studentBack.getParent()).isEqualTo(parent);

        parent.removeStudent(studentBack);
        assertThat(parent.getStudents()).doesNotContain(studentBack);
        assertThat(studentBack.getParent()).isNull();

        parent.students(new HashSet<>(Set.of(studentBack)));
        assertThat(parent.getStudents()).containsOnly(studentBack);
        assertThat(studentBack.getParent()).isEqualTo(parent);

        parent.setStudents(new HashSet<>());
        assertThat(parent.getStudents()).doesNotContain(studentBack);
        assertThat(studentBack.getParent()).isNull();
    }
}

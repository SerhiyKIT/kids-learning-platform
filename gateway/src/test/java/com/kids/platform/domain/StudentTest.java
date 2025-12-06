package com.kids.platform.domain;

import static com.kids.platform.domain.ParentTestSamples.*;
import static com.kids.platform.domain.StudentTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.kids.platform.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class StudentTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(Student.class);
        Student student1 = getStudentSample1();
        Student student2 = new Student();
        assertThat(student1).isNotEqualTo(student2);

        student2.setId(student1.getId());
        assertThat(student1).isEqualTo(student2);

        student2 = getStudentSample2();
        assertThat(student1).isNotEqualTo(student2);
    }

    @Test
    void parentTest() {
        Student student = getStudentRandomSampleGenerator();
        Parent parentBack = getParentRandomSampleGenerator();

        student.setParent(parentBack);
        assertThat(student.getParent()).isEqualTo(parentBack);

        student.parent(null);
        assertThat(student.getParent()).isNull();
    }
}

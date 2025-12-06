package com.kids.platform.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.kids.platform.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class ParentDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(ParentDTO.class);
        ParentDTO parentDTO1 = new ParentDTO();
        parentDTO1.setId(1L);
        ParentDTO parentDTO2 = new ParentDTO();
        assertThat(parentDTO1).isNotEqualTo(parentDTO2);
        parentDTO2.setId(parentDTO1.getId());
        assertThat(parentDTO1).isEqualTo(parentDTO2);
        parentDTO2.setId(2L);
        assertThat(parentDTO1).isNotEqualTo(parentDTO2);
        parentDTO1.setId(null);
        assertThat(parentDTO1).isNotEqualTo(parentDTO2);
    }
}

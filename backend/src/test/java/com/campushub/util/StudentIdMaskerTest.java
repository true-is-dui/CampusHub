package com.campushub.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StudentIdMaskerTest {

    @Test
    void mask_returnsNull_whenBlank() {
        assertThat(StudentIdMasker.mask("")).isNull();
        assertThat(StudentIdMasker.mask("  ")).isNull();
    }

    @Test
    void mask_shortStudentId_returnsAllStars() {
        assertThat(StudentIdMasker.mask("1234")).isEqualTo("****");
    }

    @Test
    void mask_normalStudentId_keepsEdgesAndMasksMiddle() {
        assertThat(StudentIdMasker.mask("20260001")).isEqualTo("20****01");
    }
}

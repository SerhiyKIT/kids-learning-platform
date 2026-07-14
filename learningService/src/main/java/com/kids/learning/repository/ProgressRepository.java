package com.kids.learning.repository;

import com.kids.learning.domain.Progress;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

@SuppressWarnings("unused")
@Repository
public interface ProgressRepository extends JpaRepository<Progress, Long> {
    Page<Progress> findByStudentId(Long studentId, Pageable pageable);
}

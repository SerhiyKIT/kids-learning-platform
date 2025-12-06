package com.kids.platform.repository;

import com.kids.platform.domain.Student;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Spring Data R2DBC repository for the Student entity.
 */
@SuppressWarnings("unused")
@Repository
public interface StudentRepository extends ReactiveCrudRepository<Student, Long>, StudentRepositoryInternal {
    Flux<Student> findAllBy(Pageable pageable);

    @Query("SELECT * FROM student entity WHERE entity.parent_id = :id")
    Flux<Student> findByParent(Long id);

    @Query("SELECT * FROM student entity WHERE entity.parent_id IS NULL")
    Flux<Student> findAllWhereParentIsNull();

    @Override
    <S extends Student> Mono<S> save(S entity);

    @Override
    Flux<Student> findAll();

    @Override
    Mono<Student> findById(Long id);

    @Override
    Mono<Void> deleteById(Long id);
}

interface StudentRepositoryInternal {
    <S extends Student> Mono<S> save(S entity);

    Flux<Student> findAllBy(Pageable pageable);

    Flux<Student> findAll();

    Mono<Student> findById(Long id);
    // this is not supported at the moment because of https://github.com/jhipster/generator-jhipster/issues/18269
    // Flux<Student> findAllBy(Pageable pageable, Criteria criteria);
}

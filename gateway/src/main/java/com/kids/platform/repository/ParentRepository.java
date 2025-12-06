package com.kids.platform.repository;

import com.kids.platform.domain.Parent;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Spring Data R2DBC repository for the Parent entity.
 */
@SuppressWarnings("unused")
@Repository
public interface ParentRepository extends ReactiveCrudRepository<Parent, Long>, ParentRepositoryInternal {
    Flux<Parent> findAllBy(Pageable pageable);

    @Override
    <S extends Parent> Mono<S> save(S entity);

    @Override
    Flux<Parent> findAll();

    @Override
    Mono<Parent> findById(Long id);

    @Override
    Mono<Void> deleteById(Long id);
}

interface ParentRepositoryInternal {
    <S extends Parent> Mono<S> save(S entity);

    Flux<Parent> findAllBy(Pageable pageable);

    Flux<Parent> findAll();

    Mono<Parent> findById(Long id);
    // this is not supported at the moment because of https://github.com/jhipster/generator-jhipster/issues/18269
    // Flux<Parent> findAllBy(Pageable pageable, Criteria criteria);
}

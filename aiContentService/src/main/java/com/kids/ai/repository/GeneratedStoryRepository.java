package com.kids.ai.repository;

import com.kids.ai.domain.GeneratedStory;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data MongoDB repository for the GeneratedStory entity.
 */
@Repository
public interface GeneratedStoryRepository extends MongoRepository<GeneratedStory, String> {}

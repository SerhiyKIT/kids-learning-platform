package com.kids.ai.repository;

import com.kids.ai.domain.AiPrompt;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data MongoDB repository for the AiPrompt entity.
 */
@Repository
public interface AiPromptRepository extends MongoRepository<AiPrompt, String> {}

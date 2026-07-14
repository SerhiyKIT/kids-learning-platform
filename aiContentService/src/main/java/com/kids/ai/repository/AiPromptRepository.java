package com.kids.ai.repository;

import com.kids.ai.domain.AiPrompt;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AiPromptRepository extends MongoRepository<AiPrompt, String> {
    Optional<AiPrompt> findByStyleName(String styleName);
}

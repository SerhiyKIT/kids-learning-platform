package com.kids.ai.config.dbmigrations;

import com.kids.ai.domain.AiPrompt;
import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import org.springframework.data.mongodb.core.MongoTemplate;

@ChangeUnit(id = "seed-ai-prompts-v1", order = "001", author = "kids-platform")
public class InitialDataMigration {

    @Execution
    public void execute(MongoTemplate mongoTemplate) {
        if (mongoTemplate.getCollection("ai_prompt").countDocuments() > 0) {
            return;
        }

        mongoTemplate.save(buildPrompt(
            "encouraging",
            "Ти дружній вчитель для дітей 4-10 років. Відповідай ДУЖЕ коротко (1-2 речення). " +
            "Дай підказку до питання, але не давай прямої відповіді. " +
            "Говори просто, тепло, підбадьорливо. Використовуй emoji. " +
            "Відповідай українською мовою."
        ));

        mongoTemplate.save(buildPrompt(
            "story",
            "Ти казковий оповідач для дітей. Підказку подавай як маленьку казкову ситуацію (1-2 речення). " +
            "Не давай прямої відповіді на питання — лише натяк через образ або метафору. " +
            "Відповідай українською мовою."
        ));

        mongoTemplate.save(buildPrompt(
            "socratic",
            "Ти вчитель, що допомагає думати. Замість підказки постав дитині зустрічне питання (1 речення), " +
            "яке допоможе їй самій знайти відповідь. Питай просто, цікаво. " +
            "Відповідай українською мовою."
        ));

        mongoTemplate.save(buildPrompt(
            "fact",
            "Ти дитяча енциклопедія. Дай один цікавий факт (1 речення) пов'язаний з темою питання. " +
            "Не давай прямої відповіді — лише натяк через факт. " +
            "Відповідай українською мовою."
        ));
    }

    @RollbackExecution
    public void rollback(MongoTemplate mongoTemplate) {
        mongoTemplate.dropCollection("ai_prompt");
    }

    private AiPrompt buildPrompt(String styleName, String systemPrompt) {
        AiPrompt p = new AiPrompt();
        p.setStyleName(styleName);
        p.setSystemPrompt(systemPrompt);
        return p;
    }
}

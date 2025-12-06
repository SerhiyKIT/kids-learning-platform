package com.kids.ai.domain;

import java.util.UUID;

public class AiPromptTestSamples {

    public static AiPrompt getAiPromptSample1() {
        return new AiPrompt().id("id1").styleName("styleName1");
    }

    public static AiPrompt getAiPromptSample2() {
        return new AiPrompt().id("id2").styleName("styleName2");
    }

    public static AiPrompt getAiPromptRandomSampleGenerator() {
        return new AiPrompt().id(UUID.randomUUID().toString()).styleName(UUID.randomUUID().toString());
    }
}

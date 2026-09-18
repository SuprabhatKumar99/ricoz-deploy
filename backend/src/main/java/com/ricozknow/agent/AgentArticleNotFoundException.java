package com.ricozknow.agent;

import java.util.UUID;

public class AgentArticleNotFoundException extends RuntimeException {
    public AgentArticleNotFoundException(UUID id) {
        super("Article not found, not published, or not agent-visible: " + id);
    }
}

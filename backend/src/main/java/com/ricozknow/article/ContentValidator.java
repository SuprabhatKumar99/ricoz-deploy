package com.ricozknow.article;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Validates structured article content.
 *
 * Article content is stored as JSONB and must use the supported
 * block structure. Raw/executable HTML is not accepted.
 *
 * MVP block types:
 * heading, paragraph, image, list, code.
 */
@Component
@RequiredArgsConstructor
public class ContentValidator {

    private static final Set<String> ALLOWED_BLOCK_TYPES = Set.of(
            "heading",
            "paragraph",
            "image",
            "list",
            "code"
    );

    public void validate(JsonNode content) {

        if (content == null || content.isNull()) {
            throw new InvalidArticleContentException(
                    "Content is required"
            );
        }

        if (!content.isObject()) {
            throw new InvalidArticleContentException(
                    "Content must be a JSON object"
            );
        }

        JsonNode blocks = content.get("blocks");

        if (blocks == null || !blocks.isArray() || blocks.isEmpty()) {
            throw new InvalidArticleContentException(
                    "Content must contain a non-empty 'blocks' array"
            );
        }

        for (JsonNode block : blocks) {

            if (!block.isObject()) {
                throw new InvalidArticleContentException(
                        "Each content block must be a JSON object"
                );
            }

            JsonNode typeNode = block.get("type");

            if (typeNode == null || !typeNode.isTextual()) {
                throw new InvalidArticleContentException(
                        "Every content block must have a 'type'"
                );
            }

            String type = typeNode.asText();

            if (!ALLOWED_BLOCK_TYPES.contains(type)) {
                throw new InvalidArticleContentException(
                        "Unsupported block type: " + type
                );
            }

            if ("heading".equals(type)) {
                JsonNode level = block.get("level");

                if (level == null
                        || !level.canConvertToInt()
                        || level.asInt() < 1
                        || level.asInt() > 4) {

                    throw new InvalidArticleContentException(
                            "Heading blocks require level 1-4"
                    );
                }
            }
        }
    }
}
package com.ricozknow.article;

import static com.ricozknow.TestJson.parse;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ContentValidatorTest {

    private final ContentValidator validator = new ContentValidator();

    @Test
    void acceptsAllAllowlistedBlockTypes() {
        String content = """
                {"blocks":[
                    {"type":"heading","level":2,"text":"Title"},
                    {"type":"paragraph","text":"Body"},
                    {"type":"image","assetId":"abc-123"},
                    {"type":"list","items":["a","b"]},
                    {"type":"code","text":"echo hi"}
                ]}""";
        assertThatCode(() -> validator.validate(parse(content))).doesNotThrowAnyException();
    }

    @Test
    void rejectsNonAllowlistedBlockType() {
        String content = "{\"blocks\":[{\"type\":\"script\",\"text\":\"alert(1)\"}]}";
        assertThatThrownBy(() -> validator.validate(parse(content)))
                .isInstanceOf(InvalidArticleContentException.class)
                .hasMessageContaining("script");
    }

    @Test
    void rejectsRawHtmlSmuggledAsText() {
        // The allowlist has no "raw_html" type at all — even if someone tries to
        // pass HTML as the text of a paragraph block, it stays inert text content
        // (the renderer never interprets it), but a block *type* outside the
        // allowlist must still be rejected outright.
        String content = "{\"blocks\":[{\"type\":\"raw_html\",\"text\":\"<script>alert(1)</script>\"}]}";
        assertThatThrownBy(() -> validator.validate(parse(content)))
                .isInstanceOf(InvalidArticleContentException.class);
    }

    @Test
    void rejectsHeadingWithoutValidLevel() {
        String content = "{\"blocks\":[{\"type\":\"heading\",\"text\":\"no level\"}]}";
        assertThatThrownBy(() -> validator.validate(parse(content)))
                .isInstanceOf(InvalidArticleContentException.class)
                .hasMessageContaining("level");
    }

    @Test
    void rejectsMissingBlocksArray() {
        assertThatThrownBy(() -> validator.validate(parse("{}")))
                .isInstanceOf(InvalidArticleContentException.class);
    }

    @Test
    void malformedJsonCannotReachTheValidator() {
        assertThatThrownBy(() -> parse("not json at all"))
                .isInstanceOf(AssertionError.class);
    }
}

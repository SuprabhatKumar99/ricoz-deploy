package com.ricozknow.article;

public class InvalidArticleStateException extends RuntimeException {
    public InvalidArticleStateException(String message) {
        super(message);
    }
}

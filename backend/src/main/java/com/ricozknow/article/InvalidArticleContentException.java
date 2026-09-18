package com.ricozknow.article;

public class InvalidArticleContentException extends RuntimeException {
    public InvalidArticleContentException(String message) {
        super(message);
    }
}

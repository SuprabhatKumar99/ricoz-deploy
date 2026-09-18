package com.ricozknow.article;

/** Who can see a published article. Enforced server-side in the search/read path. */
public enum Visibility {
    PUBLIC,
    AUTHENTICATED,
    PRIVATE
}

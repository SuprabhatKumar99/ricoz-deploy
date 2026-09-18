package com.ricozknow.search;

import java.util.List;

/**
 * Mirrors the document shape from spec section 13 exactly. Field names here
 * are what get indexed into OpenSearch, so keep them stable — a rename here
 * is a reindex, not just a code change.
 */
public class ArticleSearchDocument {

    public String tenantId;
    public String articleId;
    public String versionId;
    public String slug;
    public String title;
    public List<String> headings;
    public String body;
    public List<String> keywords;
    public String categoryId;
    public String categoryName;
    public String visibility;
    public String publishedAt;

    public ArticleSearchDocument() {
    }

    public ArticleSearchDocument(String tenantId, String articleId, String versionId, String slug, String title,
                                  List<String> headings, String body, List<String> keywords,
                                  String categoryId, String categoryName, String visibility, String publishedAt) {
        this.tenantId = tenantId;
        this.articleId = articleId;
        this.versionId = versionId;
        this.slug = slug;
        this.title = title;
        this.headings = headings;
        this.body = body;
        this.keywords = keywords;
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.visibility = visibility;
        this.publishedAt = publishedAt;
    }
}

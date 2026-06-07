package com.rmd.rag;

public class KnowledgeDocument {

    private final String id;
    private final String title;
    private final String category;
    private final String content;

    public KnowledgeDocument(String id, String title, String category, String content) {
        this.id = id;
        this.title = title;
        this.category = category;
        this.content = content;
    }

    public String getId()       { return id; }
    public String getTitle()    { return title; }
    public String getCategory() { return category; }
    public String getContent()  { return content; }
}

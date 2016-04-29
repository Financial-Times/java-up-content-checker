package com.ft.up.contentchecker;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Content {
    public String id;
    public String type;
    public String bodyXML;
    public String title;
    //      Byline           string       `json:byline`
    //      PublishedDate    string       `json:publishedDate`
    public List<Identifier> identifiers;
    //      RequestUrl       string       `json:requestUrl`
    //      Brands           []string     `json:brands`
    public MainImage mainImage;
    public String binaryUrl;
    public List<Member> members;
    //      Comments         Comments     `json:comments`
    public String publishReference;
    public String lastModified;
}

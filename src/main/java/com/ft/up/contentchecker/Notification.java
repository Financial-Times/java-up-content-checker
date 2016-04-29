package com.ft.up.contentchecker;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Notification {
  public String type;
  public String id;
  public String apiUrl;
  public String lastModified;
}

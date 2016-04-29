package com.ft.up.contentchecker;

import java.util.List;
import java.util.UUID;

public interface Checker {
  public List<List<String>> check(UUID uuid);
}

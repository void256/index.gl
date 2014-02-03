package com.lessvoid.indexgl;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.Application;

public class IndexGLApplication extends Application {
  @Override
  public Set<Object> getSingletons() {
    Set<Object> s = new HashSet<Object>();
    try {
      s.add(new IndexGL());
    } catch (Exception e) {
      e.printStackTrace();
    }
    return s;
  }
}
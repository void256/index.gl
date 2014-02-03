package com.lessvoid.indexgl.registry;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.apache.lucene.util.Version;
import org.junit.Test;

import com.lessvoid.indexgl.registry.AnalyzerUtils;
import com.lessvoid.indexgl.registry.GLAnalyzer;

public class GLAnalyzerTest {

  @Test
  public void testGL_COLOR() throws Exception {
    String input = "GL_COLOR";
    System.out.println("[" + input + "]");
    for (String s : AnalyzerUtils.tokensFromAnalysis(new GLAnalyzer(Version.LUCENE_44), input)) {
      System.out.println(s);
    }
  }

  @Test
  public void testglColor3f() throws Exception {
    String input = "glColor3f";
    System.out.println("[" + input + "]");
    for (String s : AnalyzerUtils.tokensFromAnalysis(new GLAnalyzer(Version.LUCENE_44), input)) {
      System.out.println(s);
    }
  }

  @Test
  public void testGLClearBufferiv() throws Exception {
    String input = "glClearBufferiv";
    System.out.println("[" + input + "]");
    for (String s : AnalyzerUtils.tokensFromAnalysis(new GLAnalyzer(Version.LUCENE_44), input)) {
      System.out.println(s);
    }
  }

  private void execTest(final String input, final String ... expected) throws Exception {
    assertList(AnalyzerUtils.tokensFromAnalysis(new GLAnalyzer(Version.LUCENE_44), input), expected);
  }

  private void assertList(final List<String> actual, final String ... expected) {
    for (int i=0; i<actual.size(); i++) {
      System.out.println(i + ": " + actual.get(i));
    }

    assertEquals(actual.size(), expected.length);

    for (int i=0; i<expected.length; i++) {
      assertEquals(actual.get(i), expected[i]);
    }
  }
}

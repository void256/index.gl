package com.lessvoid.indexgl.registry;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;

import com.lessvoid.indexgl.registry.Registry.GLResult;
import com.lessvoid.indexgl.registry.Registry.GLResult.ApiWithSupportedVersions;

public class RegistryTest {
  private static Registry registry;

  @BeforeClass
  public static void before() throws Exception {
    registry = new Registry();
  }
  
  @Test
  public void testTypeaheadBegin() throws Exception {
    execTypeaheadTest(
        "begin",
        "glBegin", "glBeginQuery", "glBeginTransformFeedback", "glBeginConditionalRender", "glBeginQueryIndexed");
  }

  @Test
  public void testTypeaheadGLColor() throws Exception {
    execTypeaheadTest(
        "glColor",
        "glColor3b", "glColor3bv", "glColor3d", "glColor3dv", "glColor3f", "glColor3fv", "glColor3i", "glColor3iv", "glColor3s", "glColor3sv");
  }

  @Test
  public void testTypeaheadGLColor3f() throws Exception {
    execTypeaheadTest(
        "glColor3f",
        "glColor3f", "glColor3fv");
  }

  @Test
  public void testTypeaheadGL_2D() throws Exception {
    execTypeaheadTest(
        "GL_2D",
        "GL_2D");
  }

  @Test
  public void testGLNewList() throws Exception {
    GLResult result = registry.getGLInfo("glNewList");

    assertEquals("glNewList", result.getName());
    assertVersions(result.getVersions(), "1.0", "1.1", "1.2", "1.3", "1.4", "1.5", "2.0", "2.1", "3.0", "3.1", "3.2", "3.3", "4.0", "4.1", "4.2", "4.3", "4.4");
    assertEquals(3, result.getList().size());
    assertSupported(result.getList().get(0), "OpenGL",              "Y", "Y", "Y", "Y", "Y", "Y", "Y", "Y", "Y", "Y", "Y", "Y", "Y", "Y", "Y", "Y", "Y");
    assertSupported(result.getList().get(1), "OpenGL Core Profile", "-", "-", "-", "-", "-", "-", "-", "-", "-", "-", "N", "N", "N", "N", "N", "N", "N");
    assertSupported(result.getList().get(2), "OpenGL ES",           "-", "-", "-", "-", "-", "-", "-", "-", "-", "-", "-", "-", "-", "-", "-", "-", "-");
  }

  @Test
  public void testGL_VERTEX_ARRAY() throws Exception {
    GLResult result = registry.getGLInfo("GL_VERTEX_ARRAY");

    assertEquals("GL_VERTEX_ARRAY", result.getName());
    assertVersions(result.getVersions(), "1.0", "1.1", "1.2", "1.3", "1.4", "1.5", "2.0", "2.1", "3.0", "3.1", "3.2", "3.3", "4.0", "4.1", "4.2", "4.3", "4.4");
    assertEquals(3, result.getList().size());
    assertSupported(result.getList().get(0), "OpenGL",              "N", "Y", "Y", "Y", "Y", "Y", "Y", "Y", "Y", "Y", "Y", "Y", "Y", "Y", "Y", "Y", "Y");
    assertSupported(result.getList().get(1), "OpenGL Core Profile", "-", "-", "-", "-", "-", "-", "-", "-", "-", "-", "N", "N", "N", "N", "N", "Y", "Y");
    assertSupported(result.getList().get(2), "OpenGL ES",           "Y", "-", "-", "-", "-", "-", "Y", "-", "Y", "-", "-", "-", "-", "-", "-", "-", "-");
  }

  @Test
  public void testNoDirectHit() throws Exception {
    GLResult result = registry.getGLInfo("e");
    assertNull(result);
  }

  @Test
  public void testDirectHit() throws Exception {
    GLResult result = registry.getGLInfo("GL_2D");

    assertEquals("GL_2D", result.getName());
    assertVersions(result.getVersions(), "1.0","1.1","1.2","1.3","1.4","1.5","2.0","2.1","3.0","3.1","3.2","3.3","4.0","4.1","4.2","4.3","4.4");
    assertEquals(3, result.getList().size());
    assertSupported(result.getList().get(0), "OpenGL",              "N", "Y", "Y", "Y", "Y", "Y", "Y", "Y", "Y", "Y", "Y", "Y", "Y", "Y", "Y", "Y", "Y");
    assertSupported(result.getList().get(1), "OpenGL Core Profile", "-", "-", "-", "-", "-", "-", "-", "-", "-", "-", "N", "N", "N", "N", "N", "N", "N");
    assertSupported(result.getList().get(2), "OpenGL ES",           "-", "-", "-", "-", "-", "-", "-", "-", "-", "-", "-", "-", "-", "-", "-", "-", "-");
  }

  private void assertResult(final String[] result, final String ... expected) {
    assertArrayEquals(expected, result);
  }

  private void execTypeaheadTest(final String search, final String ... expected) throws Exception {
    assertResult(registry.typeahead(search).getResult(), expected);
  }

  private void assertVersions(final List<String> actual, final String ... expected) {
    assertArrayEquals(actual.toArray(), expected);
  }

  private void assertSupported(final ApiWithSupportedVersions api, final String name, final String ... supported) {
    assertEquals(api.getApi(), name);
    assertArrayEquals(api.getSupported().toArray(), supported);
  }
}

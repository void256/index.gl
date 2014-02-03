package com.lessvoid.indexgl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URI;
import java.text.MessageFormat;
import java.util.ResourceBundle;

import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.lang3.StringEscapeUtils;

import com.google.gson.Gson;
import com.lessvoid.indexgl.registry.Registry;
import com.lessvoid.indexgl.registry.Registry.GLResult;

@Path("")
@Singleton
public class IndexGL {
  private final Gson gson = new Gson();
  private final Registry registry;

  @Context
  private UriInfo uriInfo;

  @Context
  private HttpServletRequest request;

  public IndexGL() throws Exception {
    long now = System.nanoTime();
    registry = new Registry();
    System.out.println("Registry parsed in: " + (System.nanoTime() - now)/1000000000f + "sec");
  }

  @GET
  @Produces(MediaType.TEXT_HTML)
  public Response getMyResources() throws Exception {
    StringBuilder result = new StringBuilder();

    addMethodLink(result, IndexGL.class.getMethod("getFind", String.class), "getFind", "q", "{search}");
    addMethodLink(result, IndexGL.class.getMethod("getJson", String.class), "getJson");
    addMethodLink(result, IndexGL.class.getMethod("getHtml", String.class), "getHtml");

    return Response.ok(getAPIContent(result.toString()), MediaType.TEXT_HTML).build();
  }

  @GET
  @Path("find")
  @Produces(MediaType.APPLICATION_JSON)
  public String getFind(@QueryParam(value="q") final String q) throws Exception {
    return gson.toJsonTree(registry.typeahead(q)).toString();
  }

  @GET
  @Path("gl/{identifier}")
  @Produces(MediaType.APPLICATION_JSON)
  public String getJson(@PathParam("identifier") final String identifier) throws Exception {
    return gson.toJson(registry.getGLInfo(identifier));
  }

  @GET
  @Path("gl-table/{identifier}")
  @Produces(MediaType.TEXT_HTML)
  public String getHtml(@PathParam("identifier") final String identifier) throws Exception {
    return translateToStatic(registry.getGLInfo(identifier));
  }

  private void addMethodLink(final StringBuilder result, final Method method, final String message, final String queryName, final String queryValue) {
    UriBuilder ub = uriInfo.getAbsolutePathBuilder();
    addMethodLink(result, message, ub.path(method).queryParam(queryName, queryValue).build());
  }

  private void addMethodLink(final StringBuilder result, final Method method, final String message) {
    UriBuilder ub = uriInfo.getAbsolutePathBuilder();
    addMethodLink(result, message, ub.path(method).build());
  }

  private void addMethodLink(final StringBuilder result, final String message, final URI url) {
    ResourceBundle res = ResourceBundle.getBundle("messages", request.getLocale());

    result.append("<div class=\"panel panel-info\">");
    result.append("<div class=\"panel-heading\">");
    result.append(MessageFormat.format(res.getString(message + ".caption"), makeLink(url)));
    result.append("</div>");
    result.append("<div class=\"panel-body\">");
    result.append("<p>");
    result.append(res.getString(message + ".description"));
    result.append("</p>");
    result.append("<p>");
    result.append(res.getString("example"));
    result.append("</p>");
    result.append("<code>");
    result.append("GET ");
    result.append("<a href=\"");
    result.append(url.toString()
        .replaceAll("%7Bidentifier%7D", res.getString(message + ".name"))
        .replaceAll("%7Bsearch%7D", res.getString(message + ".name"))
        );
    result.append("\">");
    result.append(url.toString()
        .replaceAll("%7Bidentifier%7D", res.getString(message + ".name"))
        .replaceAll("%7Bsearch%7D", res.getString(message + ".name"))
        );
    result.append("</a>");
    result.append("</code>");
    result.append("<p>");
    result.append(res.getString("result"));
    result.append("</p>");
    result.append("<code>");
    result.append(StringEscapeUtils.escapeHtml4(res.getString(message + ".result")));
    result.append("</code>");
    result.append("</div>");
    result.append("</div>");
  }

  private String makeLink(final URI url) {
    StringBuilder result = new StringBuilder();
    result.append(url.getPath());
    if (url.getQuery() != null) {
      result.append("?");
      result.append(url.getQuery());
    }
    return result.toString();
  }

  private String getAPIContent(final String methods) throws IOException {
    String result = new String(read(IndexGL.class.getResourceAsStream("/_api.html")));
    return result.replaceAll("#####", methods);
  }

  private byte[] read(final InputStream inputStream) throws IOException {
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    byte[] buffer = new byte[1024];
    int length = 0;
    while ((length = inputStream.read(buffer)) != -1) {
      output.write(buffer, 0, length);
    }
    return output.toByteArray();
  }

  private String translateToStatic(final GLResult glInfo) {
    if (glInfo == null) {
      return "not found";
    }
    return glInfo.generateTable();
  }
}

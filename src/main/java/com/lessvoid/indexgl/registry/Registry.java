package com.lessvoid.indexgl.registry;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.Fields;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.index.SlowCompositeReaderWrapper;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.sandbox.queries.DuplicateFilter;
import org.apache.lucene.sandbox.queries.DuplicateFilter.KeepMode;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.FilteredQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.Version;

/**
 * The Registry holds the Lucene RAMDirectoy and provides methods to search it.
 * 
 * @author void
 */
public class Registry {
  /**
   * Default analyzer.
   */
  private static final Analyzer ANALYZER = new GLAnalyzer(Version.LUCENE_44);

  /**
   * Number of hits per page.
   */
  private static final int HITS_PER_PAGE = 10;

  /**
   * The lucene directory.
   */
  private final Directory index = new RAMDirectory();

  /**
   * The lucene directory reader.
   */
  private final IndexReader reader;

  /**
   * The lucene index searcher.
   */
  private final IndexSearcher searcher;

  /**
   * The parser.
   */
  private final RegistryParser parser;

  /**
   * GL versions.
   */
  private final Set<String> glVersions = new TreeSet<String>();
  private final Set<String> glCoreVersions = new TreeSet<String>();
  private final Set<String> glESVersions = new TreeSet<String>();

  /**
   * Registry constructor. This will parse the registry.
   */
  public Registry() throws Exception {
    IndexWriter indexWriter = createIndexWriter();
    parser = new RegistryParser();
    parser.process(Registry.class.getResourceAsStream("/gl.xml"), indexWriter);
    indexWriter.close();

    reader = SlowCompositeReaderWrapper.wrap(DirectoryReader.open(index));
    searcher = new IndexSearcher(reader);

    DirectoryReader reader = DirectoryReader.open(index);
    Fields fields = MultiFields.getFields(reader);
    if (fields != null) {
      Terms terms = fields.terms("api-number");
      if (terms != null) {
        TermsEnum termsEnum = terms.iterator(null);
        BytesRef text;
        while ((text = termsEnum.next()) != null) {
          String apiNumber = text.utf8ToString();
          if (apiNumber.startsWith("gl:")) {
            String api = apiNumber.substring(3, apiNumber.length());
            glVersions.add(api);
            if (api.startsWith("3.3") || api.startsWith("3.2") || api.startsWith("4.")) {
              glCoreVersions.add(api);
            }
            if (api.equals("1.0") || api.equals("2.0") || api.equals("3.0")) {
              glESVersions.add(api);
            }
          }
        }
      }
    }
  }

  public TypeaheadResult typeahead(final String q) throws IOException {
    long start = System.nanoTime();

    TopScoreDocCollector collector = TopScoreDocCollector.create(HITS_PER_PAGE, true);
    searcher.search(new FilteredQuery(buildQuery(q), createDuplicateFilter()), collector);

    List<String> result = new ArrayList<String>();
    ScoreDoc[] docs = collector.topDocs().scoreDocs;
    for (int i = 0; i < docs.length; i++) {
      Document firstDoc = searcher.doc(docs[i].doc);
      result.add(firstDoc.get("name"));
    }

    return new TypeaheadResult(
        result.toArray(new String[0]),
        System.nanoTime() - start,
        result.size(),
        collector.getTotalHits());
  }

  private DuplicateFilter createDuplicateFilter() {
    DuplicateFilter df = new DuplicateFilter("name");
    df.setKeepMode(KeepMode.KM_USE_FIRST_OCCURRENCE);
    return df;
  }

  public GLResult getGLInfo(final String gl) throws Exception {
    Map<String, VersionInfo> versionInfos = new Hashtable<String, VersionInfo>();
    for (String version : glVersions) {
      versionInfos.put(version, new VersionInfo());
    }

    // nothing found?
    ScoreDoc[] required = findRequired(gl, searcher);
    if (required.length == 0) {
      return null; // FIXME Nothing found ... return better GLResult for that
                   // case
    }

    // the name is the same for all docs so we can extract it from the first
    // entry
    String glName = searcher.doc(required[0].doc).get("name");

    // now process them
    for (int i = 0; i < required.length; i++) {
      Document doc = searcher.doc(required[i].doc);
      if ("gl".equals(doc.get("api"))) {
        boolean supported = false;
        for (String version : glVersions) {
          if (!supported) {
            supported = version.equals(doc.get("number"));
          }
          VersionInfo versionInfo = versionInfos.get(version);
          if (versionInfo.compatibilityProfile == null) {
            versionInfo.compatibilityProfile = 0;
          }
          if (supported) {
            versionInfo.compatibilityProfile++;
          }
          if (glCoreVersions.contains(version)) {
            if (versionInfo.coreProfile == null) {
              versionInfo.coreProfile = 0;
            }
            if (supported) {
              versionInfo.coreProfile++;
            }
          }
        }
      } else if (doc.get("api").startsWith("gles")) {
        boolean supported = false;
        for (String version : glESVersions) {
          if (!supported) {
            supported = version.equals(doc.get("number"));
          }
          VersionInfo versionInfo = versionInfos.get(version);
          if (versionInfo.esProfile == null) {
            versionInfo.esProfile = 0;
          }
          if (supported) {
            versionInfo.esProfile++;
          }
        }
      }
    }

    // now correct what we've got so far for the removed features
    ScoreDoc[] removed = findRemoved(gl, searcher);
    for (int i = 0; i < removed.length; i++) {
      Document doc = searcher.doc(removed[i].doc);
      if ("gl".equals(doc.get("api"))) {
        boolean removedVersion = false;
        for (String version : glVersions) {
          if (!removedVersion) {
            removedVersion = version.equals(doc.get("number"));
          }
          if (removedVersion) {
            VersionInfo versionInfo = versionInfos.get(version);
            if (versionInfo.coreProfile != null) {
              versionInfo.coreProfile--;
            }
          }
        }
      } else if (doc.get("api").startsWith("gles")) {
        boolean removedVersion = false;
        for (String version : glESVersions) {
          if (!removedVersion) {
            removedVersion = version.equals(doc.get("number"));
          }
          if (removedVersion) {
            VersionInfo versionInfo = versionInfos.get(version);
            if (versionInfo.esProfile != null) {
              versionInfo.esProfile--;
            }
          }
        }
      }
    }

    return new GLResult(glName, glVersions, versionInfos);
  }

  private Query buildQuery(final String q) {
    String[] split = q.split(" ");
    if (split.length == 1) {
      return new TermQuery(new Term("name", q.toLowerCase().trim()));
    }
    PhraseQuery phraseQuery = new PhraseQuery();
    for (String s : split) {
      phraseQuery.add(new Term("name", s.toLowerCase().trim()));
    }
    return phraseQuery;
  }

  private ScoreDoc[] findRequired(final String gl, final IndexSearcher searcher) throws IOException {
    BooleanQuery anyExistingType = new BooleanQuery();
    anyExistingType.add(new TermQuery(new Term("type", "command")), Occur.SHOULD);
    anyExistingType.add(new TermQuery(new Term("type", "enum")), Occur.SHOULD);

    BooleanQuery query = new BooleanQuery();
    query.add(new TermQuery(new Term("byName", gl.toLowerCase())), Occur.MUST);
    query.add(anyExistingType, Occur.MUST);

    return executeQuery(searcher, query);
  }

  private ScoreDoc[] findRemoved(final String gl, final IndexSearcher searcher) throws IOException {
    BooleanQuery anyExistingType = new BooleanQuery();
    anyExistingType.add(new TermQuery(new Term("type", "command-removed")), Occur.SHOULD);
    anyExistingType.add(new TermQuery(new Term("type", "enum-removed")), Occur.SHOULD);

    BooleanQuery query = new BooleanQuery();
    query.add(new TermQuery(new Term("byName", gl.toLowerCase())), Occur.MUST);
    query.add(anyExistingType, Occur.MUST);

    return executeQuery(searcher, query);
  }

  private ScoreDoc[] executeQuery(final IndexSearcher searcher, BooleanQuery query) throws IOException {
    TopScoreDocCollector collector = TopScoreDocCollector.create(HITS_PER_PAGE, true);
    searcher.search(query, collector);
    return collector.topDocs().scoreDocs;
  }

  private IndexWriter createIndexWriter() throws IOException {
    return new IndexWriter(index, indexWriterConfig());
  }

  private IndexWriterConfig indexWriterConfig() {
    return new IndexWriterConfig(Version.LUCENE_44, ANALYZER);
  }

  /**
   * A VersionInfo represents a single GL-Version and keeps information which
   * api supports it.
   */
  private static class VersionInfo {
    // both could be null to represent not available for this version
    private Integer compatibilityProfile;
    private Integer coreProfile;
    private Integer esProfile;

    public VersionInfo() {
      this.compatibilityProfile = null;
      this.coreProfile = null;
      this.esProfile = null;
    }
  }

  /**
   * The GLResult is returned for the getGLInfo() call.
   */
  public static class GLResult {
    private final String glName;
    private final List<String> versions;
    private final List<ApiWithSupportedVersions> list = new ArrayList<ApiWithSupportedVersions>();

    public GLResult(final String glName, final Set<String> glVersions, final Map<String, VersionInfo> versionInfos) {
      this.glName = glName;
      this.versions = new ArrayList<String>(glVersions);
      list.add(new ApiWithSupportedVersions("OpenGL", extractCompatibility(versionInfos)));
      list.add(new ApiWithSupportedVersions("OpenGL Core Profile", extractCore(versionInfos)));
      list.add(new ApiWithSupportedVersions("OpenGL ES", extractES(versionInfos)));
    }

    /**
     * This generates the result data as a static minimal table. Used for systems that don't like
     * JSON data. Usually GSON is used to serialize this class directly to JSON data. This method
     * is only used for special cases.
     * @return the table html
     */
    public String generateTable() {
      StringBuilder result = new StringBuilder();

      result.append("<table>");

      // header
      result.append("<tr>");
      result.append("<th>").append("API").append("</th>");
      for (String version : versions) {
        result.append("<th>").append(version).append("</th>");
      }
      result.append("</tr>");

      // the data
      for (ApiWithSupportedVersions version : list) {
        version.toHTML(result);
      }

      result.append("</table>");
      return result.toString();
    }

    String getName() {
      return glName;
    }

    List<String> getVersions() {
      return Collections.unmodifiableList(versions);
    }

    List<ApiWithSupportedVersions> getList() {
      return Collections.unmodifiableList(list);
    }

    private List<String> extractCompatibility(final Map<String, VersionInfo> versionInfos) {
      List<String> result = new ArrayList<String>();
      for (String version : versions) {
        VersionInfo versionInfo = versionInfos.get(version);
        result.add(toString(versionInfo.compatibilityProfile));
      }
      return result;
    }

    private List<String> extractCore(final Map<String, VersionInfo> versionInfos) {
      List<String> result = new ArrayList<String>();
      for (String version : versions) {
        VersionInfo versionInfo = versionInfos.get(version);
        result.add(toString(versionInfo.coreProfile));
      }
      return result;
    }

    private List<String> extractES(final Map<String, VersionInfo> versionInfos) {
      List<String> result = new ArrayList<String>();
      for (String version : versions) {
        VersionInfo versionInfo = versionInfos.get(version);
        result.add(toString(versionInfo.esProfile));
      }
      return result;
    }

    private String toString(final Integer supported) {
      if (supported == null) {
        return "-";
      }
      if (supported >= 1) {
        return "Y";
      }
      return "N";
    }

    /**
     * A label ("OpenGL", "Core", "OpenGL ES") and a list of Infos. The
     * supported list contains the exact same number as the header list.
     */
    public static class ApiWithSupportedVersions {
      private String api;
      private List<String> supported = new ArrayList<String>();

      public ApiWithSupportedVersions(final String label, final List<String> supported) {
        this.api = label;
        this.supported = new ArrayList<String>(supported);
      }

      public void toHTML(final StringBuilder result) {
        result.append("<tr>");
        result.append("<td>").append(api).append("</td>");

        for (String s : supported) {
          result.append("<td>").append(s).append("</td>");
        }
        result.append("</tr>");
      }

      String getApi() {
        return api;
      }

      List<String> getSupported() {
        return supported;
      }
    }
  }

  /**
   * The Resultset for the Typeahead call.
   * 
   * @author void
   */
  public static class TypeaheadResult {
    private static final String MESSAGE_DIRECT_HIT = "Showing result ({0}ms)";
    private static final String MESSAGE_SINGLE_PAGE = "Showing {0} results ({1}ms)";
    private static final String MESSAGE_MORE = "Showing first {0} results out of {1} ({2}ms)";
    private final String[] result;
    private final String stats;

    public TypeaheadResult(final String[] result, final long time, final Integer actual, final Integer total) {
      this.result = result;
      this.stats = getMessage(time, actual, total);
    }

    public String[] getResult() {
      return result;
    }

    public String getStats() {
      return stats;
    }

    private String getMessage(final long time, final Integer actual, final Integer total) {
      if (actual == 1 && total == 1) {
        return MessageFormat.format(MESSAGE_DIRECT_HIT, toFloatString(time / 1000000.f));
      } else if (actual == total) {
        return MessageFormat.format(MESSAGE_SINGLE_PAGE, actual, toFloatString(time / 1000000.f));
      }
      return MessageFormat.format(MESSAGE_MORE, actual, total, toFloatString(time / 1000000.f));
    }

    private String toFloatString(final float value) {
      NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.ENGLISH);
      DecimalFormat decimalFormat = (DecimalFormat) numberFormat;
      decimalFormat.applyPattern("######.###");
      return decimalFormat.format(value);
    }
  }
}

package com.lessvoid.indexgl.registry;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;

/**
 * This class parses the gl.xml.
 */
public class RegistryParser {

  /**
   * Parse the XML document from the given InputStream and store the results in the IndexWriter given.
   *
   * @param stream the stream to parse
   * @param writer the writer to write
   * @throws Exception
   */
  public void process(final InputStream stream, final IndexWriter writer) throws Exception {
    XMLInputFactory factory = XMLInputFactory.newInstance();
    XMLStreamReader reader = factory.createXMLStreamReader(stream);

    while (reader.hasNext()) {
      int event = reader.next();

      if (XMLStreamConstants.START_ELEMENT == event) {
        if ("feature".equals(reader.getLocalName())) {
          parseFeature(reader, writer);
        }
      }
    }
  }

  private void parseFeature(final XMLStreamReader reader, final IndexWriter writer) throws Exception {
    FeatureCollector featureCollector = new FeatureCollector(reader, writer);

    while (reader.hasNext()) {
      int event = reader.next();
      switch (event) {
        case XMLStreamConstants.END_ELEMENT:
          if ("feature".equals(reader.getLocalName())) {
            featureCollector.writeDocs();
            return;
          }
          break;
        case XMLStreamConstants.START_ELEMENT:
          if ("require".equals(reader.getLocalName())) {
            parseRequire(reader, featureCollector);
          } else if ("remove".equals(reader.getLocalName())) {
            parseRemove(reader, featureCollector);
          }
          break;
      }
    }
  }

  private void parseRequire(final XMLStreamReader reader, final FeatureCollector featureCollector) throws Exception {
    while (reader.hasNext()) {
      int event = reader.next();
      switch (event) {
        case XMLStreamConstants.END_ELEMENT:
          if ("require".equals(reader.getLocalName())) {
            return;
          }
          break;
        case XMLStreamConstants.START_ELEMENT:
          if ("command".equals(reader.getLocalName())) {
            parse(reader, featureCollector, Type.Command, "command");
          } else if ("enum".equals(reader.getLocalName())) {
            parse(reader, featureCollector, Type.Enum, "enum");
          }
          break;
      }
    }
  }

  private void parseRemove(final XMLStreamReader reader, final FeatureCollector featureCollector) throws Exception {
    while (reader.hasNext()) {
      int event = reader.next();
      switch (event) {
        case XMLStreamConstants.END_ELEMENT:
          if ("remove".equals(reader.getLocalName())) {
            return;
          }
          break;
        case XMLStreamConstants.START_ELEMENT:
          if ("command".equals(reader.getLocalName())) {
            parse(reader, featureCollector, Type.CommandRemoved, "command");
          } else if ("enum".equals(reader.getLocalName())) {
            parse(reader, featureCollector, Type.EnumRemoved, "enum");
          }
          break;
      }
    }
  }

  private void parse(
      final XMLStreamReader reader,
      final FeatureCollector featureCollector,
      final Type type,
      final String tag) throws Exception {
    String name = reader.getAttributeValue(null, "name");
    featureCollector.addFeature(new Feature(name, type));

    parseUntilEndElement(reader, tag);
  }

  private void parseUntilEndElement(final XMLStreamReader reader, final String tag) throws XMLStreamException {
    while (reader.hasNext()) {
      int event = reader.next();
      if (event == XMLStreamConstants.END_ELEMENT) {
        if (tag.equals(reader.getLocalName())) {
          return;
        }
      }
    }
  }

  private static enum Type {
    Command("command"),
    CommandRemoved("command-removed"),
    Enum("enum"),
    EnumRemoved("enum-removed");

    private final String typeName;
    private Type(final String typeName) {
      this.typeName = typeName;
    }

    public String toString() {
      return typeName;
    }
  }

  private static class Feature {
    private final String name;
    private final Type type;

    public Feature(final String name, final Type type) {
      this.name = name;
      this.type = type;
    }
  }

  private static class FeatureCollector {
    private final String api;
    private final String number;
    private final IndexWriter indexWriter;
    private final List<Feature> features = new ArrayList<Feature>();

    public FeatureCollector(final XMLStreamReader reader, final IndexWriter writer) {
      api = reader.getAttributeValue(null, "api");
      number = reader.getAttributeValue(null, "number");
      indexWriter = writer;
    }

    public void addFeature(final Feature feature) {
      features.add(feature);
    }

    public void writeDocs() throws IOException {
      for (Feature feature : features) {
        writeDoc(api, number, feature.name, feature.type.toString());
      }
    }

    private void writeDoc(final String api, final String number, final String name, final String type) throws IOException {
      Document doc = new Document();
      doc.add(new StringField("api", api, Field.Store.YES));
      doc.add(new StringField("number", number, Field.Store.YES));
      doc.add(new StringField("api-number", api + ":" + number, Field.Store.YES));
      doc.add(new TextField("name", name, Field.Store.YES));
      doc.add(new StringField("byName", name.toLowerCase(), Field.Store.NO));
      doc.add(new StringField("type", type, Field.Store.YES));
      System.out.println(String.format("%4s, %3s, %50s, %s", api, number, name, type));
      indexWriter.addDocument(doc);
    }
  }
}

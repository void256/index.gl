package com.lessvoid.indexgl.registry;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

/**
 * <table><tr><th>API</th><th>1.0</th><th>1.1</th><th>1.2</th><th>1.3</th><th>1.4</th><th>1.5</th><th>2.0</th><th>2.1</th><th>3.0</th><th>3.1</th><th>3.2</th><th>3.3</th><th>4.0</th><th>4.1</th><th>4.2</th><th>4.3</th><th>4.4</th></tr><tr><td>OpenGL</td><td>Y</td><td>Y</td><td>Y</td><td>Y</td><td>Y</td><td>Y</td><td>Y</td><td>Y</td><td>Y</td><td>Y</td><td>Y</td><td>Y</td><td>Y</td><td>Y</td><td>Y</td><td>Y</td><td>Y</td></tr><tr><td>OpenGL Core Profile</td><td>-</td><td>-</td><td>-</td><td>-</td><td>-</td><td>-</td><td>-</td><td>-</td><td>-</td><td>-</td><td>N</td><td>N</td><td>N</td><td>N</td><td>N</td><td>N</td><td>N</td></tr><tr><td>OpenGL ES</td><td>-</td><td>-</td><td>-</td><td>-</td><td>-</td><td>-</td><td>-</td><td>-</td><td>-</td><td>-</td><td>-</td><td>-</td><td>-</td><td>-</td><td>-</td><td>-</td><td>-</td></tr></table>
 */
public class AnalyzerUtils {
  public static List<String> tokensFromAnalysis(final Analyzer analyzer, final String text) throws IOException {
    List<String> result = new ArrayList<String>();

    TokenStream stream = analyzer.tokenStream(null, new StringReader(text));
    CharTermAttribute cattr = stream.addAttribute(CharTermAttribute.class);
    stream.reset();
    while (stream.incrementToken()) {
      result.add(cattr.toString());
    }
    stream.end();
    stream.close();

    return result;
  }
}

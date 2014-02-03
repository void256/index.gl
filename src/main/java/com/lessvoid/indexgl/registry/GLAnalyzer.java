package com.lessvoid.indexgl.registry;

import java.io.Reader;
import java.util.Arrays;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.core.WhitespaceTokenizer;
import org.apache.lucene.analysis.miscellaneous.WordDelimiterFilter;
import org.apache.lucene.analysis.ngram.NGramTokenFilter;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.util.Version;

/**
 * A special Lucene Analyzer for Open GL identifiers.
 * 
 * @author void
 */
public class GLAnalyzer extends Analyzer {
  private final Version version;

  public GLAnalyzer(final Version version) {
    this.version = version;
  }

  @Override
  protected Analyzer.TokenStreamComponents createComponents(final String fieldName, final Reader reader) {
    Tokenizer source = new WhitespaceTokenizer(version, reader);
    TokenStream stream = source;

    stream = new WordDelimiterFilter(stream, WordDelimiterFilter.GENERATE_WORD_PARTS
        | WordDelimiterFilter.GENERATE_NUMBER_PARTS |
        // WordDelimiterFilter.CATENATE_WORDS |
        // WordDelimiterFilter.CATENATE_NUMBERS |
        // WordDelimiterFilter.CATENATE_ALL |
        WordDelimiterFilter.PRESERVE_ORIGINAL |
        WordDelimiterFilter.SPLIT_ON_CASE_CHANGE, // |
        //WordDelimiterFilter.SPLIT_ON_NUMERICS,
        null);

    stream = new LowerCaseFilter(version, stream);
    stream = new StopFilter(version, stream, new CharArraySet(version, Arrays.asList("gl"), true));
    stream = new NGramTokenFilter(version, stream, 1, 100);
    return new TokenStreamComponents(source, stream);
  }
}

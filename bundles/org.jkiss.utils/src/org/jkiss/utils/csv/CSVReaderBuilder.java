/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * This package contains a slightly modified version of opencsv library
 * without unwanted functionality and dependencies, licensed under Apache 2.0.
 *
 * See https://search.maven.org/artifact/com.opencsv/opencsv/3.4/bundle
 * See http://opencsv.sf.net/
 */
package org.jkiss.utils.csv;


import java.io.Reader;

/**
 * Builder for creating a CSVReader.  This should be the preferred method of
 * creating a Reader as there are so many possible values to be set it is
 * impossible to have constructors for all of them and keep backwards
 * compatibility with previous constructors.
 *
 * <code>
 * final CSVParser parser =
 * new CSVParserBuilder()
 * .withSeparator('\t')
 * .withIgnoreQuotations(true)
 * .build();
 * final CSVReader reader =
 * new CSVReaderBuilder(new StringReader(csv))
 * .withSkipLines(1)
 * .withCSVParser(parser)
 * .build();
 * </code>
 *
 * @see CSVReader
 */
public class CSVReaderBuilder {

    private final CSVParserBuilder parserBuilder = new CSVParserBuilder();
    private final Reader reader;
    private int skipLines = CSVReader.DEFAULT_SKIP_LINES;
    /*@Nullable*/private CSVParser csvParser = null;
    private boolean keepCR;
    private boolean verifyReader = CSVReader.DEFAULT_VERIFY_READER;
    private CSVReaderNullFieldIndicator nullFieldIndicator = CSVReaderNullFieldIndicator.NEITHER;

   /**
    * Sets the reader to an underlying CSV source.
    *
    * @param reader the reader to an underlying CSV source.
    */
   public CSVReaderBuilder(
         final Reader reader) {
      if (reader == null) {
         throw new IllegalArgumentException("Reader may not be null");
      }
      this.reader = reader;
   }

    /**
     * Used by unit tests.
     *
     * @return the reader.
     */
    protected Reader getReader() {
        return reader;
    }

    /**
     * used by unit tests.
     *
     * @return The set number of lines to skip
     */
    protected int getSkipLines() {
        return skipLines;
    }

    /**
     * used by unit tests.
     *
     * @return the csvParser used by the builder.
     */
    protected CSVParser getCsvParser() {
        return csvParser;
    }

    /**
    * Sets the line number to skip for start reading.
     *
     * @param skipLines the line number to skip for start reading.
     * @return the CSVReaderBuilder with skipLines set.
    */
    public CSVReaderBuilder withSkipLines(
         final int skipLines) {
      this.skipLines = (skipLines <= 0 ? 0 : skipLines);
      return this;
   }


    /**
     * Sets the parser to use to parse the input.
     *
     * @param csvParser the parser to use to parse the input.
     * @return the CSVReaderBuilder with the CSVParser set.
    */
    public CSVReaderBuilder withCSVParser(
         final /*@Nullable*/ CSVParser csvParser) {
      this.csvParser = csvParser;
       return this;
   }


    /**
     * Creates the CSVReader.
     * @return the CSVReader based on the set criteria.
     */
    public CSVReader build() {
      final CSVParser parser =
              (csvParser != null ? csvParser : parserBuilder.withFieldAsNull(nullFieldIndicator).build());
       return new CSVReader(reader, skipLines, parser, keepCR, verifyReader);
   }

    /**
     * Sets if the reader will keep or discard carriage returns.
     *
     * @param keepCR - true to keep carriage returns, false to discard.
     * @return the CSVReaderBuilder based on the set criteria.
     */
    public CSVReaderBuilder withKeepCarriageReturn(boolean keepCR) {
        this.keepCR = keepCR;
        return this;
    }

    /**
     * Returns if the reader built will keep or discard carriage returns.
     *
     * @return true if the reader built will keep carriage returns, false otherwise.
     */
    protected boolean keepCarriageReturn() {
        return this.keepCR;
    }

    /**
     * Checks to see if the CSVReader should verify the reader state before reads or not.
     *
     * This should be set to false if you are using some form of asynchronous reader (like readers created
     * by the java.nio.* classes).
     *
     * The default value is true.
     *
     * @param verifyReader true if CSVReader should verify reader before each read, false otherwise.
     * @return The CSVReaderBuilder based on this criteria.
     */
    public CSVReaderBuilder withVerifyReader(boolean verifyReader) {
        this.verifyReader = verifyReader;
        return this;
    }

    /**
     * Checks to see if it should treat an field with two separators, two quotes, or both as a null field.
     *
     * @param indicator - CSVReaderNullFieldIndicator set to what should be considered a null field.
     * @return The CSVReaderBuilder based on this criteria.
     */
    public CSVReaderBuilder withFieldAsNull(CSVReaderNullFieldIndicator indicator) {
        this.nullFieldIndicator = indicator;
        return this;
    }
}

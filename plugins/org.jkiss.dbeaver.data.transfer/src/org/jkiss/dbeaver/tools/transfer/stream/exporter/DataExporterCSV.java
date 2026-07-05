/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.tools.transfer.stream.exporter;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.data.DBDContent;
import org.jkiss.dbeaver.model.data.DBDContentStorage;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.model.exec.DBCResultSet;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.DBExecUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.tools.transfer.DTUtils;
import org.jkiss.dbeaver.tools.transfer.stream.IAppendableDataExporter;
import org.jkiss.dbeaver.tools.transfer.stream.IStreamDataExporterSite;
import org.jkiss.dbeaver.tools.transfer.stream.StreamTransferUtils;
import org.jkiss.dbeaver.utils.ContentUtils;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.Pair;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * CSV Exporter
 */
public class DataExporterCSV extends StreamExporterAbstract implements IAppendableDataExporter {

    public static final String PROCESSOR_ID = "stream.csv";

    public static final String PROP_DELIMITER = "delimiter";
    public static final String PROP_ROW_DELIMITER = "rowDelimiter";
    private static final String PROP_HEADER = "header";
    private static final String PROP_HEADER_FORMAT = "headerFormat";
    private static final String PROP_HEADER_CASE = "headerCase";
    public static final String PROP_QUOTE_CHAR = "quoteChar";
    private static final String PROP_QUOTE_ALWAYS = "quoteAlways";
    public static final String PROP_QUOTE_NEVER = "quoteNever";
    private static final String PROP_NULL_STRING = "nullString";
    private static final String PROP_FORMAT_NUMBERS = "formatNumbers";
    public static final String PROP_LINE_FEED_ESCAPE_STRING = "lineFeedEscapeString";
    private static final String PROP_FORMAT_ARRAY = "formatArray";

    private static final String DEF_QUOTE_CHAR = "\"";
    private static final String DEFAULT_ARRAY_BRACKETS = "{ }";
    public static final int READ_BUFFER_SIZE = 10;
    private boolean formatNumbers;

    enum HeaderPosition {
        none,
        top,
        bottom,
        both
    }

    enum HeaderFormat {
        label,
        description,
        both
    }

    private static final String ROW_DELIMITER_DEFAULT = "default";
    private static final String LF = "\n";
    private static final String CRLF = "\r\n";

    private String delimiter;
    private String quoteChar = "\"";
    private String lineFeedEscapeString;

    private boolean useQuotes = true;
    private QuoteStrategy quoteStrategy = QuoteStrategy.DISABLED;
    private String rowDelimiter;
    private String nullString;
    private HeaderPosition headerPosition;
    private HeaderFormat headerFormat;
    private DBPIdentifierCase headerCase;
    private DBDAttributeBinding[] columns;
    private DataExporterArrayFormat dataExporterArrayFormat;

    private final StringBuilder lineBuffer = new StringBuilder();
    private CsvLineEscaper csvLineEscaper;

    @Override
    public void init(IStreamDataExporterSite site) throws DBException
    {
        super.init(site);
        Map<String, Object> properties = site.getProperties();
        this.delimiter = StreamTransferUtils.getDelimiterString(properties, PROP_DELIMITER);
        this.rowDelimiter = StreamTransferUtils.getDelimiterString(properties, PROP_ROW_DELIMITER);
        if (ROW_DELIMITER_DEFAULT.equalsIgnoreCase(this.rowDelimiter.trim())) {
            this.rowDelimiter = GeneralUtils.getDefaultLineSeparator();
        }
        this.lineFeedEscapeString = CommonUtils.toString(properties.get(PROP_LINE_FEED_ESCAPE_STRING), "")
            .replace("\\t", "\t")
            .replace("\\n", "\n")
            .replace("\\r", "\r");
        Object quoteProp = properties.get(PROP_QUOTE_CHAR);
        String quoteStr = quoteProp == null ? DEF_QUOTE_CHAR : quoteProp.toString();
        if (!CommonUtils.isEmpty(quoteStr)) {
            quoteChar = quoteStr;
        }
        if (CommonUtils.toBoolean(properties.get(PROP_QUOTE_NEVER))) {
            quoteChar = "";
        }

        Object nullStringProp = properties.get(PROP_NULL_STRING);
        nullString = nullStringProp == null ? null : nullStringProp.toString();
        useQuotes = CommonUtils.isNotEmpty(quoteChar);

        if (useQuotes && quoteChar.equals(delimiter)) {
            throw new IllegalArgumentException("Quotes and separator can't be the same string: " + quoteChar);
        }

        quoteStrategy = QuoteStrategy.fromValue(CommonUtils.toString(properties.get(PROP_QUOTE_ALWAYS)));
        if (headerPosition == null) {
            headerPosition = CommonUtils.valueOf(HeaderPosition.class, CommonUtils.toString(properties.get(PROP_HEADER)), HeaderPosition.top);
        }

        headerFormat = CommonUtils.valueOf(HeaderFormat.class, CommonUtils.toString(properties.get(PROP_HEADER_FORMAT)), HeaderFormat.label);
        formatNumbers = CommonUtils.toBoolean(getSite().getProperties().get(PROP_FORMAT_NUMBERS));
        headerCase = switch (CommonUtils.toString(properties.get(PROP_HEADER_CASE))) {
            case "as is" -> DBPIdentifierCase.MIXED;
            case "lower" -> DBPIdentifierCase.LOWER;
            default -> DBPIdentifierCase.UPPER;
        };
        String arrFormatProp = CommonUtils.toString(
            properties.get(PROP_FORMAT_ARRAY),
            DEFAULT_ARRAY_BRACKETS
        ).trim();
        if (arrFormatProp.isEmpty()) {
            arrFormatProp = DEFAULT_ARRAY_BRACKETS;
        }
        dataExporterArrayFormat = DataExporterArrayFormat.getArrayFormat(arrFormatProp);
        csvLineEscaper = new CsvLineEscaper();
    }

    @Override
    protected DBDDisplayFormat getValueExportFormat(DBDAttributeBinding column) {
        if ((column.getDataKind() == DBPDataKind.NUMERIC && !formatNumbers) || column.getDataKind() == DBPDataKind.ARRAY) {
            return DBDDisplayFormat.NATIVE;
        }
        return super.getValueExportFormat(column);
    }

    @Override
    public void exportHeader(DBCSession session) throws DBException, IOException
    {
        columns = getSite().getAttributes();
        if (headerPosition == HeaderPosition.top || headerPosition == HeaderPosition.both) {
            if (headerFormat != HeaderFormat.label) {
                DBSEntity srcEntity = DBUtils.getAdapter(DBSEntity.class, getSite().getSource());
                DBExecUtils.bindAttributes(session, srcEntity, null, columns, null);
            }
            printHeader();
        }
    }

    private void printHeader()
    {
        for (int i = 0, columnsSize = columns.length; i < columnsSize; i++) {
            DBDAttributeBinding column = columns[i];
            String colName = column.getName();
            if (headerFormat == HeaderFormat.description) {
                colName = column.getDescription();
                if (colName == null) {
                    colName = column.getLabel();
                }
            } else {
                String colLabel = column.getLabel();
                if (CommonUtils.equalObjects(colLabel, colName)) {
                    colName = column.getParentObject() == null ? column.getName() : DBUtils.getObjectFullName(column, DBPEvaluationContext.UI);
                } else if (!CommonUtils.isEmpty(colLabel)) {
                    // Label has higher priority
                    colName = colLabel;
                }
                if (headerFormat == HeaderFormat.both) {
                    String description = column.getDescription();
                    if (!CommonUtils.isEmpty(description)) {
                        colName += ":" + description;
                    }
                }
            }
            writeCellValue(headerCase.transform(colName), true);
            if (i < columnsSize - 1) {
                writeDelimiter();
            }
        }
        writeRowLimit();
    }

    @Override
    public void exportRow(DBCSession session, DBCResultSet resultSet, Object[] row) throws DBException, IOException
    {
        for (int i = 0; i < row.length && i < columns.length; i++) {
            DBDAttributeBinding column = columns[i];
            if (row[i] instanceof DBDContent content) {
                // Content
                // Inline textual content and handle binaries in some special way
                try {
                    DBDContentStorage cs = content.getContents(session.getProgressMonitor());
                    if (cs == null) {
                        writeCellValue(DBConstants.NULL_VALUE_LABEL, true);
                    } else if (ContentUtils.isTextContent(content)) {
                        writeCellValue(cs.getContentReader());
                    } else {
//                        out.write(quoteChar);
                        getSite().writeBinaryData(cs);
//                        out.write(quoteChar);
                    }
                }
                finally {
                    DTUtils.closeContents(resultSet, content);
                }
            } else {
                String stringValue = super.getValueDisplayString(column, row[i]);
                boolean quote = false;
                if (column.getDataKind() == DBPDataKind.ARRAY) {
                    stringValue = editArrayPrefixAndSuffix(dataExporterArrayFormat, stringValue);
                }

                if (quoteStrategy == QuoteStrategy.DISABLED) {
                    if (!stringValue.isEmpty() && !(row[i] instanceof Number) && !(row[i] instanceof Date) && Character.isDigit(stringValue.charAt(0))) {
                        // Quote string values which starts from number
                        quote = true;
                    }
                } else if (quoteStrategy == QuoteStrategy.STRINGS) {
                    if (!stringValue.isEmpty() && !(row[i] instanceof Number) && !(row[i] instanceof Date)) {
                        quote = true;
                    }
                } else if (quoteStrategy == QuoteStrategy.ALL_BUT_NUMBERS) {
                    if (!(row[i] instanceof Number)) {
                        quote = true;
                    }
                }

                if (DBUtils.isNullValue(row[i])) {
                    if (CommonUtils.isNotEmpty(nullString)) {
                        writeCellValue(nullString, quote);
                    } else if (quoteStrategy == QuoteStrategy.ALL_INCLUDING_NULLS) {
                        writeCellValue("", true);
                    }
                } else {
                    writeCellValue(stringValue, quote);
                }
            }
            if (i < row.length - 1) {
                writeDelimiter();
            }
        }
        writeRowLimit();
    }

    private String editArrayPrefixAndSuffix(DataExporterArrayFormat modifiedFormat, String stringValue) {
        if (stringValue == null || stringValue.isEmpty()) {
            return stringValue;
        }

        stringValue = stringValue.trim();

        DataExporterArrayFormat currentArrayFormat = DataExporterArrayFormat.getArrayFormatOnPrefix(stringValue.charAt(0));
        if (currentArrayFormat.equals(modifiedFormat)) {
            return stringValue;
        }

        boolean insideQuotes = false;
        StringBuilder modifiedBuilder = new StringBuilder();
        for (char c : stringValue.toCharArray()) {
            if (c == '"') {
                insideQuotes = !insideQuotes;
            }
            if (!insideQuotes) {
                if (c == currentArrayFormat.getPrefix()) {
                    modifiedBuilder.append(modifiedFormat.getPrefix());
                    continue;
                } else if (c == currentArrayFormat.getSuffix()) {
                    modifiedBuilder.append(modifiedFormat.getSuffix());
                    continue;
                }
            }
            modifiedBuilder.append(c);
        }
        return modifiedBuilder.toString();
    }

    @Override
    public void exportFooter(DBRProgressMonitor monitor) {
        if (headerPosition == HeaderPosition.bottom || headerPosition == HeaderPosition.both) {
            printHeader();
        }
    }

    @Override
    public void importData(@NotNull IStreamDataExporterSite site) {
        final Path file = site.getOutputFile();
        if (file == null || !Files.exists(file)) {
            return;
        }
        // FIXME: Sources may be different and thus may have a different set of attributes
        headerPosition = HeaderPosition.none;
    }

    @Override
    public boolean shouldTruncateOutputFileBeforeExport() {
        return false;
    }

    private void writeCellValue(@NotNull String value, boolean quote) {
        if (CommonUtils.isNotEmpty(lineFeedEscapeString)) {
            writeLines(value.split(CRLF + "|" + LF, -1), quote, lineFeedEscapeString);
        } else {
            writePreparedCellValue(useQuotes ? escapeQuotes(value) : value, quote);
        }
    }

    private void writeCellValue(@NotNull Reader reader) throws IOException {
        try {
            csvLineEscaper.clearState();
            char[] chars = new char[READ_BUFFER_SIZE];
            for (int count = reader.read(chars); count > 0; count = reader.read(chars)) {
                csvLineEscaper.writeToLineBuffer(chars, count);
            }
            csvLineEscaper.writePending();
        } finally {
            ContentUtils.close(reader);
        }
        writePreparedCellValue(lineBuffer.toString(), csvLineEscaper.isLineNeedsQuotation());
    }

    private void writeLines(@NotNull String[] multiRow, boolean quote, @NotNull String customLineBreak) {
        StringJoiner multiLineBuffer = new StringJoiner(customLineBreak);
        for (String rowPart : multiRow) {
            multiLineBuffer.add(escapeQuotes(rowPart));
        }
        writePreparedCellValue(multiLineBuffer.toString(), quote || multiRow.length > 1);
    }

    private void writePreparedCellValue(@NotNull String preparedCellValue, boolean quote) {
        boolean isQuoteLines = useQuotes && (quote || cellValueNeedsQuotation(preparedCellValue));
        PrintWriter out = getWriter();
        if (isQuoteLines) {
            out.write(quoteChar);
        }
        out.write(preparedCellValue);
        if (isQuoteLines) {
            out.write(quoteChar);
        }
    }

    private boolean cellValueNeedsQuotation(@NotNull String line) {
        if (quoteStrategy == QuoteStrategy.ALL ||
            quoteStrategy == QuoteStrategy.ALL_INCLUDING_NULLS ||
            line.isEmpty()
        ) {
            return true;
        } else {
            return line.contains(delimiter) || line.contains(quoteChar) || line.indexOf('\n') != -1 || line.indexOf('\r') != -1;
        }
    }

    @NotNull
    private String escapeQuotes(@NotNull String line) {
        lineBuffer.setLength(0);
        // escape quotes with double quotes
        if (useQuotes && line.contains(quoteChar)) {
            int index = 0;
            while (index < line.length()) {
                if (isQuoteChar(line, index)) {
                    lineBuffer.append(quoteChar.repeat(2));
                    index += quoteChar.length();
                } else {
                    lineBuffer.append(line.charAt(index++));
                }
            }
            return lineBuffer.toString();
        } else {
            return line;
        }
    }

    private boolean isQuoteChar(@NotNull String value, int toffset) {
        // if separator is longer it might contain quote char in it.
        return (delimiter.length() <= quoteChar.length() || !value.startsWith(delimiter, toffset)) && value.startsWith(quoteChar, toffset);
    }

    private class CsvLineEscaper {

        // special chars must be parsed from the longest one
        private final List<Pair<CharStrategy, char[]>> orderedSpecialChars = new ArrayList<>();

        // tricky case when delimiter starts from quote and is longer then quote
        private boolean isDelimiterContainsQuotes;

        // case of buffer overwhelm with special char stats in the buffer end
        private int longestSpecialChar = 1;

        private char[] pending = new char[0];

        private boolean lineNeedsQuotation;

        public CsvLineEscaper() {
            if (CommonUtils.isNotEmpty(lineFeedEscapeString)) {
                orderedSpecialChars.add(Pair.of(CharStrategy.CRLF, CRLF.toCharArray()));
                orderedSpecialChars.add(Pair.of(CharStrategy.LF, LF.toCharArray()));
            }
            if (useQuotes && CommonUtils.isNotEmpty(quoteChar)) {
                orderedSpecialChars.add(Pair.of(CharStrategy.QUOTES, quoteChar.toCharArray()));
                isDelimiterContainsQuotes = delimiter.startsWith(quoteChar);
            }
            orderedSpecialChars.sort(Comparator.comparingInt((Pair<CharStrategy, char[]> p) -> p.getSecond().length)
                .reversed()
                .thenComparing(Pair::getFirst));
            if (!orderedSpecialChars.isEmpty()) {
                longestSpecialChar = Math.max(orderedSpecialChars.getFirst().getSecond().length, delimiter.length());
            }
        }

        public void writeToLineBuffer(@NotNull char[] chars, int readCount) {
            writeToLineBuffer(chars, readCount, longestSpecialChar);
        }

        public void writePending() {
            if (pending.length > 0) {
                writeToLineBuffer(new char[0], 0, 1);
            }
        }

        private void writeToLineBuffer(@NotNull char[] chars, int readCount, int pendingLength) {
            int totalWithPending = readCount + pending.length;
            int index = 0;
            while ((totalWithPending - index) >= pendingLength) {
                CharStrategy strategy = defineStrategy(chars, index);
                index += switch (strategy) {
                    case NORMAL_CHAR -> processNormalChar(chars[index]);
                    case QUOTES -> processQuotes();
                    case LF, CRLF -> processLinebreak(strategy);
                };
            }

            int length = totalWithPending - index;
            if (length > 0) {
                pending = new char[length];
                System.arraycopy(chars, index - length, pending, 0, pending.length);
            } else {
                pending = new char[0];
            }
        }

        private int processNormalChar(char character) {
            lineBuffer.append(character);
            return 1;
        }

        private int processQuotes() {
            if (!isLineNeedsQuotation()) {
                lineNeedsQuotation = true;
            }
            lineBuffer.append(quoteChar.repeat(2));
            return quoteChar.length();
        }

        private int processLinebreak(@NotNull CharStrategy lineBreakStrategy) {
            if (!isLineNeedsQuotation()) {
                lineNeedsQuotation = true;
            }
            lineBuffer.append(lineFeedEscapeString);
            return switch (lineBreakStrategy) {
                case LF -> LF.length();
                case CRLF -> CRLF.length();
                default -> throw new IllegalArgumentException("Only CRLF or LF is allowed here. This line must be unreachable");
            };
        }

        @NotNull
        private CharStrategy defineStrategy(@NotNull char[] chars, int toffset) {
            for (Pair<CharStrategy, char[]> strategyPair : orderedSpecialChars) {
                if (checkMatch(chars, toffset, strategyPair)) {
                    return strategyPair.getFirst();
                }
            }
            return CharStrategy.NORMAL_CHAR;
        }

        private boolean checkMatch(@NotNull char[] chars, int toffset, @NotNull Pair<CharStrategy, char[]> strategy) {
            return switch (strategy.getFirst()) {
                case CRLF, LF -> containsSpecialChar(chars, toffset, strategy.getSecond());
                case QUOTES -> containsQuotes(chars, toffset, strategy.getSecond());
                default -> false;
            };
        }

        private boolean containsQuotes(@NotNull char[] chars, int toffset, char[] quotes) {
            return isDelimiterContainsQuotes
                ? !containsSpecialChar(chars, toffset, delimiter.toCharArray()) && containsSpecialChar(chars, toffset, quotes)
                : containsSpecialChar(chars, toffset, quotes);
        }

        // pending = [a,b], chars = [c,d], specialChar = [a,b,c] -> true
        private boolean containsSpecialChar(@NotNull char[] chars, int toffset, @NotNull char[] specialChar) {
            if (toffset < pending.length) {
                int charsInPending = pending.length - toffset;
                // all in pending
                if (charsInPending >= specialChar.length) {
                    return Arrays.equals(pending, toffset, toffset + specialChar.length, specialChar, 0, specialChar.length);
                }
                // pending and chars
                return Arrays.equals(pending, toffset, pending.length, specialChar, 0, charsInPending)
                    && Arrays.equals(chars, 0, specialChar.length - charsInPending, specialChar, charsInPending, specialChar.length);

            }

            // look only in chars
            int charsOffset = toffset - pending.length;
            int charsEndIndex = charsOffset + specialChar.length;
            return charsEndIndex <= chars.length
                && Arrays.equals(chars, charsOffset, charsEndIndex, specialChar, 0, specialChar.length);
        }

        public void clearState() {
            pending = new char[0];
            lineBuffer.setLength(0);
            lineNeedsQuotation = false;
        }

        public boolean isLineNeedsQuotation() {
            return lineNeedsQuotation;
        }

        private enum CharStrategy {
            QUOTES,
            LF,
            CRLF,
            NORMAL_CHAR
        }

    }

    private void writeDelimiter()
    {
        getWriter().write(delimiter);
    }

    private void writeRowLimit()
    {
        getWriter().write(rowDelimiter);
    }

}

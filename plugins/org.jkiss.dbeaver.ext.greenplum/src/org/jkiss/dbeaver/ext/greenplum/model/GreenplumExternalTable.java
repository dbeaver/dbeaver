/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
 * Copyright (C) 2019 Dmitriy Dubson (ddubson@pivotal.io)
 * Copyright (C) 2019 Gavin Shaw (gshaw@pivotal.io)
 * Copyright (C) 2019 Zach Marcin (zmarcin@pivotal.io)
 * Copyright (C) 2019 Nikhil Pawar (npawar@pivotal.io)
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
package org.jkiss.dbeaver.ext.greenplum.model;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreSchema;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreTable;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreTableColumn;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.IPropertyValueListProvider;
import org.jkiss.dbeaver.model.meta.IPropertyValueTransformer;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.CommonUtils;

import java.sql.ResultSet;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class GreenplumExternalTable extends PostgreTable {
    private static final String DEFAULT_FORMAT_OPTIONS = "delimiter ',' null '' escape '\"' quote '\"' header";

    public enum FormatType {
        c("CSV"),
        t("TEXT"),
        b("CUSTOM");

        private String formatType;

        FormatType(String formatTypeString) {
            this.formatType = formatTypeString;
        }

        public String getValue() {
            return formatType;
        }

        public static FormatType fromValue(String formatTypeString) throws IllegalArgumentException {
            return Arrays
                    .stream(values())
                    .filter(formatType -> formatType.getValue().equalsIgnoreCase(formatTypeString))
                    .findFirst()
                    .orElse(b);
        }
    }

    public enum RejectLimitType {
        r("ROWS"),
        p("PERCENT ");

        private String rejectLimitType;

        RejectLimitType(String rejectLimitTypeString) {
            this.rejectLimitType = rejectLimitTypeString;
        }

        public String getValue() {
            return rejectLimitType;
        }

    }

    private GreenplumExternalTableUriLocationsHandler uriLocationsHandler;
    private String execLocation;
    private FormatType formatType;
    private String formatOptions;
    private String encoding;
    private RejectLimitType rejectLimitType;
    private int rejectLimit;
    private boolean writable;
    private boolean temporaryTable;
    private boolean loggingErrors;
    private String command;

    public GreenplumExternalTable(PostgreSchema catalog) {
        super(catalog);
        this.uriLocationsHandler = new GreenplumExternalTableUriLocationsHandler("", '\n');
        this.encoding = GreenplumCharacterSet.UNICODE_8BIT.getCharacterSetValue();
        this.formatType = FormatType.t;
        this.formatOptions = DEFAULT_FORMAT_OPTIONS;
    }

    public GreenplumExternalTable(PostgreSchema catalog,
                                  ResultSet dbResult) {
        super(catalog, dbResult);
        this.uriLocationsHandler = new GreenplumExternalTableUriLocationsHandler(
                JDBCUtils.safeGetStringTrimmed(dbResult, "urilocation"), ',');
        this.execLocation = JDBCUtils.safeGetString(dbResult, "execlocation");
        this.formatType = CommonUtils.valueOf(FormatType.class, JDBCUtils.safeGetString(dbResult, "fmttype"), FormatType.b);
        this.formatOptions = JDBCUtils.safeGetString(dbResult, "fmtopts");
        this.encoding = JDBCUtils.safeGetString(dbResult, "encoding");

        this.rejectLimit = JDBCUtils.safeGetInt(dbResult, "rejectlimit");
        String rejectlimittype = JDBCUtils.safeGetString(dbResult, "rejectlimittype");
        this.writable = JDBCUtils.safeGetBoolean(dbResult, "writable");
        this.temporaryTable = JDBCUtils.safeGetBoolean(dbResult, "is_temp_table");
        this.loggingErrors = !getDataSource().isServerVersionAtLeast(9, 4)
                && JDBCUtils.safeGetBoolean(dbResult, "is_logging_errors");
        this.command = JDBCUtils.safeGetString(dbResult, "command");
        if (rejectlimittype != null && rejectlimittype.length() > 0) {
            this.rejectLimitType = CommonUtils.valueOf(RejectLimitType.class, rejectlimittype, RejectLimitType.r);
        } else {
            this.rejectLimitType = null;
        }
    }

    @Property(viewable = true, editable = true, updatable = true, order = 24,
            multiline = true, valueRenderer = ExternalTableUriLocationsRenderer.class)
    public String getUriLocations() {
        return this.uriLocationsHandler.getCommaSeparatedList();
    }

    public void setUriLocations(String lineFeedSeparatedUriLocations) {
        this.uriLocationsHandler =
                new GreenplumExternalTableUriLocationsHandler(lineFeedSeparatedUriLocations, '\n');
    }

    public String getExecLocation() {
        return execLocation;
    }

    @Property(viewable = true, editable = true, updatable = true, order = 26,
            listProvider = ExternalTableFormatTypeProvider.class)
    public String getFormatType() {
        if (this.formatType == null) {
            return null;
        } else {
            return this.formatType.getValue();
        }
    }

    public void setFormatType(String formatType) {
        this.formatType = FormatType.fromValue(formatType);
    }

    @Property(viewable = true, editable = true, updatable = true, order = 27)
    public String getFormatOptions() {
        return formatOptions;
    }

    public void setFormatOptions(String formatOptions) {
        this.formatOptions = formatOptions;
    }

    @Property(viewable = true, editable = true, updatable = true, order = 25,
            listProvider = GreenplumCharacterSetProvider.class)
    public String getEncoding() {
        return encoding;
    }

    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    public RejectLimitType getRejectLimitType() {
        return rejectLimitType;
    }

    public int getRejectLimit() {
        return rejectLimit;
    }

    public boolean isWritable() {
        return writable;
    }

    public boolean isTemporaryTable() {
        return temporaryTable;
    }

    public boolean isLoggingErrors() {
        return loggingErrors;
    }

    public String getCommand() {
        return command;
    }

    public String generateDDL(DBRProgressMonitor monitor) throws DBException {
        StringBuilder ddlBuilder = new StringBuilder();
        ddlBuilder.append("CREATE ")
                .append(this.isWritable() ? "WRITABLE " : "")
                .append("EXTERNAL ")
                .append(isWebTable() ? "WEB " : "")
                .append(this.isTemporaryTable() ? "TEMPORARY " : "")
                .append("TABLE ")
                .append(addDatabaseQualifier())
                .append(this.getName())
                .append(" (\n");

        List<PostgreTableColumn> tableColumns = filterOutNonMetadataColumns(monitor);

        if (tableColumns.size() == 0) {
            ddlBuilder.append("\n)\n");
        } else if (tableColumns.size() == 1) {
            PostgreTableColumn column = tableColumns.get(0);
            ddlBuilder.append("\t").append(column.getName()).append(" ").append(column.getTypeName()).append("\n)\n");
        } else {
            ddlBuilder.append(tableColumns
                    .stream()
                    .map(field -> "\t" + field.getName() + " " + field.getTypeName())
                    .collect(Collectors.joining(",\n")));
            ddlBuilder.append("\n)\n");
        }

        if (CommonUtils.isNotEmpty(this.getUriLocations())) {
            ddlBuilder.append("LOCATION (\n");

            ddlBuilder.append(this.uriLocationsHandler
                    .stream()
                    .map(location -> "\t'" + location + "'")
                    .collect(Collectors.joining(",\n")));

            ddlBuilder.append("\n) ").append(determineExecutionLocation()).append("\n");
        } else if (tableHasCommand()) {
            ddlBuilder.append("EXECUTE '").append(this.getCommand()).append("' ").append(determineExecutionLocation()).append("\n");
        }

        ddlBuilder.append("FORMAT '").append(this.getFormatType()).append("'");

        if(this.getFormatOptions() != null) {
            ddlBuilder.append(generateFormatOptions(this.formatType, this.getFormatOptions()));
        }

        if (this.getEncoding() != null && this.getEncoding().length() > 0) {
            ddlBuilder.append("\nENCODING '").append(this.getEncoding()).append("'");
        }

        if (this.isLoggingErrors()) {
            ddlBuilder.append("\nLOG ERRORS");
        }

        if (this.getRejectLimit() > 0 && this.getRejectLimitType() != null) {
            ddlBuilder.append(this.isLoggingErrors() ? " " : "\n")
                    .append("SEGMENT REJECT LIMIT ")
                    .append(this.getRejectLimit())
                    .append(" ")
                    .append(this.getRejectLimitType().getValue());
        }

        return ddlBuilder.toString();
    }

    @Override
    public String generateChangeOwnerQuery(String owner) {
        assert CommonUtils.isNotEmpty(owner);

        return "ALTER EXTERNAL TABLE " + DBUtils.getObjectFullName(this, DBPEvaluationContext.DDL) + " OWNER TO " + owner;
    }

    private List<PostgreTableColumn> filterOutNonMetadataColumns(DBRProgressMonitor monitor) throws DBException {
        List<PostgreTableColumn> tableColumns;
        Stream<? extends PostgreTableColumn> tableColumnsStream = Optional.ofNullable(this.getAttributes(monitor))
                .orElse(Collections.emptyList())
                .stream();

        if(this.isPersisted()) {
            tableColumns = tableColumnsStream
                    .filter(field -> field.getOrdinalPosition() >= 0)
                    .collect(Collectors.toList());
        } else {
            final int TEMPORARY_COLUMN_ORDINAL_POSITION = -1;
            tableColumns = tableColumnsStream
                    .filter(field -> field.getOrdinalPosition() == TEMPORARY_COLUMN_ORDINAL_POSITION)
                    .collect(Collectors.toList());
        }
        return tableColumns;
    }

    private CharSequence addDatabaseQualifier() {
        StringBuilder databaseQualifier = new StringBuilder().append(this.getDatabase().getName())
                .append(".")
                .append(this.getSchema().getName())
                .append(".");

        return this.isTemporaryTable() ? "" : databaseQualifier;
    }

    private boolean tableHasCommand() {
        return (this.getCommand() != null && !this.getCommand().isEmpty());
    }

    private boolean isWebTable() {
        return (this.uriLocationsHandler.stream().anyMatch(location -> location.startsWith("http"))
                || tableHasCommand());
    }

    private String generateFormatOptions(FormatType formatType, String formatOptions) {
        if (formatType == null || formatOptions.isEmpty()){
            return "";
        }

        if (formatType.equals(FormatType.b)) {
            String[] formatSpecTokens = formatOptions.split(" ");
            String formatterSpec = formatSpecTokens.length >= 2 ? formatSpecTokens[1] : "";
            return " ( FORMATTER=" + formatterSpec + " )";
        }
        return " ( " + formatOptions + " )";
    }

    private String determineExecutionLocation() {
        if (this.getExecLocation() != null && this.getExecLocation().equalsIgnoreCase("MASTER_ONLY")) {
            return "ON MASTER";
        }

        return "ON ALL";
    }

    public static class ExternalTableFormatTypeProvider implements IPropertyValueListProvider<GreenplumExternalTable> {
        public boolean allowCustomValue() {
            return false;
        }

        public Object[] getPossibleValues(GreenplumExternalTable object) {
            Predicate<FormatType> excludeCustomTypes = formatType -> !formatType.equals(FormatType.b);

            return Arrays.stream(GreenplumExternalTable.FormatType.values())
                    .filter(excludeCustomTypes)
                    .map(GreenplumExternalTable.FormatType::getValue).toArray();
        }
    }

    public static class GreenplumCharacterSetProvider implements IPropertyValueListProvider<GreenplumExternalTable> {
        public boolean allowCustomValue() {
            return false;
        }

        public Object[] getPossibleValues(GreenplumExternalTable object) {
            return Arrays.stream(GreenplumCharacterSet.values())
                    .map(GreenplumCharacterSet::getCharacterSetValue).toArray();
        }
    }

    public static class ExternalTableUriLocationsRenderer
            implements IPropertyValueTransformer<GreenplumExternalTable, String> {
        @Override
        public String transform(GreenplumExternalTable greenplumExternalTable,
                                String commaSeparatedListUriLocations) throws IllegalArgumentException {
            return greenplumExternalTable.uriLocationsHandler.getLineFeedSeparatedList();
        }
    }
}


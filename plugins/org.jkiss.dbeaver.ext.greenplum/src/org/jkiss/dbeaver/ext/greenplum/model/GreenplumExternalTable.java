/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
import org.jkiss.dbeaver.ext.postgresql.model.PostgreTableColumn;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreTableRegular;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.CommonUtils;

import java.sql.ResultSet;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class GreenplumExternalTable extends PostgreTableRegular {
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
    }

    public enum RejectLimitType {
        r("ROWS");

        private String rejectLimitType;

        RejectLimitType(String rejectLimitTypeString) {
            this.rejectLimitType = rejectLimitTypeString;
        }

        public String getValue() {
            return rejectLimitType;
        }
    }

    private final List<String> uriLocations;
    private final String execLocation;
    private final FormatType formatType;
    private final String formatOptions;
    private final String encoding;
    private final RejectLimitType rejectLimitType;
    private final int rejectLimit;
    private final boolean writable;
    private final boolean temporaryTable;
    private final boolean loggingErrors;

    public GreenplumExternalTable(PostgreSchema catalog, ResultSet dbResult) {
        super(catalog, dbResult);
        String uriLocations = JDBCUtils.safeGetStringTrimmed(dbResult, "urilocation");
        this.uriLocations = !CommonUtils.isEmpty(uriLocations) ?
                Arrays.asList(uriLocations.split(",")) : Collections.emptyList();
        this.execLocation = JDBCUtils.safeGetString(dbResult, "execlocation");
        this.formatType = FormatType.valueOf(JDBCUtils.safeGetString(dbResult, "fmttype"));
        this.formatOptions = JDBCUtils.safeGetString(dbResult, "fmtopts");
        this.encoding = JDBCUtils.safeGetString(dbResult, "encoding");

        this.rejectLimit = JDBCUtils.safeGetInt(dbResult, "rejectlimit");
        String rejectlimittype = JDBCUtils.safeGetString(dbResult, "rejectlimittype");
        this.writable = JDBCUtils.safeGetBoolean(dbResult, "writable");
        this.temporaryTable = JDBCUtils.safeGetBoolean(dbResult, "is_temp_table");
        this.loggingErrors = JDBCUtils.safeGetBoolean(dbResult, "is_logging_errors");
        if (rejectlimittype != null && rejectlimittype.length() > 0) {
            this.rejectLimitType = RejectLimitType.valueOf(rejectlimittype);
        } else {
            this.rejectLimitType = null;
        }

    }

    public List<String> getUriLocations() {
        return uriLocations;
    }

    public String getExecLocation() {
        return execLocation;
    }

    public FormatType getFormatType() {
        return formatType;
    }

    public String getFormatOptions() {
        return formatOptions;
    }

    public String getEncoding() {
        return encoding;
    }

    public RejectLimitType getRejectLimitType() {
        return rejectLimitType;
    }

    public int getRejectLimit() {
        return rejectLimit;
    }

    public boolean isWritable() {
        return this.writable;
    }

    public boolean isTemporaryTable() {
        return temporaryTable;
    }

    public boolean isLoggingErrors() {
        return loggingErrors;
    }

    public String generateDDL(DBRProgressMonitor monitor) throws DBException {
        StringBuilder ddlBuilder = new StringBuilder();
        ddlBuilder.append("CREATE ")
                .append(this.isWritable() ? "WRITABLE " : "")
                .append("EXTERNAL ")
                .append(webUriLocationExists() ? "WEB " : "")
                .append(this.isTemporaryTable() ? "TEMPORARY " : "")
                .append("TABLE ")
                .append(addDatabaseQualifier())
                .append(this.getName())
                .append(" (\n");

        List<PostgreTableColumn> tableColumns = this.getAttributes(monitor)
                .stream()
                .filter(field -> field.getOrdinalPosition() >= 0)
                .collect(Collectors.toList());

        if (tableColumns.size() == 1) {
            PostgreTableColumn column = tableColumns.get(0);
            ddlBuilder.append("\t" + column.getName() + " " + column.getTypeName() + "\n)\n");
        } else {
            ddlBuilder.append(tableColumns
                    .stream()
                    .map(field -> "\t" + field.getName() + " " + field.getTypeName())
                    .collect(Collectors.joining(",\n")));
            ddlBuilder.append("\n)\n");
        }

        if (this.getUriLocations() != null && !this.getUriLocations().isEmpty()) {
            ddlBuilder.append("LOCATION (\n");

            ddlBuilder.append(this.getUriLocations()
                    .stream()
                    .map(location -> "\t'" + location + "'")
                    .collect(Collectors.joining(",\n")));

            ddlBuilder.append("\n) " + determineExecutionLocation() + "\n");
        }

        ddlBuilder.append("FORMAT '" + this.getFormatType().getValue() + "' ( "
                + generateFormatOptions(this.getFormatType(), this.getFormatOptions()) + " )");

        if (this.getEncoding() != null && this.getEncoding().length() > 0) {
            ddlBuilder.append("\nENCODING '" + this.getEncoding() + "'");
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

    private CharSequence addDatabaseQualifier() {
        StringBuilder databaseQualifier = new StringBuilder().append(this.getDatabase().getName())
                .append(".")
                .append(this.getSchema().getName())
                .append(".");

        return this.isTemporaryTable() ? "" : databaseQualifier;
    }

    private boolean webUriLocationExists() {
        return this.uriLocations.stream().anyMatch(location -> location.startsWith("http"));
    }

    private String generateFormatOptions(FormatType formatType, String formatOptions) {
        if (formatType.equals(FormatType.b)) {
            String[] formatSpecTokens = formatOptions.split(" ");
            String formatterSpec = formatSpecTokens.length >= 2 ? formatSpecTokens[1] : "";
            return "FORMATTER=" + formatterSpec;
        }
        return formatOptions;
    }

    private String determineExecutionLocation() {
        if (this.getExecLocation() != null && this.getExecLocation().equalsIgnoreCase("MASTER_ONLY")) {
            return "ON MASTER";
        }

        return "ON ALL";
    }
}

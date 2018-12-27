package org.jkiss.dbeaver.ext.greenplum.model;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreSchema;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreTableColumn;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreTableRegular;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.sql.ResultSet;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static java.lang.String.format;

public class GreenplumExternalTable extends PostgreTableRegular {
    public enum FormatType {
        c("CSV"),
        t("TEXT");

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

    public GreenplumExternalTable(PostgreSchema catalog, ResultSet dbResult) {
        super(catalog, dbResult);
        this.uriLocations = Arrays.asList(JDBCUtils.safeGetString(dbResult, "urilocation")
                .trim().split(","));
        this.execLocation = JDBCUtils.safeGetString(dbResult, "execlocation");
        this.formatType = FormatType.valueOf(JDBCUtils.safeGetString(dbResult, "fmttype"));
        this.formatOptions = JDBCUtils.safeGetString(dbResult, "fmtopts");
        this.encoding = JDBCUtils.safeGetString(dbResult, "encoding");

        this.rejectLimit = JDBCUtils.safeGetInt(dbResult, "rejectlimit");
        String rejectlimittype = JDBCUtils.safeGetString(dbResult, "rejectlimittype");
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

    public String generateDDL(DBRProgressMonitor monitor) throws DBException {
        StringBuilder ddlBuilder = new StringBuilder(format("CREATE EXTERNAL TABLE %s.%s.%s (\n",
                this.getDatabase().getName(),
                this.getSchema().getName(),
                this.getName()));

        List<PostgreTableColumn> tableColumns = this.getAttributes(monitor)
                .stream()
                .filter(field -> field.getOrdinalPosition() >= 0)
                .collect(Collectors.toList());

        if (tableColumns.size() == 1) {
            PostgreTableColumn column = tableColumns.get(0);
            ddlBuilder.append(format("\t%s %s\n)\n", column.getName(), column.getTypeName()));
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
                    .map(location -> "\t'"+location+"'")
                    .collect(Collectors.joining(",\n")));

            ddlBuilder.append(format("\n) %s\n", determineExecutionLocation()));
        }

        ddlBuilder.append(format("FORMAT '%s' ( %s )", this.getFormatType().getValue(), this.getFormatOptions()));

        if (this.getEncoding() != null && this.getEncoding().length() > 0) {
            ddlBuilder.append(format("\nENCODING '%s'", this.getEncoding()));
        }

        if (this.getRejectLimit() > 0 && this.getRejectLimitType() != null) {
            ddlBuilder.append(format("\nSEGMENT REJECT LIMIT %d %s", this.getRejectLimit(), this.getRejectLimitType().getValue()));
        }

        return ddlBuilder.toString();
    }

    private String determineExecutionLocation() {
        if (this.getExecLocation() != null && this.getExecLocation().equalsIgnoreCase("MASTER_ONLY")) {
            return "ON MASTER";
        }

        return "ON ALL";
    }
}

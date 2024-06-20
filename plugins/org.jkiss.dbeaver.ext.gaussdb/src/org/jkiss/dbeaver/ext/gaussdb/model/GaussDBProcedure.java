package org.jkiss.dbeaver.ext.gaussdb.model;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.postgresql.PostgreUtils;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDataType;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreProcedure;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreSchema;
import org.jkiss.dbeaver.model.DBPScriptObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.utils.CommonUtils;

public class GaussDBProcedure extends PostgreProcedure {

    private long proPackageId;

    public String proKind;

    public String proSrc;

    public String body = getBody();

    public long getProPackageId() {
        return proPackageId;
    }

    public GaussDBProcedure(PostgreSchema schema) {
        super(schema);
    }

    public GaussDBProcedure(DBRProgressMonitor monitor, PostgreSchema schema, ResultSet dbResult) {
        super(monitor, schema, dbResult);
        this.proPackageId = JDBCUtils.safeGetLong(dbResult, "propackageid");
        this.proSrc = JDBCUtils.safeGetString(dbResult, "prosrc");
    }

    @Override
    @Property(hidden = true, editable = true, updatable = true, order = -1)
    public String getObjectDefinitionText(DBRProgressMonitor monitor, Map<String, Object> options) throws DBException {
        boolean omitHeader = CommonUtils.getOption(options, OPTION_DEBUGGER_SOURCE);
        String procDDL = omitHeader ? "" : "-- DROP " + getProcedureTypeName() + " " + getFullQualifiedSignature() + ";\n\n";
        if (isPersisted() && (!getDataSource().getServerType().supportsFunctionDefRead() || omitHeader) && !isAggregate()) {
            if (proSrc == null) {
                try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Read procedure body")) {
                    proSrc = JDBCUtils.queryString(session, "SELECT prosrc FROM pg_proc where oid = ?", getObjectId());
                } catch (SQLException e) {
                    throw new DBException("Error reading procedure body", e);
                }
            }
            PostgreDataType returnType = getReturnType();
            String returnTypeName = returnType == null ? null : returnType.getFullTypeName();
            procDDL += omitHeader ? proSrc : generateFunctionDeclaration(getLanguage(monitor), returnTypeName, proSrc);
        } else {
            if (body == null) {
                if (!isPersisted()) {
                    PostgreDataType returnType = getReturnType();
                    String returnTypeName = returnType == null ? null : returnType.getFullTypeName();
                    body = generateFunctionDeclaration(getLanguage(monitor), returnTypeName, "\n\t-- Enter function body here\n");
                } else if (getObjectId() == 0) {
                    // No OID so let's use old (bad) way
                    body = this.proSrc;
                } else {
                    try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Read procedure body")) {
                        body = JDBCUtils.queryString(session, "SELECT pg_get_functiondef(" + getObjectId() + ")");
                        body = body == null ? this.proSrc : body.substring(4, body.length() - 2);
                    } catch (SQLException e) {
                        throw new DBException("Error reading procedure body", e);
                    }
                }
            }
            procDDL += body;
        }
        if (this.isPersisted() && !omitHeader) {
            procDDL += ";\n";

            if (CommonUtils.getOption(options, DBPScriptObject.OPTION_INCLUDE_COMMENTS) && !CommonUtils.isEmpty(getDescription())) {
                procDDL += "\nCOMMENT ON " + getProcedureTypeName() + " " + getFullQualifiedSignature() + " IS "
                            + SQLUtils.quoteString(this, getDescription()) + ";\n";
            }

            if (CommonUtils.getOption(options, DBPScriptObject.OPTION_INCLUDE_PERMISSIONS)) {
                List<DBEPersistAction> actions = new ArrayList<>();
                PostgreUtils.getObjectGrantPermissionActions(monitor, this, actions, options);
                procDDL += "\n" + SQLUtils.generateScript(getDataSource(), actions.toArray(new DBEPersistAction[0]), false);
            }
        }

        return procDDL;
    }

}
/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.utils.CommonUtils;

public class GaussDBProcedure extends PostgreProcedure {
    public long propackageid;
    public String prokind;
    public String procSrc;

    public String body = getBody();

    public long getPropackageid() {
        return propackageid;
    }

    public GaussDBProcedure(PostgreSchema schema) {
        super(schema);
    }

    public GaussDBProcedure(DBRProgressMonitor monitor, PostgreSchema schema, ResultSet dbResult) {
        super(monitor, schema, dbResult);
        this.propackageid = JDBCUtils.safeGetLong(dbResult, "propackageid");
        this.procSrc = JDBCUtils.safeGetString(dbResult, "prosrc");
    }

    @Override
    @Property(hidden = true, editable = true, updatable = true, order = -1)
    public String getObjectDefinitionText(DBRProgressMonitor monitor, Map<String, Object> options) throws DBException {
        boolean omitHeader = CommonUtils.getOption(options, OPTION_DEBUGGER_SOURCE);
        String procDDL = omitHeader ? "" : "-- DROP " + getProcedureTypeName() + " " + getFullQualifiedSignature() + ";\n\n";
        if (isPersisted() && (!getDataSource().getServerType().supportsFunctionDefRead() || omitHeader) && !isAggregate()) {
            procDDL = getObjectDefinitionTextWhenPersisted(monitor, omitHeader, procDDL);
        } else {
            procDDL = getObjectDefinitionTextWhenBodyNull(monitor, procDDL);
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

    private String getObjectDefinitionTextWhenPersisted(DBRProgressMonitor monitor, boolean omitHeader,
        String procDDL) throws DBCException, DBException {
        if (procSrc == null) {
            try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Read procedure body")) {
                procSrc = JDBCUtils.queryString(session, "SELECT prosrc FROM pg_proc where oid = ?", getObjectId());
            } catch (SQLException e) {
                throw new DBException("Error reading procedure body", e);
            }
        }
        PostgreDataType returnType = getReturnType();
        String returnTypeName = returnType == null ? null : returnType.getFullTypeName();
        return (procDDL + (omitHeader ? procSrc : generateFunctionDeclaration(getLanguage(monitor), returnTypeName, procSrc)));
    }

    private String getObjectDefinitionTextWhenBodyNull(DBRProgressMonitor monitor, String procDDL) throws DBException, DBCException {
        if (body == null) {
            if (!isPersisted()) {
                PostgreDataType returnType = getReturnType();
                String returnTypeName = returnType == null ? null : returnType.getFullTypeName();
                body = generateFunctionDeclaration(getLanguage(monitor), returnTypeName, "\n\t-- Enter function body here\n");
            } else if (getObjectId() == 0) {
                // No OID so let's use old (bad) way
                body = this.procSrc;
            } else {
                try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Read procedure body")) {
                    String res = JDBCUtils.queryString(session, "SELECT pg_get_functiondef(" + getObjectId() + ")");
                    body = res == null ? this.procSrc : res.substring(4, res.length() - 2);
                } catch (SQLException e) {
                    throw new DBException("Error reading procedure body", e);
                }
            }
        }

        return (procDDL + body);
    }

}

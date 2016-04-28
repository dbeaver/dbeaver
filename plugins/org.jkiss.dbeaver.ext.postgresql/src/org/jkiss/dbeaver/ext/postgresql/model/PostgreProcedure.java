/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.ext.postgresql.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.postgresql.PostgreUtils;
import org.jkiss.dbeaver.model.DBPOverloadedObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.struct.AbstractProcedure;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObjectUnique;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureParameterKind;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureType;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * PostgreProcedure
 */
public class PostgreProcedure extends AbstractProcedure<PostgreDataSource, PostgreSchema> implements PostgreObject, PostgreScriptObject, DBSObjectUnique, DBPOverloadedObject
{
    private static final Log log = Log.getLog(PostgreProcedure.class);
    private static final String CAT_FLAGS = "Flags";
    private static final String CAT_PROPS = "Properties";
    private static final String CAT_STATS = "Statistics";

    public enum ProcedureVolatile {
        i,
        s,
        v,
    }

    public enum ArgumentMode {
        i(DBSProcedureParameterKind.IN),
        o(DBSProcedureParameterKind.OUT),
        b(DBSProcedureParameterKind.INOUT),
        v(DBSProcedureParameterKind.RESULTSET),
        t(DBSProcedureParameterKind.TABLE),
        u(DBSProcedureParameterKind.UNKNOWN);

        private final DBSProcedureParameterKind parameterKind;
        ArgumentMode(DBSProcedureParameterKind parameterKind) {
            this.parameterKind = parameterKind;
        }

        public DBSProcedureParameterKind getParameterKind() {
            return parameterKind;
        }
    }

    private long oid;
    private String procSrc;
    private String body;
    private long ownerId;
    private long languageId;
    private float execCost;
    private float estRows;
    private PostgreDataType varArrayType;
    private String procTransform;
    private boolean isAggregate;
    private boolean isWindow;
    private boolean isSecurityDefiner;
    private boolean leakproof;
    private boolean isStrict;
    private boolean returnsSet;
    private ProcedureVolatile procVolatile;
    private PostgreDataType returnType;
    private Object[] argDefaults;
    private int[] transformTypes;
    private String[] config;

    private String overloadedName;
    private List<PostgreProcedureParameter> params = new ArrayList<>();

    public PostgreProcedure(PostgreSchema schema) {
        super(schema, false);
    }

    public PostgreProcedure(
        PostgreSchema schema,
        ResultSet dbResult)
    {
        super(schema, true);
        loadInfo(dbResult);
    }

    private void loadInfo(ResultSet dbResult) {
        this.oid = JDBCUtils.safeGetLong(dbResult, "oid");
        setName(JDBCUtils.safeGetString(dbResult, "proname"));
        this.ownerId = JDBCUtils.safeGetLong(dbResult, "proowner");
        this.languageId = JDBCUtils.safeGetLong(dbResult, "prolang");
        this.execCost = JDBCUtils.safeGetFloat(dbResult, "procost");
        this.estRows = JDBCUtils.safeGetFloat(dbResult, "prorows");

        Long[] allArgTypes = JDBCUtils.safeGetArray(dbResult, "proallargtypes");
        if (!ArrayUtils.isEmpty(allArgTypes)) {
            String[] argNames = JDBCUtils.safeGetArray(dbResult, "proargnames");
            String[] argModes = JDBCUtils.safeGetArray(dbResult, "proargmodes");

            for (int i = 0; i < allArgTypes.length; i++) {
                Long paramType = allArgTypes[i];
                final PostgreDataType dataType = container.getDatabase().getDataType(paramType.intValue());
                if (dataType == null) {
                    log.warn("Parameter data type [" + paramType + "] not found");
                    continue;
                }
                //String paramName = argNames == null || argNames.length < inArg
                String paramName = argNames == null || argNames.length < allArgTypes.length ? "$" + (i + 1) : argNames[i];
                ArgumentMode mode = ArgumentMode.i;
                if (argModes != null && argModes.length == allArgTypes.length) {
                    try {
                        mode = ArgumentMode.valueOf(argModes[i]);
                    } catch (IllegalArgumentException e) {
                        log.debug(e);
                    }
                }
                PostgreProcedureParameter param = new PostgreProcedureParameter(
                    this,
                    paramName,
                    dataType,
                    mode == null ? DBSProcedureParameterKind.IN : mode.getParameterKind(),
                    i + 1);
                params.add(param);
            }

        } else {
            long[] inArgTypes = PostgreUtils.getIdVector(JDBCUtils.safeGetObject(dbResult, "proargtypes"));
            if (!ArrayUtils.isEmpty(inArgTypes)) {
                for (int i = 0; i < inArgTypes.length; i++) {
                    Long paramType = inArgTypes[i];
                    final PostgreDataType dataType = container.getDatabase().getDataType(paramType.intValue());
                    if (dataType == null) {
                        log.warn("Parameter data type [" + paramType + "] not found");
                        continue;
                    }
                    //String paramName = argNames == null || argNames.length < inArg
                    String paramName = "$" + (i + 1);
                    PostgreProcedureParameter param = new PostgreProcedureParameter(this, paramName, dataType, DBSProcedureParameterKind.IN, i + 1);
                    params.add(param);
                }
            }
        }

        this.overloadedName = makeOverloadedName(false);

        {
            final long varTypeId = JDBCUtils.safeGetLong(dbResult, "provariadic");
            if (varTypeId != 0) {
                varArrayType = container.getDatabase().getDataType(varTypeId);
            }
        }
        this.procTransform = JDBCUtils.safeGetString(dbResult, "protransform");
        this.isAggregate = JDBCUtils.safeGetBoolean(dbResult, "proisagg");
        this.isWindow = JDBCUtils.safeGetBoolean(dbResult, "proiswindow");
        this.isSecurityDefiner = JDBCUtils.safeGetBoolean(dbResult, "prosecdef");
        this.leakproof = JDBCUtils.safeGetBoolean(dbResult, "proleakproof");
        this.isStrict = JDBCUtils.safeGetBoolean(dbResult, "proisstrict");
        this.returnsSet = JDBCUtils.safeGetBoolean(dbResult, "proretset");
        try {
            this.procVolatile = ProcedureVolatile.valueOf(JDBCUtils.safeGetString(dbResult, "provolatile"));
        } catch (IllegalArgumentException e) {
            log.debug(e);
        }
        {
            final long retTypeId = JDBCUtils.safeGetLong(dbResult, "prorettype");
            if (retTypeId != 0) {
                returnType = container.getDatabase().getDataType(retTypeId);
            }
        }
        this.procSrc = JDBCUtils.safeGetString(dbResult, "prosrc");
    }

    @NotNull
    @Override
    public PostgreDatabase getDatabase() {
        return container.getDatabase();
    }

    @Override
    public long getObjectId() {
        return oid;
    }

    @Override
    public DBSProcedureType getProcedureType()
    {
        return DBSProcedureType.PROCEDURE;
    }

    @Property(hidden = true, editable = true, updatable = true, order = -1)
    public String getBody()
    {
        return body;
    }

    @Override
    public Collection<PostgreProcedureParameter> getParameters(DBRProgressMonitor monitor)
        throws DBException
    {
        return params;
    }

    @NotNull
    @Override
    public String getFullQualifiedName()
    {
        return DBUtils.getFullQualifiedName(getDataSource(),
            getContainer(),
            this);
    }

    @NotNull
    @Override
    public String getOverloadedName() {
        return overloadedName;
    }

    @NotNull
    @Override
    public String getUniqueName() {
        return overloadedName;
    }

    @Override
    @Property(hidden = true, editable = true, updatable = true, order = -1)
    public String getObjectDefinitionText(DBRProgressMonitor monitor) throws DBException
    {
        if (body == null) {
            if (oid == 0) {
                // No OID so let's use old (bad) way
                body = this.procSrc;
            } else {
                try (JDBCSession session = DBUtils.openMetaSession(monitor, getDataSource(), "Read procedure body")) {
                    body = JDBCUtils.queryString(session, "SELECT pg_get_functiondef(" + getObjectId() + ")");
                } catch (SQLException e) {
                    throw new DBException("Error reading procedure body", e);
                }
            }
        }
        return body;
    }

    @Override
    public void setObjectDefinitionText(String sourceText) throws DBException
    {
        body = sourceText;
    }

    @Property(category = CAT_PROPS, order = 10)
    public PostgreAuthId getOwner(DBRProgressMonitor monitor) throws DBException {
        return PostgreUtils.getObjectById(monitor, container.getDatabase().authIdCache, container.getDatabase(), ownerId);
    }

    @Property(category = CAT_PROPS, viewable = true, order = 11)
    public PostgreLanguage getLanguage(DBRProgressMonitor monitor) throws DBException {
        return PostgreUtils.getObjectById(monitor, container.getDatabase().languageCache, container.getDatabase(), languageId);
    }

    @Property(category = CAT_PROPS, viewable = true, order = 12)
    public PostgreDataType getReturnType() {
        return returnType;
    }

    @Property(category = CAT_PROPS, viewable = false, order = 13)
    public PostgreDataType getVarArrayType() {
        return varArrayType;
    }

    @Property(category = CAT_PROPS, viewable = false, order = 14)
    public String getProcTransform() {
        return procTransform;
    }

    @Property(category = CAT_STATS, viewable = false, order = 30)
    public float getExecCost() {
        return execCost;
    }

    @Property(category = CAT_STATS, viewable = false, order = 31)
    public float getEstRows() {
        return estRows;
    }

    @Property(category = CAT_FLAGS, viewable = true, order = 100)
    public boolean isAggregate() {
        return isAggregate;
    }

    @Property(category = CAT_FLAGS, viewable = true, order = 101)
    public boolean isWindow() {
        return isWindow;
    }

    @Property(category = CAT_FLAGS, viewable = true, order = 102)
    public boolean isSecurityDefiner() {
        return isSecurityDefiner;
    }

    @Property(category = CAT_FLAGS, viewable = true, order = 103)
    public boolean isLeakproof() {
        return leakproof;
    }

    @Property(category = CAT_FLAGS, viewable = true, order = 104)
    public boolean isStrict() {
        return isStrict;
    }

    @Property(category = CAT_FLAGS, viewable = true, order = 105)
    public boolean isReturnsSet() {
        return returnsSet;
    }

    @Property(category = CAT_FLAGS, viewable = true, order = 106)
    public ProcedureVolatile getProcVolatile() {
        return procVolatile;
    }

    private String makeOverloadedName(boolean quote) {
        String selfName = quote ? DBUtils.getQuotedIdentifier(this) : name;
        if (!CommonUtils.isEmpty(params)) {
            StringBuilder paramsSignature = new StringBuilder(64);
            paramsSignature.append("(");
            for (int i = 0; i < params.size(); i++) {
                if (i > 0) paramsSignature.append(',');
                final PostgreDataType dataType = params.get(i).getParameterType();
                paramsSignature.append(dataType.getName());
            }
            paramsSignature.append(")");
            return selfName + paramsSignature.toString();
        } else {
            return selfName + "()";
        }
    }

    public String getFullQualifiedSignature() {
        return DBUtils.getQuotedIdentifier(getContainer()) + "." + makeOverloadedName(true);
    }

    @Override
    public String toString() {
        return overloadedName;
    }
}

/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
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
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.struct.AbstractProcedure;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObjectUnique;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureType;
import org.jkiss.utils.ArrayUtils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;

/**
 * PostgreProcedure
 */
public class PostgreProcedure extends AbstractProcedure<PostgreDataSource, PostgreSchema> implements PostgreObject, PostgreScriptObject, DBSObjectUnique, DBPOverloadedObject
{
    static final Log log = Log.getLog(PostgreProcedure.class);

    enum ProcedureVolatile {
        i,
        s,
        v,
    }

    private int oid;
    private String body;
    private int ownerId;
    private int languageId;
    private float execCost;
    private float estRows;
    private int varArrayType;
    private String procTransform;
    private boolean isAggregate;
    private boolean isWindow;
    private boolean isSecurityDefiner;
    private boolean leakproof;
    private boolean isStrict;
    private boolean returnsSet;
    private ProcedureVolatile procVolatile;
    private int argCount;
    private int argDefCount;
    private int returnType;
    private Long[] inArgTypes;
    private Long[] allArgTypes;
    private char[] argModes;
    private String[] argNames;
    private Object[] argDefaults;
    private int[] transformTypes;
    private String[] config;

    private String overloadedName;

    public PostgreProcedure(
        PostgreSchema catalog,
        ResultSet dbResult)
    {
        super(catalog, true);
        loadInfo(dbResult);
    }

    private void loadInfo(ResultSet dbResult)
    {
        this.oid = JDBCUtils.safeGetInt(dbResult, "oid");
        setName(JDBCUtils.safeGetString(dbResult, "proname"));
        this.ownerId = JDBCUtils.safeGetInt(dbResult, "proowner");
        this.languageId = JDBCUtils.safeGetInt(dbResult, "prolang");

        this.inArgTypes = PostgreUtils.getIdVector(JDBCUtils.safeGetObject(dbResult, "proargtypes"));
        this.allArgTypes = JDBCUtils.safeGetArray(dbResult, "proallargtypes");
        this.argNames = JDBCUtils.safeGetArray(dbResult, "proargnames");

        if (!ArrayUtils.isEmpty(inArgTypes)) {
            StringBuilder params = new StringBuilder(64);
            params.append("(");
            for (int i = 0; i < inArgTypes.length; i++) {
                if (i > 0) params.append(',');
                Long paramType = inArgTypes[i];
                final PostgreDataType dataType = container.getDatabase().dataTypeCache.getDataType(paramType.intValue());
                if (dataType == null) {
                    log.warn("Parameter data type [" + paramType + "] not found");
                } else {
                    params.append(dataType.getName());
                }
            }
            params.append(")");
            this.overloadedName = this.name + params.toString();
        } else {
            this.overloadedName = this.name;
        }
    }

    @NotNull
    @Override
    public PostgreDatabase getDatabase() {
        return container.getDatabase();
    }

    @Override
    public int getObjectId() {
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
        return null;
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
            try (JDBCSession session = DBUtils.openMetaSession(monitor, getDataSource(), "Read procedure body")) {
                body = JDBCUtils.queryString(session, "SELECT pg_get_functiondef(" + getObjectId() + ")");
            } catch (SQLException e) {
                throw new DBException("Error reading procedure body", e);
            }
        }
        return body;
    }

    @Override
    public void setObjectDefinitionText(String sourceText) throws DBException
    {
        body = sourceText;
    }

    @Property(order = 10)
    public PostgreAuthId getOwner(DBRProgressMonitor monitor) throws DBException {
        return PostgreUtils.getObjectById(monitor, container.getDatabase().authIdCache, container.getDatabase(), ownerId);
    }

    @Property(viewable = true, order = 11)
    public PostgreLanguage getLanguage(DBRProgressMonitor monitor) throws DBException {
        return PostgreUtils.getObjectById(monitor, container.getDatabase().languageCache, container.getDatabase(), languageId);
    }

    @Override
    public String toString() {
        return getUniqueName();
    }
}

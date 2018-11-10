/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ext.mssql.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.mssql.SQLServerUtils;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPScriptObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.DBSObjectCache;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectCache;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.utils.CommonUtils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * SQLServerView
 */
public class SQLServerView extends SQLServerTableBase implements DBPScriptObject
{
    private static final Log log = Log.getLog(SQLServerView.class);

    private String ddl;

    public SQLServerView(SQLServerSchema schema)
    {
        super(schema);
    }

    // Copy constructor
    public SQLServerView(DBRProgressMonitor monitor, SQLServerSchema schema, DBSEntity source) throws DBException {
        super(monitor, schema, source);
    }

    public SQLServerView(
        SQLServerSchema catalog,
        ResultSet dbResult)
    {
        super(catalog, dbResult);
    }

    @Override
    public boolean isView()
    {
        return true;
    }

    @Override
    public Collection<SQLServerTableColumn> getAttributes(@NotNull DBRProgressMonitor monitor)
        throws DBException
    {
        List<SQLServerTableColumn> childColumns = getContainer().getTableCache().getChildren(monitor, getContainer(), this);
        if (childColumns == null) {
            return Collections.emptyList();
        }
        List<SQLServerTableColumn> columns = new ArrayList<>(childColumns);
        columns.sort(DBUtils.orderComparator());
        return columns;
    }

    @Override
    public SQLServerTableColumn getAttribute(@NotNull DBRProgressMonitor monitor, @NotNull String attributeName)
        throws DBException
    {
        return getContainer().getTableCache().getChild(monitor, getContainer(), this, attributeName);
    }

    public SQLServerTableColumn getAttribute(@NotNull DBRProgressMonitor monitor, @NotNull long columnId)
        throws DBException
    {
        for (SQLServerTableColumn col : getAttributes(monitor)) {
            if (col.getObjectId() == columnId) {
                return col;
            }
        }
        log.error("Column '" + columnId + "' not found in table '" + getFullyQualifiedName(DBPEvaluationContext.DML) + "'");
        return null;
    }

    @Override
    @Association
    public synchronized Collection<SQLServerTableIndex> getIndexes(DBRProgressMonitor monitor)
        throws DBException
    {
        return null;
    }

    @Nullable
    @Override
    @Association
    public synchronized Collection<SQLServerTableUniqueKey> getConstraints(@NotNull DBRProgressMonitor monitor)
        throws DBException
    {
        return null;
    }

    @Override
    @Association
    public Collection<SQLServerTableForeignKey> getReferences(@NotNull DBRProgressMonitor monitor)
        throws DBException
    {
        return null;
    }

    @Override
    public synchronized Collection<SQLServerTableForeignKey> getAssociations(@NotNull DBRProgressMonitor monitor)
        throws DBException
    {
        return null;
    }

    @Override
    public String getObjectDefinitionText(DBRProgressMonitor monitor, Map<String, Object> options) throws DBException {
        if (CommonUtils.getOption(options, DBPScriptObject.OPTION_REFRESH)) {
            ddl = null;
        }
        if (ddl == null) {
            ddl = SQLServerUtils.extractSource(monitor, getSchema(), getName());
        }
        return ddl;
    }

    @Nullable
    @Override
    public String getDescription()
    {
        return null;/*additionalInfo.description;*/
    }

    @Override
    public DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException {

        return getContainer().getTableCache().refreshObject(monitor, getContainer(), this);
    }

}

/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2013-2015 Denis Forveille (titou10.titou10@gmail.com)
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
package org.jkiss.dbeaver.ext.db2.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.db2.DB2Constants;
import org.jkiss.dbeaver.ext.db2.editors.DB2SourceObject;
import org.jkiss.dbeaver.ext.db2.model.dict.DB2OwnerType;
import org.jkiss.dbeaver.ext.db2.model.dict.DB2TableCheckConstraintType;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCTableConstraint;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.DBSEntityAttributeRef;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;
import org.jkiss.dbeaver.model.struct.DBSObjectState;
import org.jkiss.utils.CommonUtils;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.List;

/**
 * DB2 Table Check Constraint
 * 
 * @author Denis Forveille
 */
public class DB2TableCheckConstraint extends JDBCTableConstraint<DB2Table> implements DB2SourceObject {

    private String owner;
    private DB2OwnerType ownerType;
    private Timestamp createTime;
    private String qualifier;
    private DB2TableCheckConstraintType type;
    private String fumcPath;
    private String text;
    private Integer precentValid;
    private String collationSchema;
    private String collationName;
    private String collationSchemaOrderBy;
    private String collationNameOrderBy;

    private List<DB2TableCheckConstraintColumn> columns;

    // -----------------
    // Constructor
    // -----------------

    public DB2TableCheckConstraint(DBRProgressMonitor monitor, DB2Table table, ResultSet dbResult) throws DBException
    {
        super(table, JDBCUtils.safeGetString(dbResult, "CONSTNAME"), null, DBSEntityConstraintType.CHECK, true);

        DB2DataSource db2DataSource = table.getDataSource();

        this.owner = JDBCUtils.safeGetString(dbResult, "OWNER");
        this.createTime = JDBCUtils.safeGetTimestamp(dbResult, "CREATE_TIME");
        this.qualifier = JDBCUtils.safeGetString(dbResult, "QUALIFIER");
        this.type = CommonUtils.valueOf(DB2TableCheckConstraintType.class, JDBCUtils.safeGetString(dbResult, "TYPE"));
        this.fumcPath = JDBCUtils.safeGetString(dbResult, "FUNC_PATH");
        this.text = JDBCUtils.safeGetString(dbResult, "TEXT");
        this.precentValid = JDBCUtils.safeGetInteger(dbResult, "PERCENTVALID");
        this.collationSchema = JDBCUtils.safeGetStringTrimmed(dbResult, "COLLATIONSCHEMA");
        this.collationName = JDBCUtils.safeGetString(dbResult, "COLLATIONNAME");
        this.collationSchemaOrderBy = JDBCUtils.safeGetString(dbResult, "COLLATIONSCHEMA_ORDERBY");
        this.collationNameOrderBy = JDBCUtils.safeGetString(dbResult, "COLLATIONNAME_ORDERBY");

        if (db2DataSource.isAtLeastV9_5()) {
            this.ownerType = CommonUtils.valueOf(DB2OwnerType.class, JDBCUtils.safeGetString(dbResult, "OWNERTYPE"));
        }

    }

    @NotNull
    @Override
    public String getFullyQualifiedName(DBPEvaluationContext context)
    {
        return DBUtils.getFullQualifiedName(getDataSource(), getTable().getContainer(), getTable(), this);
    }

    @NotNull
    @Override
    public DB2DataSource getDataSource()
    {
        return getTable().getDataSource();
    }

    // -----------------
    // Columns
    // -----------------

    @Override
    public List<? extends DBSEntityAttributeRef> getAttributeReferences(DBRProgressMonitor monitor) throws DBException
    {
        return columns;
    }

    public void setColumns(List<DB2TableCheckConstraintColumn> columns)
    {
        this.columns = columns;
    }

    // -----------------
    // Source
    // -----------------

    @Override
    public DB2Schema getSchema()
    {
        return getTable().getSchema();
    }

    @NotNull
    @Override
    public DBSObjectState getObjectState()
    {
        return DBSObjectState.UNKNOWN;
    }

    @Override
    public void refreshObjectState(@NotNull DBRProgressMonitor monitor) throws DBCException
    {
    }

    @Override
    public String getObjectDefinitionText(DBRProgressMonitor monitor) throws DBException
    {
        return SQLUtils.formatSQL(getDataSource(), text);
    }

    // -----------------
    // Properties
    // -----------------
    @Override
    @Property(viewable = true, editable = false, order = 2)
    public DB2Table getTable()
    {
        return super.getTable();
    }

    @NotNull
    @Override
    @Property(hidden = true)
    public DBSEntityConstraintType getConstraintType()
    {
        return super.getConstraintType();
    }

    @Property(viewable = true, editable = false, order = 3)
    public DB2TableCheckConstraintType getType()
    {
        return type;
    }

    @Property(viewable = false, editable = false, category = DB2Constants.CAT_OWNER)
    public String getOwner()
    {
        return owner;
    }

    @Property(viewable = false, editable = false, category = DB2Constants.CAT_OWNER)
    public DB2OwnerType getOwnerType()
    {
        return ownerType;
    }

    @Property(viewable = false, editable = false, category = DB2Constants.CAT_DATETIME)
    public Timestamp getCreateTime()
    {
        return createTime;
    }

    @Property(viewable = false, editable = false)
    public String getQualifier()
    {
        return qualifier;
    }

    @Property(viewable = false, editable = false)
    public String getFumcPath()
    {
        return fumcPath;
    }

    @Property(viewable = false, editable = false, category = DB2Constants.CAT_STATS)
    public Integer getPrecentValid()
    {
        return precentValid;
    }

    @Property(viewable = false, editable = false, category = DB2Constants.CAT_COLLATION)
    public String getCollationSchema()
    {
        return collationSchema;
    }

    @Property(viewable = false, editable = false, category = DB2Constants.CAT_COLLATION)
    public String getCollationName()
    {
        return collationName;
    }

    @Property(viewable = false, editable = false, category = DB2Constants.CAT_COLLATION)
    public String getCollationSchemaOrderBy()
    {
        return collationSchemaOrderBy;
    }

    @Property(viewable = false, editable = false, category = DB2Constants.CAT_COLLATION)
    public String getCollationNameOrderBy()
    {
        return collationNameOrderBy;
    }

}

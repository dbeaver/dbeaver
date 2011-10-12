/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.model;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.IDatabasePersistAction;
import org.jkiss.dbeaver.ext.oracle.model.source.OracleSourceObject;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.LazyProperty;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.meta.PropertyGroup;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Oracle materialized view
 */
public class OracleMaterializedView extends OracleTablePhysical implements OracleSourceObject
{

    public static class AdditionalInfo extends TableAdditionalInfo {
        private String text;
        private boolean updatable;

        @Property(name = "Definition", hidden = true, editable = true, updatable = true, order = -1)
        public String getText() { return text; }
        public void setText(String text) { this.text = text; }

        @Property(name = "Updatable", order = 20)
        public boolean isUpdatable() { return updatable; }
        public void setUpdatable(boolean updatable) { this.updatable = updatable; }
    }

    private final AdditionalInfo additionalInfo = new AdditionalInfo();

    public OracleMaterializedView(OracleSchema schema, String name)
    {
        super(schema, name);
    }

    public OracleMaterializedView(
        OracleSchema schema,
        ResultSet dbResult)
    {
        super(schema, dbResult);
    }

    public boolean isView()
    {
        return true;
    }

    @Override
    public AdditionalInfo getAdditionalInfo()
    {
        return additionalInfo;
    }

    @Override
    protected String getTableTypeName()
    {
        return "MATERIALIZED_VIEW";
    }

    @PropertyGroup()
    @LazyProperty(cacheValidator = AdditionalInfoValidator.class)
    public AdditionalInfo getAdditionalInfo(DBRProgressMonitor monitor) throws DBException
    {
        synchronized (additionalInfo) {
            if (!additionalInfo.loaded && monitor != null) {
                loadAdditionalInfo(monitor);
            }
            return additionalInfo;
        }
    }

    private void loadAdditionalInfo(DBRProgressMonitor monitor) throws DBException
    {
        if (!isPersisted()) {
            additionalInfo.loaded = true;
            return;
        }
        JDBCExecutionContext context = getDataSource().openContext(monitor, DBCExecutionPurpose.META, "Load table status");
        try {
            JDBCPreparedStatement dbStat = context.prepareStatement(
                "SELECT QUERY,UPDATABLE\n" +
                "FROM SYS.ALL_MVIEWS WHERE OWNER=? AND MVIEW_NAME=?");
            try {
                dbStat.setString(1, getContainer().getName());
                dbStat.setString(2, getName());
                JDBCResultSet dbResult = dbStat.executeQuery();
                try {
                    if (dbResult.next()) {
                        additionalInfo.setText(JDBCUtils.safeGetString(dbResult, "QUERY"));
                        additionalInfo.setUpdatable(JDBCUtils.safeGetBoolean(dbResult, "QUERY", "Y"));
                    }
                    additionalInfo.loaded = true;
                } finally {
                    dbResult.close();
                }
            } finally {
                dbStat.close();
            }
        }
        catch (SQLException e) {
            throw new DBCException(e);
        }
        finally {
            context.close();
        }
    }


    public OracleSchema getSchema()
    {
        return getContainer();
    }

    public OracleSourceType getSourceType()
    {
        return OracleSourceType.MATERIALIZED_VIEW;
    }

    @Property(name = "Declaration", hidden = true, editable = true, updatable = true, order = -1)
    public String getSourceDeclaration(DBRProgressMonitor monitor) throws DBException
    {
        return getAdditionalInfo(monitor).getText();
    }

    public void setSourceDeclaration(String source)
    {
        if (source == null) {
            additionalInfo.loaded = false;
        } else {
            additionalInfo.setText(source);
        }
    }

    public IDatabasePersistAction[] getCompileActions()
    {
        return new IDatabasePersistAction[] {
            new OracleObjectPersistAction(
                OracleObjectType.MATERIALIZED_VIEW,
                "Compile materialized view",
                "ALTER MATERIALIZED VIEW " + getFullQualifiedName() + " COMPILE"
            )};
    }

}

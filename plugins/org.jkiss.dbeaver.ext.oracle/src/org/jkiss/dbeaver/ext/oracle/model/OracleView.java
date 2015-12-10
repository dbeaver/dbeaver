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
package org.jkiss.dbeaver.ext.oracle.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.oracle.model.source.OracleSourceObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.LazyProperty;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.meta.PropertyGroup;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;
import org.jkiss.utils.CommonUtils;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * OracleView
 */
public class OracleView extends OracleTableBase implements OracleSourceObject
{

    public static final DBSEntityConstraintType CONSTRAINT_WITH_CHECK_OPTION = new DBSEntityConstraintType("V", "With Check Option", null, false, false);
    public static final DBSEntityConstraintType CONSTRAINT_WITH_READ_ONLY = new DBSEntityConstraintType("O", "With Read Only", null, false, false);

    public class AdditionalInfo extends TableAdditionalInfo {
        private String text;
        private String typeText;
        private String oidText;
        private String typeOwner;
        private String typeName;
        private OracleView superView;

        @Property(hidden = true, editable = true, updatable = true, order = -1)
        public String getText() { return text; }
        public void setText(String text) { this.text = text; }

        @Property(viewable = false, order = 10)
        public Object getType(DBRProgressMonitor monitor) throws DBException
        {
            if (typeOwner == null) {
                return null;
            }
            OracleSchema owner = getDataSource().getSchema(monitor, typeOwner);
            return owner == null ? null : owner.getDataType(monitor, typeName);
        }
        @Property(viewable = false, order = 11)
        public String getTypeText() { return typeText; }
        public void setTypeText(String typeText) { this.typeText = typeText; }
        @Property(viewable = false, order = 12)
        public String getOidText() { return oidText; }
        public void setOidText(String oidText) { this.oidText = oidText; }
        @Property(viewable = false, editable = true, order = 5)
        public OracleView getSuperView() { return superView; }
        public void setSuperView(OracleView superView) { this.superView = superView; }
    }

    private final AdditionalInfo additionalInfo = new AdditionalInfo();

    public OracleView(OracleSchema schema, String name)
    {
        super(schema, name, false);
    }

    public OracleView(
        OracleSchema schema,
        ResultSet dbResult)
    {
        super(schema, dbResult);
    }

    @NotNull
    @Property(viewable = true, editable = true, valueTransformer = DBObjectNameCaseTransformer.class, order = 1)
    @Override
    public String getName()
    {
        return super.getName();
    }

    @Override
    public boolean isView()
    {
        return true;
    }

    @Override
    public OracleSchema getSchema()
    {
        return getContainer();
    }

    @Override
    public OracleSourceType getSourceType()
    {
        return OracleSourceType.VIEW;
    }

    @Override
    @Property(hidden = true, editable = true, updatable = true, order = -1)
    public String getObjectDefinitionText(DBRProgressMonitor monitor) throws DBException
    {
        return getAdditionalInfo(monitor).getText();
    }

    public void setObjectDefinitionText(String source)
    {
        if (source == null) {
            additionalInfo.loaded = false;
        } else {
            additionalInfo.setText(source);
        }
    }

    @Override
    public AdditionalInfo getAdditionalInfo()
    {
        return additionalInfo;
    }

    @Override
    protected String getTableTypeName()
    {
        return "VIEW";
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

    @Override
    public boolean refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException
    {
        additionalInfo.loaded = false;
        super.refreshObject(monitor);
        return true;
    }

    @Override
    public void refreshObjectState(@NotNull DBRProgressMonitor monitor) throws DBCException
    {
        this.valid = OracleUtils.getObjectStatus(monitor, this, OracleObjectType.VIEW);
    }

    private void loadAdditionalInfo(DBRProgressMonitor monitor) throws DBException
    {
        if (!isPersisted()) {
            additionalInfo.loaded = true;
            return;
        }
        try (JDBCSession session = DBUtils.openMetaSession(monitor, getDataSource(), "Load table status")) {
            try (JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT TEXT,TYPE_TEXT,OID_TEXT,VIEW_TYPE_OWNER,VIEW_TYPE,SUPERVIEW_NAME\n" +
                    "FROM SYS.ALL_VIEWS WHERE OWNER=? AND VIEW_NAME=?")) {
                dbStat.setString(1, getContainer().getName());
                dbStat.setString(2, getName());
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    if (dbResult.next()) {
                        additionalInfo.setText(JDBCUtils.safeGetString(dbResult, "TEXT"));
                        additionalInfo.setTypeText(JDBCUtils.safeGetStringTrimmed(dbResult, "TYPE_TEXT"));
                        additionalInfo.setOidText(JDBCUtils.safeGetStringTrimmed(dbResult, "OID_TEXT"));
                        additionalInfo.typeOwner = JDBCUtils.safeGetStringTrimmed(dbResult, "VIEW_TYPE_OWNER");
                        additionalInfo.typeName = JDBCUtils.safeGetStringTrimmed(dbResult, "VIEW_TYPE");

                        String superViewName = JDBCUtils.safeGetString(dbResult, "SUPERVIEW_NAME");
                        if (!CommonUtils.isEmpty(superViewName)) {
                            additionalInfo.setSuperView(getContainer().getView(monitor, superViewName));
                        }
                    }
                    additionalInfo.loaded = true;
                }
            }
        }
        catch (SQLException e) {
            throw new DBCException(e, getDataSource());
        }
    }

    @Override
    public DBEPersistAction[] getCompileActions()
    {
        return new DBEPersistAction[] {
            new OracleObjectPersistAction(
                OracleObjectType.VIEW,
                "Compile view",
                "ALTER VIEW " + getFullQualifiedName() + " COMPILE"
            )};
    }

}

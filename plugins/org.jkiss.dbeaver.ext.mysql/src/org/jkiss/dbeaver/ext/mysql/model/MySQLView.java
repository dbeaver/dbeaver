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
package org.jkiss.dbeaver.ext.mysql.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.mysql.MySQLConstants;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.IPropertyCacheValidator;
import org.jkiss.dbeaver.model.meta.LazyProperty;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.meta.PropertyGroup;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableConstraint;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableForeignKey;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableIndex;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * MySQLView
 */
public class MySQLView extends MySQLTableBase
{
    public enum CheckOption {
        NONE(null),
        CASCADE("CASCADED"),
        LOCAL("LOCAL");
        private final String definitionName;

        CheckOption(String definitionName)
        {
            this.definitionName = definitionName;
        }

        public String getDefinitionName()
        {
            return definitionName;
        }
    }

    public static class AdditionalInfo {
        private volatile boolean loaded = false;
        private String definition;
        private CheckOption checkOption;
        private boolean updatable;
        private String definer;

        public boolean isLoaded() { return loaded; }

        @Property(hidden = true, editable = true, updatable = true, order = -1) public String getDefinition() { return definition; }
        public void setDefinition(String definition) { this.definition = definition; }

        @Property(viewable = true, editable = true, updatable = true, order = 4) public CheckOption getCheckOption() { return checkOption; }
        public void setCheckOption(CheckOption checkOption) { this.checkOption = checkOption; }

        @Property(viewable = true, order = 5) public boolean isUpdatable() { return updatable; }
        public void setUpdatable(boolean updatable) { this.updatable = updatable; }
        @Property(viewable = true, order = 6) public String getDefiner() { return definer; }
        public void setDefiner(String definer) { this.definer = definer; }
    }

    public static class AdditionalInfoValidator implements IPropertyCacheValidator<MySQLView> {
        @Override
        public boolean isPropertyCached(MySQLView object, Object propertyId)
        {
            return object.additionalInfo.loaded;
        }
    }

    private final AdditionalInfo additionalInfo = new AdditionalInfo();

    public MySQLView(MySQLCatalog catalog)
    {
        super(catalog);
    }

    public MySQLView(
        MySQLCatalog catalog,
        ResultSet dbResult)
    {
        super(catalog, dbResult);
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

    public AdditionalInfo getAdditionalInfo()
    {
        return additionalInfo;
    }

    @PropertyGroup()
    @LazyProperty(cacheValidator = AdditionalInfoValidator.class)
    public AdditionalInfo getAdditionalInfo(DBRProgressMonitor monitor) throws DBCException
    {
        synchronized (additionalInfo) {
            if (!additionalInfo.loaded) {
                loadAdditionalInfo(monitor);
            }
            return additionalInfo;
        }
    }

    @Override
    public List<? extends DBSTableIndex> getIndexes(DBRProgressMonitor monitor) throws DBException
    {
        return null;
    }

    @Nullable
    @Override
    public List<? extends DBSTableConstraint> getConstraints(@NotNull DBRProgressMonitor monitor) throws DBException
    {
        return null;
    }

    @Override
    public List<? extends DBSTableForeignKey> getAssociations(@NotNull DBRProgressMonitor monitor) throws DBException
    {
        return null;
    }

    @Override
    public List<? extends DBSTableForeignKey> getReferences(@NotNull DBRProgressMonitor monitor) throws DBException
    {
        return null;
    }

    @Override
    public boolean refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException
    {
        additionalInfo.loaded = false;
        super.refreshObject(monitor);
        return true;
    }

    @Nullable
    @Override
    public String getDescription()
    {
        return null;
    }


    private void loadAdditionalInfo(DBRProgressMonitor monitor) throws DBCException
    {
        if (!isPersisted() || getContainer().isSystem()) {
            additionalInfo.loaded = true;
            return;
        }
        try (JDBCSession session = DBUtils.openMetaSession(monitor, getDataSource(), "Load table status")) {
            try (JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT * FROM " + MySQLConstants.META_TABLE_VIEWS + " WHERE " + MySQLConstants.COL_TABLE_SCHEMA + "=? AND " + MySQLConstants.COL_TABLE_NAME + "=?")) {
                dbStat.setString(1, getContainer().getName());
                dbStat.setString(2, getName());
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    if (dbResult.next()) {
                        try {
                            additionalInfo.setCheckOption(CheckOption.valueOf(JDBCUtils.safeGetString(dbResult, MySQLConstants.COL_CHECK_OPTION)));
                        } catch (IllegalArgumentException e) {
                            log.warn(e);
                        }
                        additionalInfo.setDefiner(JDBCUtils.safeGetString(dbResult, MySQLConstants.COL_DEFINER));
                        additionalInfo.setDefinition(
                            SQLUtils.formatSQL(
                                getDataSource(),
                                JDBCUtils.safeGetString(dbResult, MySQLConstants.COL_VIEW_DEFINITION)));
                        additionalInfo.setUpdatable("YES".equals(JDBCUtils.safeGetString(dbResult, MySQLConstants.COL_IS_UPDATABLE)));
                    }
                    additionalInfo.loaded = true;
                }
            }
        } catch (SQLException e) {
            throw new DBCException(e, getDataSource());
        }
    }

    @Override
    @Property(hidden = true, editable = true, updatable = true, order = -1)
    public String getObjectDefinitionText(DBRProgressMonitor monitor) throws DBException
    {
        return getAdditionalInfo(monitor).getDefinition();
    }

    @Override
    public void setObjectDefinitionText(String sourceText) throws DBException
    {
        getAdditionalInfo().setDefinition(sourceText);
    }

}

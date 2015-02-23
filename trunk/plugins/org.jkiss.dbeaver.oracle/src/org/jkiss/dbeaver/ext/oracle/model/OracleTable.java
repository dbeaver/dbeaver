/*
 * Copyright (C) 2010-2015 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.ext.oracle.model;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.data.DBDPseudoAttribute;
import org.jkiss.dbeaver.model.data.DBDPseudoAttributeContainer;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.CommonUtils;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * OracleTable
 */
public class OracleTable extends OracleTablePhysical implements DBDPseudoAttributeContainer
{
    private OracleDataType tableType;
    private String iotType;
    private String iotName;
    private boolean temporary;
    private boolean secondary;
    private boolean nested;

    public static class AdditionalInfo extends TableAdditionalInfo {
    }

    private final AdditionalInfo additionalInfo = new AdditionalInfo();

    public OracleTable(OracleSchema schema, String name)
    {
        super(schema, name);
    }

    public OracleTable(
        DBRProgressMonitor monitor,
        OracleSchema schema,
        ResultSet dbResult)
    {
        super(schema, dbResult);
        String typeOwner = JDBCUtils.safeGetString(dbResult, "TABLE_TYPE_OWNER");
        if (!CommonUtils.isEmpty(typeOwner)) {
            tableType = OracleDataType.resolveDataType(
                monitor,
                schema.getDataSource(),
                typeOwner,
                JDBCUtils.safeGetString(dbResult, "TABLE_TYPE"));
        }
        this.iotType = JDBCUtils.safeGetString(dbResult, "IOT_TYPE");
        this.iotName = JDBCUtils.safeGetString(dbResult, "IOT_NAME");
        this.temporary = JDBCUtils.safeGetBoolean(dbResult, "TEMPORARY", "Y");
        this.secondary = JDBCUtils.safeGetBoolean(dbResult, "SECONDARY", "Y");
        this.nested = JDBCUtils.safeGetBoolean(dbResult, "NESTED", "Y");
    }

    @Override
    public TableAdditionalInfo getAdditionalInfo()
    {
        return additionalInfo;
    }

    @Override
    protected String getTableTypeName()
    {
        return "TABLE";
    }

    @Override
    public boolean isView()
    {
        return false;
    }

    @Property(viewable = false, order = 5)
    public OracleDataType getTableType()
    {
        return tableType;
    }

    @Property(viewable = false, order = 6)
    public String getIotType()
    {
        return iotType;
    }

    @Property(viewable = false, order = 7)
    public String getIotName()
    {
        return iotName;
    }

    @Property(viewable = false, order = 10)
    public boolean isTemporary()
    {
        return temporary;
    }

    @Property(viewable = false, order = 11)
    public boolean isSecondary()
    {
        return secondary;
    }

    @Property(viewable = false, order = 12)
    public boolean isNested()
    {
        return nested;
    }

    @Override
    public Collection<OracleTableForeignKey> getReferences(DBRProgressMonitor monitor)
        throws DBException
    {
        List<OracleTableForeignKey> refs = new ArrayList<OracleTableForeignKey>();
        // This is dummy implementation
        // Get references from this schema only
        final Collection<OracleTableForeignKey> allForeignKeys =
            getContainer().foreignKeyCache.getObjects(monitor, getContainer(), null);
        for (OracleTableForeignKey constraint : allForeignKeys) {
            if (constraint.getReferencedTable() == this) {
                refs.add(constraint);
            }
        }
        return refs;
    }

    @Override
    @Association
    public Collection<OracleTableForeignKey> getAssociations(DBRProgressMonitor monitor)
        throws DBException
    {
        return getContainer().foreignKeyCache.getObjects(monitor, getContainer(), this);
    }

    @Override
    public boolean refreshObject(DBRProgressMonitor monitor) throws DBException
    {
        super.refreshObject(monitor);
        getContainer().foreignKeyCache.clearObjectCache(this);

        return true;
    }

    @Override
    public DBDPseudoAttribute[] getPseudoAttributes() throws DBException
    {
        if (CommonUtils.isEmpty(this.iotType) && getDataSource().getContainer().getPreferenceStore().getBoolean(OracleConstants.PREF_SUPPORT_ROWID)) {
            // IOT tables have index id instead of ROWID
            return new DBDPseudoAttribute[] {
                OracleConstants.PSEUDO_ATTR_ROWID
            };
        } else {
            return null;
        }
    }

    @Override
    protected void appendSelectSource(StringBuilder query, String tableAlias, DBDPseudoAttribute rowIdAttribute) {
        if (tableType != null && tableType.getName().equals(OracleConstants.TYPE_NAME_XML)) {
            query.append("XMLType(value(").append(tableAlias).append(").getClobval()) as XML");
            if (rowIdAttribute != null) {
                query.append(",").append(rowIdAttribute.translateExpression(tableAlias));
            }
        } else {
            super.appendSelectSource(query, tableAlias, rowIdAttribute);
        }
    }
}

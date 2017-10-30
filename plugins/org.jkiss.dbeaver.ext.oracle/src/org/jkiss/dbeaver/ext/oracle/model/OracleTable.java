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
package org.jkiss.dbeaver.ext.oracle.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.DBPImageProvider;
import org.jkiss.dbeaver.model.DBPScriptObject;
import org.jkiss.dbeaver.model.data.DBDPseudoAttribute;
import org.jkiss.dbeaver.model.data.DBDPseudoAttributeContainer;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.utils.CommonUtils;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * OracleTable
 */
public class OracleTable extends OracleTablePhysical implements DBPScriptObject, DBDPseudoAttributeContainer, DBPImageProvider
{
    private static final Log log = Log.getLog(OracleTable.class);

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
        if (!CommonUtils.isEmpty(iotName)) {
            //this.setName(iotName);
        }
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
    public OracleTableColumn getAttribute(@NotNull DBRProgressMonitor monitor, @NotNull String attributeName) throws DBException {
/*
        // Fake XML attribute handle
        if (tableType != null && tableType.getName().equals(OracleConstants.TYPE_NAME_XML) && OracleConstants.XML_COLUMN_NAME.equals(attributeName)) {
            OracleTableColumn col = getXMLColumn(monitor);
            if (col != null) return col;
        }
*/

        return super.getAttribute(monitor, attributeName);
    }

    @Nullable
    private OracleTableColumn getXMLColumn(DBRProgressMonitor monitor) throws DBException {
        for (OracleTableColumn col : CommonUtils.safeCollection(getAttributes(monitor))) {
            if (col.getDataType() == tableType) {
                return col;
            }
        }
        return null;
    }


    @Override
    public Collection<OracleTableForeignKey> getReferences(@NotNull DBRProgressMonitor monitor)
        throws DBException
    {
        List<OracleTableForeignKey> refs = new ArrayList<>();
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
    public Collection<OracleTableForeignKey> getAssociations(@NotNull DBRProgressMonitor monitor)
        throws DBException
    {
        return getContainer().foreignKeyCache.getObjects(monitor, getContainer(), this);
    }

    @Override
    public DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException
    {
        getContainer().foreignKeyCache.clearObjectCache(this);
        return super.refreshObject(monitor);
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
    protected void appendSelectSource(DBRProgressMonitor monitor, StringBuilder query, String tableAlias, DBDPseudoAttribute rowIdAttribute) {
        if (tableType != null && tableType.getName().equals(OracleConstants.TYPE_NAME_XML)) {
            try {
                OracleTableColumn xmlColumn = getXMLColumn(monitor);
                if (xmlColumn != null) {
                    query.append("XMLType(").append(tableAlias).append(".").append(xmlColumn.getName()).append(".getClobval()) as ").append(xmlColumn.getName());
                    if (rowIdAttribute != null) {
                        query.append(",").append(rowIdAttribute.translateExpression(tableAlias));
                    }
                    return;
                }
            } catch (DBException e) {
                log.warn(e);
            }
        }
        super.appendSelectSource(monitor, query, tableAlias, rowIdAttribute);
    }

    @Override
    public String getObjectDefinitionText(DBRProgressMonitor monitor, Map<String, Object> options) throws DBException {
        return getDDL(monitor, OracleDDLFormat.getCurrentFormat(getDataSource()), options);
    }


    @Nullable
    @Override
    public DBPImage getObjectImage() {
        if (CommonUtils.isEmpty(iotType)) {
            return DBIcon.TREE_TABLE;
        } else {
            return DBIcon.TREE_TABLE_INDEX;
        }
    }

}

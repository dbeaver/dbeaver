/*
 * Copyright (C) 2013      Denis Forveille titou10.titou10@gmail.com
 * Copyright (C) 2010-2013 Serge Rieder serge@jkiss.org
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
package org.jkiss.dbeaver.ext.db2.model;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.db2.DB2Constants;
import org.jkiss.dbeaver.ext.db2.editors.DB2ColumnDataTypeListProvider;
import org.jkiss.dbeaver.ext.db2.model.dict.DB2ColumnHiddenState;
import org.jkiss.dbeaver.ext.db2.model.dict.DB2TableColumnCompression;
import org.jkiss.dbeaver.ext.db2.model.dict.DB2TableColumnGenerated;
import org.jkiss.dbeaver.ext.db2.model.dict.DB2YesNo;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.DBPHiddenObject;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCTableColumn;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableColumn;
import org.jkiss.utils.CommonUtils;

import java.sql.ResultSet;

/**
 * DB2 Table Column
 * 
 * @author Denis Forveille
 */
public class DB2TableColumn extends JDBCTableColumn<DB2TableBase> implements DBSTableColumn, DBPHiddenObject {

    private DB2DataType dataType;
    private DB2Schema dataTypeSchema;
    private String remarks;
    private Boolean hidden;
    private Boolean identity;
    private Boolean lobCompact;
    private DB2TableColumnGenerated generated;
    private String generatedText;
    private DB2TableColumnCompression compress;
    private Boolean rowBegin;
    private Boolean rowEnd;
    private String collationSchema;
    private String collationNane;

    private Integer keySeq;
    private Integer partKeySeq;

    private Long colcard;
    private String high2key;
    private String low2key;
    private Integer avgLength;
    private Long nbNulls;

    // -----------------
    // Constructors
    // -----------------

    public DB2TableColumn(DBRProgressMonitor monitor, DB2TableBase tableBase, ResultSet dbResult) throws DBException
    {
        super(tableBase, true);

        DB2DataSource db2DataSource = (DB2DataSource) tableBase.getDataSource();

        setName(JDBCUtils.safeGetString(dbResult, "COLNAME"));
        setOrdinalPosition(JDBCUtils.safeGetInt(dbResult, "COLNO"));
        setRequired(JDBCUtils.safeGetBoolean(dbResult, "NULLS", DB2YesNo.N.name()));
        setDefaultValue(JDBCUtils.safeGetString(dbResult, "DEFAULT"));
        setMaxLength(JDBCUtils.safeGetInteger(dbResult, "LENGTH"));
        setScale(JDBCUtils.safeGetInteger(dbResult, "SCALE"));

        this.hidden = DB2ColumnHiddenState.isHidden(JDBCUtils.safeGetString(dbResult, "HIDDEN"));
        this.identity = JDBCUtils.safeGetBoolean(dbResult, "IDENTITY", DB2YesNo.Y.name());
        this.lobCompact = JDBCUtils.safeGetBoolean(dbResult, "COMPACT", DB2YesNo.Y.name());
        this.generated = CommonUtils.valueOf(DB2TableColumnGenerated.class, JDBCUtils.safeGetString(dbResult, "GENERATED"));
        this.generatedText = JDBCUtils.safeGetString(dbResult, "TEXT");
        this.compress = CommonUtils.valueOf(DB2TableColumnCompression.class, JDBCUtils.safeGetString(dbResult, "COMPRESS"));
        this.colcard = JDBCUtils.safeGetLong(dbResult, "COLCARD");
        this.high2key = JDBCUtils.safeGetString(dbResult, "HIGH2KEY");
        this.low2key = JDBCUtils.safeGetString(dbResult, "LOW2KEY");
        this.avgLength = JDBCUtils.safeGetInteger(dbResult, "AVGCOLLEN");
        this.nbNulls = JDBCUtils.safeGetLong(dbResult, "NUMNULLS");
        this.keySeq = JDBCUtils.safeGetInteger(dbResult, "KEYSEQ");
        this.partKeySeq = JDBCUtils.safeGetInteger(dbResult, "PARTKEYSEQ");

        this.remarks = JDBCUtils.safeGetString(dbResult, "REMARKS");

        if (db2DataSource.isAtLeastV9_5()) {
            this.collationSchema = JDBCUtils.safeGetStringTrimmed(dbResult, "COLLATIONSCHEMA");
            this.collationNane = JDBCUtils.safeGetString(dbResult, "COLLATIONNAME");
        }
        if (db2DataSource.isAtLeastV10_1()) {
            this.rowBegin = JDBCUtils.safeGetBoolean(dbResult, "ROWBEGIN", DB2YesNo.Y.name());
            this.rowEnd = JDBCUtils.safeGetBoolean(dbResult, "ROWEND", DB2YesNo.Y.name());
        }

        // Set DataTypes data
        // Search for DataType
        // Look first in Standards type
        String typeName = JDBCUtils.safeGetString(dbResult, "TYPENAME");
        this.dataType = tableBase.getDataSource().getDataTypeCache().getObject(monitor, getTable().getDataSource(), typeName);
        if (this.dataType == null) {
            String typeSchemaName = JDBCUtils.safeGetStringTrimmed(dbResult, "TYPESCHEMA");
            this.dataTypeSchema = getDataSource().getSchema(monitor, typeSchemaName);
            this.dataType = this.dataTypeSchema.getUDT(monitor, typeName);
        } else {
            this.dataTypeSchema = dataType.getSchema();
        }
        setTypeName(dataType.getFullQualifiedName());
        setValueType(dataType.getTypeID());
    }

    public DB2TableColumn(DB2TableBase tableBase)
    {
        super(tableBase, false);

        setMaxLength(50L);
        setOrdinalPosition(-1);
        this.dataType = tableBase.getDataSource().getDataTypeCache().getCachedObject("VARCHAR");
        this.dataTypeSchema = dataType.getSchema();
        setTypeName(dataType.getFullQualifiedName());
        setValueType(dataType.getTypeID());
        setRequired(true);
    }

    // -----------------
    // Business Contract
    // -----------------

    @Override
    public DB2DataSource getDataSource()
    {
        return getTable().getDataSource();
    }

    @Override
    public boolean isSequence()
    {
        return false;
    }

    @Override
    public boolean isHidden()
    {
        return hidden;
    }

    @Override
    public DBPDataKind getDataKind()
    {
        return dataType.getDataKind();
    }

    @Override
    public String getTypeName()
    {
        return super.getTypeName();
    }

    // -----------------
    // Properties
    // -----------------
    @Property(viewable = true, editable = false, order = 19, category = DB2Constants.CAT_OWNER)
    public DB2TableBase getOwner()
    {
        return getTable();
    }

    @Property(viewable = true, editable = false, order = 20, category = DB2Constants.CAT_OWNER)
    public DB2Schema getTypeSchema()
    {
        return dataTypeSchema;
    }

    @Property(viewable = true, editable = true, updatable = true, order = 21, listProvider = DB2ColumnDataTypeListProvider.class)
    public DBSDataType getType()
    {
        return dataType;
    }

    public void setType(DB2DataType dataType)
    {
        this.dataType = dataType;
    }

    @Override
    @Property(viewable = true, order = 38)
    public long getMaxLength()
    {
        return super.getMaxLength();
    }

    @Override
    @Property(viewable = true, order = 39)
    public int getScale()
    {
        return super.getScale();
    }

    @Override
    @Property(viewable = false, order = 40)
    public int getPrecision()
    {
        return super.getPrecision();
    }

    @Override
    @Property(viewable = true, order = 41, editable = true, updatable = true)
    public boolean isRequired()
    {
        return super.isRequired();
    }

    @Override
    @Property(viewable = true, order = 42, editable = true)
    public String getDefaultValue()
    {
        return super.getDefaultValue();
    }

    @Property(viewable = true, order = 43)
    public Boolean getIdentity()
    {
        return identity;
    }

    @Property(viewable = true, order = 44)
    public DB2TableColumnGenerated getGenerated()
    {
        return generated;
    }

    @Property(viewable = false, order = 45)
    public String getGeneratedText()
    {
        return generatedText;
    }

    @Override
    @Property(viewable = false, order = 999, editable = true, updatable = true)
    public String getDescription()
    {
        return remarks;
    }

    public void setDescription(String remarks)
    {
        this.remarks = remarks;
    }

    @Property(viewable = false, order = 134)
    public Boolean getLobCompact()
    {
        return lobCompact;
    }

    @Property(viewable = false, order = 120)
    public Integer getKeySeq()
    {
        return keySeq;
    }

    @Property(viewable = false, order = 121)
    public Integer getPartKeySeq()
    {
        return partKeySeq;
    }

    @Property(viewable = false, order = 137)
    public DB2TableColumnCompression getCompress()
    {
        return compress;
    }

    @Property(viewable = false, order = 138)
    public Boolean getRowBegin()
    {
        return rowBegin;
    }

    @Property(viewable = false, order = 139)
    public Boolean getRowEnd()
    {
        return rowEnd;
    }

    @Property(viewable = false, order = 150, category = DB2Constants.CAT_STATS)
    public Long getColcard()
    {
        return colcard;
    }

    @Property(viewable = false, order = 151, category = DB2Constants.CAT_STATS)
    public Long getNbNulls()
    {
        return nbNulls;
    }

    @Property(viewable = false, order = 152, category = DB2Constants.CAT_STATS)
    public Integer getAvgLength()
    {
        return avgLength;
    }

    @Property(viewable = false, order = 153, category = DB2Constants.CAT_STATS)
    public String getLow2key()
    {
        return low2key;
    }

    @Property(viewable = false, order = 154, category = DB2Constants.CAT_STATS)
    public String getHigh2key()
    {
        return high2key;
    }

    @Property(viewable = false, order = 180, category = DB2Constants.CAT_COLLATION)
    public String getCollationSchema()
    {
        return collationSchema;
    }

    @Property(viewable = false, order = 181, category = DB2Constants.CAT_COLLATION)
    public String getCollationNane()
    {
        return collationNane;
    }

}

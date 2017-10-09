/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2016-2016 Karl Griesser (fullref@gmail.com)
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
package org.jkiss.dbeaver.ext.exasol.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.exasol.editors.ExasolColumnDataTypeListProvider;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPHiddenObject;
import org.jkiss.dbeaver.model.DBPNamedObject2;
import org.jkiss.dbeaver.model.impl.DBPositiveNumberTransformer;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCColumnKeyType;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCTableColumn;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.dbeaver.model.struct.DBSTypedObjectEx;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableColumn;
import org.jkiss.utils.CommonUtils;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.util.Collection;

public class ExasolTableColumn extends JDBCTableColumn<ExasolTableBase>
    implements DBSTableColumn, DBSTypedObjectEx, DBPHiddenObject, DBPNamedObject2, JDBCColumnKeyType {

    private ExasolDataType dataType;
    private Boolean identity;
    private BigDecimal identityValue;
    private String remarks;
    private Boolean isInDistKey;
    private String formatType;
    private Boolean changed = false;
    private Boolean oriRequired;

    // -----------------
    // Constructors
    // -----------------

    public ExasolTableColumn(DBRProgressMonitor monitor, ExasolTableBase tableBase, ResultSet dbResult)
        throws DBException {
        super(tableBase, true);
        
        
        this.formatType = JDBCUtils.safeGetString(dbResult, "COLUMN_TYPE");
        setName(JDBCUtils.safeGetString(dbResult, "COLUMN_NAME"));
        setOrdinalPosition(JDBCUtils.safeGetInt(dbResult, "ORDINAL_POSITION"));
        setRequired(! JDBCUtils.safeGetBoolean(dbResult, "COLUMN_IS_NULLABLE"));
        setDefaultValue(JDBCUtils.safeGetString(dbResult, "COLUMN_DEF"));
        setMaxLength(JDBCUtils.safeGetInt(dbResult, "COLUMN_SIZE"));
        setScale(JDBCUtils.safeGetInteger(dbResult, "DECIMAL_DIGITS"));
        

        this.isInDistKey = JDBCUtils.safeGetBoolean(dbResult, "COLUMN_IS_DISTRIBUTION_KEY");
        this.identity = JDBCUtils.safeGetInteger(dbResult, "COLUMN_IDENTITY") == null ? false : true;
        if (identity)
            this.identityValue = JDBCUtils.safeGetBigDecimal(dbResult, "COLUMN_IDENTITY");
        this.remarks = JDBCUtils.safeGetString(dbResult, "COLUMN_COMMENT");
        this.dataType = tableBase.getDataSource().getDataType(monitor, JDBCUtils.safeGetString(dbResult, "TYPE_NAME"));

        // drivers > 5 have the issue that a cast from decimal without scale is made to matching integer in sql
        // so meta data queries have to handle this case
        if 	(tableBase.getDataSource().getDriverMajorVersion() > 5 && this.dataType.getName().equals("DECIMAL") && CommonUtils.toInt(super.getScale()) == 0)
        {
        	if (super.getMaxLength() <= 4) {
        		this.dataType = tableBase.getDataSource().getDataType(monitor,"SMALLINT");
			} else if (super.getMaxLength() > 4 && super.getMaxLength() <= 9 ) {
        		this.dataType = tableBase.getDataSource().getDataType(monitor,"INTEGER");
			} else if (super.getMaxLength() > 9 && super.getMaxLength() <= 18 ) {
	    		this.dataType = tableBase.getDataSource().getDataType(monitor,"BIGINT");
			}
        }
        
        this.changed = true;


    }

    public ExasolTableColumn(ExasolTableBase tableBase) {
        super(tableBase, false);

        setMaxLength(50L);
        setOrdinalPosition(-1);
        this.dataType = tableBase.getDataSource().getDataTypeCache().getCachedObject("VARCHAR");
        setTypeName(dataType.getFullyQualifiedName(DBPEvaluationContext.DML));
        setRequired(true);
    }

    // -----------------
    // Business Contract
    // -----------------

    @NotNull
    @Override
    public ExasolDataSource getDataSource() {
        return getTable().getDataSource();
    }

    @Override
    public DBPDataKind getDataKind() {
        return dataType.getDataKind();
    }

    @Override
    public String getTypeName() {
        return this.dataType.getName();
    }
    
    @Override
    public int getTypeID()
    {
    	return this.dataType.getTypeID();
    }
    

    // -----------------
    // Properties
    // -----------------
    @Property(viewable = true, editable = false, order = 19)
    public ExasolTableBase getOwner() {
        return getTable();
    }

    @Nullable
    @Property(viewable = true, editable = true, updatable = true, order = 21, listProvider = ExasolColumnDataTypeListProvider.class)
    public DBSDataType getDataType() {
        return dataType;
    }

    public void setDataType(ExasolDataType dataType) {
        if (!this.dataType.getTypeName().equals(dataType))
            this.changed = true;
        this.dataType = dataType;
    }

    @Override
    @Property(viewable = true, editable = true, updatable = true, order = 38)
    public long getMaxLength() {
        return super.getMaxLength();
    }

    public void setMaxLength(long maxLength) {
        if (this.maxLength != maxLength)
            this.changed = true;
        super.setMaxLength(maxLength);
    }

    @Override
    @Property(viewable = true, editable = true, updatable = true, valueRenderer = DBPositiveNumberTransformer.class, order = 39)
    public Integer getScale() {
        return super.getScale();
    }

    public void setScale(Integer scale) {
        if (!CommonUtils.equalObjects(this.scale, scale))
            this.changed = true;
        super.setScale(scale);
    }

    @Property(viewable = true, editable = true, updatable = true, order = 46)
    @Nullable
    public BigDecimal getIdentityValue() {
        return this.identityValue;
    }

    public void setIdentityValue(BigDecimal identityValue) {
        this.identityValue = identityValue;
    }

    @Property(viewable = false, order = 40)
    public String getStringLength() {
        return "";
    }

    @Override
    @Property(viewable = false, editable = true, updatable = true, valueRenderer = DBPositiveNumberTransformer.class, order = 42)
    public Integer getPrecision() {
        return super.getPrecision();
    }

    public void setPrecision(Integer precision) {
        if (this.precision != precision)
            this.changed = true;
        super.precision = precision;
        this.precision = precision;

    }

    @Override
    @Property(viewable = true, order = 43, editable = true, updatable = true)
    public boolean isRequired() {
        return super.isRequired();
    }

    public void setRequired(boolean required) {
    	if (changed && oriRequired == null)
    		oriRequired = super.isRequired();
        super.setRequired(required);
    }

    @Override
    @Property(viewable = true, order = 44, editable = true, updatable = true)
    public String getDefaultValue() {
        return super.getDefaultValue();
    }

    public void setDefaultValue(String defaultValue) {
        super.setDefaultValue(defaultValue);
    }

    public void setIdentity(Boolean identity) {
        this.identity = identity;
    }

    @Nullable
    @Override
    @Property(viewable = true, order = 999, editable = true, updatable = true)
    public String getDescription() {
        return remarks;
    }

    public void setDescription(String remarks) {
        this.remarks = remarks;
    }

    @Property(viewable = false, order = 121)
    public Boolean isDistKey() {
        return isInDistKey;
    }


    // no hidden columns supported in exasol
    @Override
    public boolean isHidden() {
        return false;
    }
    
    @Override
    @Property(viewable = true, editable = true, updatable = true, order = 45)
    public boolean isAutoGenerated()
    {
    	return this.identity;
    }
    
    public void setAutoGenerated(Boolean identity)
    {
    	this.identity = identity;
    }

    public String getFormatType() {
        if (changed) {
            switch (this.dataType.getTypeName()) {
                case "VARCHAR":
                case "CHAR":
                    this.scale = 0;
                    return dataType.getTypeName() + "(" + Long.toString(this.maxLength) + ")";
                case "DECIMAL":
                    return dataType.getTypeName() + "(" + Long.toString(this.maxLength) + "," + Long.toString(this.scale) + ")";
                default:
                    return dataType.getTypeName();
            }
        }
        return this.formatType;
    }

    @Override
    @Property(viewable = true, order = 80)
	public boolean isInUniqueKey() 
	{
        
        if (getTable().getClass() == ExasolView.class)
        {
            return false;
        }

		ExasolTableBase table = (ExasolTable) getTable();
		try {
			final Collection<ExasolTableUniqueKey> uniqueKeysCache = table.getConstraints(new VoidProgressMonitor());
			if (!CommonUtils.isEmpty(uniqueKeysCache))
			{
				for (ExasolTableUniqueKey key : uniqueKeysCache)
				{
					if (key.hasColumn(this))
						return true;
				}
			}
		} catch ( DBException e)
		{
			return false;
		}
		return false;
	}

	@Property(hidden = true)
    public Boolean isOriRequired()
    {
    	return oriRequired;
    }

	@Override
	public boolean isInReferenceKey()
	{
		// don't need this one
		return false;
	}


}

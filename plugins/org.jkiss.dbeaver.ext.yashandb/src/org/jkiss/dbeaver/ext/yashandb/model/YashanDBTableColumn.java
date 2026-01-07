/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.yashandb.model;

import java.sql.ResultSet;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPHiddenObject;
import org.jkiss.dbeaver.model.DBPNamedObject2;
import org.jkiss.dbeaver.model.DBPObjectWithLazyDescription;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCTableColumn;
import org.jkiss.dbeaver.model.meta.IPropertyCacheValidator;
import org.jkiss.dbeaver.model.meta.LazyProperty;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.meta.PropertyLength;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSTypedObjectEx;
import org.jkiss.dbeaver.model.struct.DBSTypedObjectExt3;
import org.jkiss.dbeaver.model.struct.DBSTypedObjectExt4;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableColumn;
import org.jkiss.utils.CommonUtils;

public class YashanDBTableColumn extends JDBCTableColumn<YashanDBTableBase>
		implements DBSTableColumn, DBSTypedObjectEx, DBSTypedObjectExt3, DBPHiddenObject, DBPNamedObject2,
		DBSTypedObjectExt4<YashanDBDataType>, DBPObjectWithLazyDescription {

	public YashanDBTableColumn(YashanDBTableBase table) {
		super(table, false);
	}

	public YashanDBTableColumn(DBRProgressMonitor monitor, YashanDBTableBase table, ResultSet dbResult)
			throws DBException {
		super(table, true);
		setDefaultValue(JDBCUtils.safeGetString(dbResult, "DATA_DEFAULT"));
		setName(JDBCUtils.safeGetString(dbResult, "COLUMN_NAME"));
		setOrdinalPosition(JDBCUtils.safeGetInt(dbResult, "COLUMN_ID"));
		this.typeName = JDBCUtils.safeGetString(dbResult, "DATA_TYPE");
		this.type = YashanDBDataType.resolveDataType(monitor, getDataSource(),
				JDBCUtils.safeGetString(dbResult, "DATA_TYPE_OWNER"), this.typeName);
		if (this.type != null) {
			this.typeName = type.getFullyQualifiedName(DBPEvaluationContext.DDL);
			this.valueType = type.getTypeID();
		}
		setMaxLength(JDBCUtils.safeGetLong(dbResult, "DATA_LENGTH"));
		setRequired(!"Y".equals(JDBCUtils.safeGetString(dbResult, "NULLABLE")));
		this.scale = JDBCUtils.safeGetInteger(dbResult, "DATA_SCALE");
		if (this.scale == null || this.scale < 0) {
			if (this.type != null && this.type.getScale() != null) {
				this.scale = this.type.getScale();
			}
		}

		if (typeName.equals("BIT")) {
			setPrecision(CommonUtils.toInt(JDBCUtils.safeGetLong(dbResult, "DATA_LENGTH")));
		} else {
			setPrecision(JDBCUtils.safeGetInteger(dbResult, "DATA_PRECISION"));
		}

		this.type = YashanDBDataType.resolveDataType(monitor, getDataSource(),
				JDBCUtils.safeGetString(dbResult, "DATA_TYPE_OWNER"), this.typeName);
		if (this.type != null) {
			this.typeName = type.getFullyQualifiedName(DBPEvaluationContext.DDL);
			this.valueType = type.getTypeID();
		}
		if (this.scale == null || this.scale < 0) {
			if (this.type != null && this.type.getScale() != null) {
				this.scale = this.type.getScale();
			}
		}

	}

	private YashanDBDataType type;
	private String comment;
	private Integer scale;

	@Override
	public YashanDBDataSource getDataSource() {
		return getTable().getDataSource();
	}

	@Override
	public boolean isHidden() {
		return false;
	}

	@Override
	public String getDescription(DBRProgressMonitor monitor) throws DBException {
		return null;
	}

	public static class CommentLoadValidator implements IPropertyCacheValidator<YashanDBTableColumn> {
		@Override
		public boolean isPropertyCached(YashanDBTableColumn object, Object propertyId) {
			return object.comment != null;
		}
	}

	@Property(viewable = true, editable = true, updatable = true, length = PropertyLength.MULTILINE, order = 100)
	@LazyProperty(cacheValidator = CommentLoadValidator.class)
	public String getComment(DBRProgressMonitor monitor) {
		if (isPersisted() && comment == null) {
			getTable().loadColumnComments(monitor);
		}
		return comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	void cacheComment() {
		if (this.comment == null) {
			this.comment = "";
		}
	}

	@Property(viewable = true, order = 15)
	public int getOrdinalPosition() {
		return ordinalPosition;
	}

	public void setOrdinalPosition(int ordinalPosition) {
		this.ordinalPosition = ordinalPosition;
	}

	@Override
	public String getTypeName() {
		return super.getTypeName();
	}

	@Property(viewable = true, editable = true, updatable = true, order = 20, listProvider = ColumnTypeNameListProvider.class)
	@Override
	public String getFullTypeName() {
		return DBUtils.getFullTypeName(this);
	}

	public void setDataType(YashanDBDataType type) {
		this.type = type;
		this.typeName = type == null ? "" : type.getFullyQualifiedName(DBPEvaluationContext.DDL);
	}

	public YashanDBDataType getDataType() {
		return type;
	}

	@Property(viewable = false, editableExpr = "!object.table.view", updatableExpr = "!object.table.view", order = 40)
	@Override
	public long getMaxLength() {
		return super.getMaxLength();
	}

	@Override
	@Property(viewable = false, editableExpr = "!object.table.view", updatableExpr = "!object.table.view", order = 41)
	public Integer getPrecision() {
		return super.getPrecision();
	}

	@Override
	@Property(viewable = false, editableExpr = "!object.table.view", updatableExpr = "!object.table.view", order = 42)
	public Integer getScale() {
		return scale;
	}

	@Override
	public void setScale(Integer scale) {
		this.scale = scale;
	}

	@Property(viewable = true, editableExpr = "!object.table.view", updatableExpr = "!object.table.view", order = 50)
	@Override
	public boolean isRequired() {
		return super.isRequired();
	}

	@Property(viewable = true, editableExpr = "!object.table.view", updatableExpr = "!object.table.view", order = 70)
	@Override
	public String getDefaultValue() {
		return super.getDefaultValue();
	}

	@Override
	public boolean isAutoGenerated() {
		return false;
	}
}

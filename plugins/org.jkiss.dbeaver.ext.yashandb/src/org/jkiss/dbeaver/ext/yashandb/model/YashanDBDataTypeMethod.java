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

import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityMethod;

public class YashanDBDataTypeMethod extends YashanDBDataTypeMember implements DBSEntityMethod {

	private String methodType;
	private boolean flagFinal;
	private boolean flagInstantiable;
	private boolean flagOverriding;

	public YashanDBDataTypeMethod(YashanDBDataType dataType) {
		super(dataType);
	}

	public YashanDBDataTypeMethod(DBRProgressMonitor monitor, YashanDBDataType dataType, ResultSet dbResult) {
		super(dataType, dbResult);
		this.name = JDBCUtils.safeGetString(dbResult, "METHOD_NAME");
		this.no = JDBCUtils.safeGetInt(dbResult, "METHOD_NO");
		this.methodType = JDBCUtils.safeGetString(dbResult, "METHOD_TYPE");
		this.flagFinal = JDBCUtils.safeGetBoolean(dbResult, "FINAL", YashanDBConstants.YES);
		this.flagInstantiable = JDBCUtils.safeGetBoolean(dbResult, "INSTANTIABLE", YashanDBConstants.YES);
		this.flagOverriding = JDBCUtils.safeGetBoolean(dbResult, "OVERRIDING", YashanDBConstants.YES);
	}

	@Property(viewable = true, editable = true, order = 5)
	public String getMethodType() {
		return methodType;
	}

	@Property(viewable = true, order = 8)
	public boolean isFinal() {
		return flagFinal;
	}

	@Property(viewable = true, order = 9)
	public boolean isInstantiable() {
		return flagInstantiable;
	}

	@Property(viewable = true, order = 10)
	public boolean isOverriding() {
		return flagOverriding;
	}
}

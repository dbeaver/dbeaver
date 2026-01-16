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
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.LazyProperty;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObjectLazy;
import org.jkiss.utils.CommonUtils;

public class YashanDBRecycledObject extends YashanDBSchemaObject implements DBSObjectLazy<YashanDBDataSource> {

	public enum Operation {
		DROP, TRUNCATE
	}

	private String recycledName;
	private Operation operation;
	private String objectType;
	private Object tablespace;
	private String dropTime;
	private String partitionName;

	protected YashanDBRecycledObject(YashanDBSchema schema, ResultSet dbResult) {
		super(schema, JDBCUtils.safeGetString(dbResult, "ORIGINAL_NAME"), true);
		this.recycledName = JDBCUtils.safeGetString(dbResult, "OBJECT_NAME");
		this.operation = CommonUtils.valueOf(Operation.class, JDBCUtils.safeGetString(dbResult, "OPERATION"));
		this.objectType = JDBCUtils.safeGetString(dbResult, "TYPE");
		this.tablespace = JDBCUtils.safeGetString(dbResult, "TS_NAME");
		this.dropTime = JDBCUtils.safeGetString(dbResult, "RECYCLEBIN_TIME");
		this.partitionName = JDBCUtils.safeGetString(dbResult, "PARTITION_NAME");
	}
	
	@Property(viewable = true, order = 2)
	public String getRecycledName() {
		return recycledName;
	}

	@Property(viewable = true, order = 3)
	public Operation getOperation() {
		return operation;
	}

	@Property(viewable = true, order = 4)
	public String getObjectType() {
		return objectType;
	}

	@Property(viewable = true, order = 5)
	@LazyProperty(cacheValidator = YashanDBTablespace.TablespaceReferenceValidator.class)
	public Object getTablespace(DBRProgressMonitor monitor) throws DBException {
		return YashanDBTablespace.resolveTablespaceReference(monitor, this, null);
	}

	@Property(order = 6)
	public String getDropTime() {
		return dropTime;
	}

	@Property(order = 7)
	public String getPartitionName() {
		return partitionName;
	}

	@Override
	public Object getLazyReference(Object propertyId) {
		return tablespace;
	}

}

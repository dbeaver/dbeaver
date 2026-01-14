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

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.struct.DBSEntityElement;

public class YashanDBDataTypeMember implements DBSEntityElement {

	private YashanDBDataType ownerType;
	protected String name;
	protected boolean inherited;
	protected int no;
	private boolean persisted;

	protected YashanDBDataTypeMember(YashanDBDataType ownerType) {
		this.ownerType = ownerType;
		this.persisted = false;
	}

	protected YashanDBDataTypeMember(YashanDBDataType ownerType, ResultSet dbResult) {
		this.ownerType = ownerType;
		this.persisted = true;
	}

	@NotNull
	@Override
	@Property(viewable = true, editable = true, valueTransformer = DBObjectNameCaseTransformer.class, order = 1)
	public String getName() {
		return name;
	}

	@Property(viewable = true, order = 20)
	public boolean isInherited() {
		return inherited;
	}

	@NotNull
	public YashanDBDataType getOwnerType() {
		return ownerType;
	}

	@Nullable
	@Override
	public String getDescription() {
		return null;
	}

	@NotNull
	@Override
	public YashanDBDataType getParentObject() {
		return ownerType;
	}

	@NotNull
	@Override
	public YashanDBDataSource getDataSource() {
		return ownerType.getDataSource();
	}

	@Override
	public boolean isPersisted() {
		return persisted;
	}

	public int getNo() {
		return no;
	}
}

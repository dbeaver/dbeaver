/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.cubrid.model;

import java.util.Collection;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.generic.model.GenericTableIndex;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;

public class CubridUser implements DBSObject{
	private String name;
	private String comment;
	private CubridObjectContainer container;

	public CubridUser(String name) {
		this.name = name;
	}

	public CubridUser(CubridObjectContainer container, String name, String comment) {
		this.name = name;
		this.comment = comment;
		this.container = container;
	}

	@Property(viewable = true, order = 1)
	public String getName() {
		return name;
	}

	@Nullable
	@Property(viewable = true, order = 2)
	public String getComment() {
		return comment;
	}

	@Override
	public String getDescription() {
		return null;
	}

	@Override
	public boolean isPersisted() {
		return true;
	}

	public boolean supportsSystemTable() {
		return name.equals("DBA") ? true : false;
	}

	public boolean supportsSystemView() {
		return name.equals("DBA") ? true : false;
	}

	public boolean showSystemTableFolder() {
		return this.getDataSource().getContainer().getNavigatorSettings().isShowSystemObjects();
	}

	@Override
	public CubridObjectContainer getParentObject() {
		return this.container;
	}

	public CubridDataSource getDataSource() {
		return this.container.getDataSource();
	}

	public Collection<? extends CubridTableBase> getPhysicalTables(DBRProgressMonitor monitor) throws DBException {
		return this.container.getPhysicalTables(monitor, name);
	}

	public Collection<? extends CubridTableBase> getPhysicalSystemTables(DBRProgressMonitor monitor) throws DBException {
		return this.container.getPhysicalSystemTables(monitor, name);
	}

	public Collection<? extends CubridView> getViews(DBRProgressMonitor monitor) throws DBException {
		return this.container.getViews(monitor, name);
    }

	public Collection<? extends CubridView> getSystemViews(DBRProgressMonitor monitor) throws DBException {
		return this.container.getSystemViews(monitor, name);
    }

	public Collection<? extends GenericTableIndex> getIndexes(DBRProgressMonitor monitor) throws DBException {
		return this.container.getIndexes(monitor);
    }

}
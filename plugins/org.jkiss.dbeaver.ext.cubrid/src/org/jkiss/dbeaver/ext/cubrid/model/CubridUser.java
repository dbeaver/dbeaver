/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.generic.model.GenericDataSource;
import org.jkiss.dbeaver.ext.generic.model.GenericSchema;
import org.jkiss.dbeaver.ext.generic.model.GenericTable;
import org.jkiss.dbeaver.ext.generic.model.GenericView;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.util.ArrayList;
import java.util.List;

public class CubridUser extends GenericSchema
{
    private String name;
    private String comment;

    public CubridUser(GenericDataSource dataSource, String schemaName, String comment) {
        super(dataSource, null, schemaName);
        this.name = schemaName;
        this.comment = comment;
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

    public boolean supportsSystemTable() {
        return name.equals("DBA");
    }

    public boolean supportsSystemView() {
        return name.equals("DBA");
    }

    public boolean showSystemTableFolder() {
        return this.getDataSource().getContainer().getNavigatorSettings().isShowSystemObjects();
    }

    @Override
    public List<CubridTable> getPhysicalTables(DBRProgressMonitor monitor) throws DBException {
        List<CubridTable> tables = new ArrayList<>();
        for (GenericTable table : super.getPhysicalTables(monitor)) {
            if (!table.isSystem()) {
                tables.add((CubridTable) table);
            }
        }
        return tables;
    }

    public List<? extends CubridTable> getPhysicalSystemTables(DBRProgressMonitor monitor)
            throws DBException {
        List<CubridTable> tables = new ArrayList<>();
        for (GenericTable table : super.getPhysicalTables(monitor)) {
            if (table.isSystem()) {
                tables.add((CubridTable) table);
            }
        }
        return tables;
    }

    @Override
    public List<CubridView> getViews(DBRProgressMonitor monitor) throws DBException {
        List<CubridView> views = new ArrayList<>();
        for (GenericView view : super.getViews(monitor)) {
            if (!view.isSystem()) {
                views.add((CubridView) view);
            }
        }
        return views;
    }

    public List<CubridView> getSystemViews(DBRProgressMonitor monitor) throws DBException {
        List<CubridView> views = new ArrayList<>();
        for (GenericView view : super.getViews(monitor)) {
            if (view.isSystem()) {
                views.add((CubridView) view);
            }
        }
        return views;
    }
}

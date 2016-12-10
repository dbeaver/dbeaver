/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.registry;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.DBPDataSourceFolder;
import org.jkiss.dbeaver.model.app.DBPDataSourceRegistry;
import org.jkiss.utils.ArrayUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * DataSourceFolder
 */
public class DataSourceFolder implements DBPDataSourceFolder
{
    private final DataSourceRegistry registry;
    private DataSourceFolder parent;
    private List<DataSourceFolder> children = new ArrayList<>();
    private String name;
    private String description;

    public DataSourceFolder(DataSourceRegistry registry, DataSourceFolder parent, String name, String description) {
        this.registry = registry;
        this.name = name;
        this.description = description;
        setParent(parent);
    }

    @NotNull
    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String newName) {
        this.name = newName;
    }

    @Override
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public DataSourceFolder getParent() {
        return parent;
    }

    public void setParent(DataSourceFolder parent) {
        if (this.parent != null) {
            this.parent.children.remove(this);
        }
        this.parent = parent;
        if (this.parent != null) {
            this.parent.children.add(this);
        }
    }

    @Override
    public DataSourceFolder[] getChildren() {
        return ArrayUtils.toArray(DataSourceFolder.class, children);
    }

    @Override
    public DBPDataSourceRegistry getDataSourceRegistry() {
        return registry;
    }

    public DataSourceFolder getChild(String name) {
        for (DataSourceFolder child : children) {
            if (child.getName().equals(name)) {
                return child;
            }
        }
        return null;
    }

    @Override
    public String getFolderPath() {
        String path = null;
        for (DataSourceFolder folder = this; folder != null; folder = folder.getParent()) {
            path = path == null ? folder.getName() : folder.getName() + "/" + path;
        }
        return path;
    }

    @Override
    public String toString() {
        return getFolderPath();
    }
}

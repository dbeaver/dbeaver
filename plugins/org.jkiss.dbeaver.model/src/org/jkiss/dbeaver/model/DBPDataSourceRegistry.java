/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
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

package org.jkiss.dbeaver.model;

import org.eclipse.core.resources.IProject;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.struct.DBSDataSourceContainer;

import java.util.List;

/**
 * DBPDataSourceRegistry
 */
public interface DBPDataSourceRegistry {

    /**
     * Owner project.
     */
    IProject getProject();

    @Nullable
    DBSDataSourceContainer getDataSource(String id);

    @Nullable
    DBSDataSourceContainer getDataSource(DBPDataSource dataSource);

    @Nullable
    DBSDataSourceContainer findDataSourceByName(String name);

    public List<? extends DBSDataSourceContainer> getDataSources();

    void addDataSourceListener(DBPEventListener listener);

    boolean removeDataSourceListener(DBPEventListener listener);

    void flushConfig();

}

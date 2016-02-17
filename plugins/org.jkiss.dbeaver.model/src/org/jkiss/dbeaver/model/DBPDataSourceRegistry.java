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

package org.jkiss.dbeaver.model;

import org.eclipse.core.resources.IProject;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;

import java.util.List;

/**
 * DBPDataSourceRegistry
 */
public interface DBPDataSourceRegistry {

    String CONFIG_FILE_PREFIX = ".dbeaver-data-sources"; //$NON-NLS-1$
    String CONFIG_FILE_EXT = ".xml"; //$NON-NLS-1$
    String CONFIG_FILE_NAME = CONFIG_FILE_PREFIX + CONFIG_FILE_EXT;

    @NotNull
    DBPApplication getApplication();
    /**
     * Owner project.
     */
    IProject getProject();

    @Nullable
    DBPDataSourceContainer getDataSource(String id);

    @Nullable
    DBPDataSourceContainer getDataSource(DBPDataSource dataSource);

    @Nullable
    DBPDataSourceContainer findDataSourceByName(String name);

    List<? extends DBPDataSourceContainer> getDataSources();

    void addDataSourceListener(DBPEventListener listener);

    boolean removeDataSourceListener(DBPEventListener listener);

    void addDataSource(DBPDataSourceContainer dataSource);

    void removeDataSource(DBPDataSourceContainer dataSource);

    void updateDataSource(DBPDataSourceContainer dataSource);

    void flushConfig();

    void refreshConfig();

    void notifyDataSourceListeners(final DBPEvent event);

}

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

package org.jkiss.dbeaver.ui;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.DBPDataSourceRegistry;
import org.jkiss.dbeaver.model.DBPDriver;
import org.jkiss.dbeaver.model.runtime.DBRRunnableContext;
import org.jkiss.dbeaver.model.struct.DBSDataSourceContainer;

/**
 * IDataSourceConnectionEditorSite
 */
public interface IDataSourceConnectionEditorSite
{
    DBRRunnableContext getRunnableContext();

    DBPDataSourceRegistry getDataSourceRegistry();

    boolean isNew();

    DBPDriver getDriver();

    @NotNull
    DBSDataSourceContainer getActiveDataSource();

    void updateButtons();

    boolean openDriverEditor();

}

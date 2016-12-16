/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 * Copyright (C) 2011-2012 Eugene Fradkin (eugene.fradkin@gmail.com)
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
package org.jkiss.dbeaver.ui.controls.resultset;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCExecutionSource;
import org.jkiss.dbeaver.model.struct.DBSDataContainer;
import org.jkiss.dbeaver.runtime.jobs.DataSourceJob;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;

abstract class ResultSetJobAbstract extends DataSourceJob implements DBCExecutionSource {

    protected final DBSDataContainer dataContainer;
    protected final ResultSetViewer controller;

    protected ResultSetJobAbstract(String name, DBSDataContainer dataContainer, ResultSetViewer controller, DBCExecutionContext executionContext) {
        super(name, DBeaverIcons.getImageDescriptor(UIIcon.SQL_EXECUTE), executionContext);
        this.dataContainer = dataContainer;
        this.controller = controller;
        setUser(false);
    }

    @Nullable
    @Override
    public DBSDataContainer getDataContainer() {
        return dataContainer;
    }

    @NotNull
    @Override
    public Object getExecutionController() {
        return controller;
    }

    @Nullable
    @Override
    public Object getSourceDescriptor() {
        return this;
    }

}

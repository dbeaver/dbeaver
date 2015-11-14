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
package org.jkiss.dbeaver.model.impl;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCExecutionSource;
import org.jkiss.dbeaver.model.struct.DBSDataContainer;

/**
 * AbstractExecutionSource
 */
public class AbstractExecutionSource implements DBCExecutionSource {

    private final DBSDataContainer dataContainer;
    private final DBCExecutionContext executionContext;
    private final Object controller;
    private final Object descriptor;

    public AbstractExecutionSource(DBSDataContainer dataContainer, DBCExecutionContext executionContext, Object controller) {
        this(dataContainer, executionContext, controller, null);
    }

    public AbstractExecutionSource(DBSDataContainer dataContainer, DBCExecutionContext executionContext, Object controller, Object descriptor) {
        this.dataContainer = dataContainer;
        this.executionContext = executionContext;
        this.controller = controller;
        this.descriptor = descriptor;
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
        return descriptor;
    }
}

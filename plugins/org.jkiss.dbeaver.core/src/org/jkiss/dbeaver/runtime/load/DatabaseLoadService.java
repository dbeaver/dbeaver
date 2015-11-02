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
package org.jkiss.dbeaver.runtime.load;

import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.struct.DBSWrapper;

/**
 * Lazy loading service
 * @param <RESULT> result type
 */
public abstract class DatabaseLoadService<RESULT> extends AbstractLoadService<RESULT> {

    private DBPDataSource dataSource;

    protected DatabaseLoadService(String serviceName, DBCExecutionContext context) {
        super(serviceName);
        this.dataSource = context.getDataSource();
    }

    protected DatabaseLoadService(String serviceName, DBPDataSource dataSource) {
        super(serviceName);
        this.dataSource = dataSource;
    }

    protected DatabaseLoadService(String serviceName, DBSWrapper wrapper) {
        this(serviceName, wrapper == null || wrapper.getObject() == null ? null : wrapper.getObject().getDataSource());
    }

    @Override
    public Object getFamily() {
        return dataSource;
    }
}
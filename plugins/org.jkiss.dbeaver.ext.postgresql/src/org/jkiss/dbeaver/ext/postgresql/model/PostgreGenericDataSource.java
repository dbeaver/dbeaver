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
package org.jkiss.dbeaver.ext.postgresql.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.generic.model.GenericDataSource;
import org.jkiss.dbeaver.ext.postgresql.model.generic.PostgreMetaModel;
import org.jkiss.dbeaver.ext.postgresql.model.jdbc.PostgreJdbcFactory;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCFactory;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

/**
 * PostgreGenericDataSource
 */
@Deprecated
public class PostgreGenericDataSource extends GenericDataSource
{

    public PostgreGenericDataSource(DBRProgressMonitor monitor, DBPDataSourceContainer container, PostgreMetaModel metaModel) throws DBException {
        super(monitor, container, metaModel);
    }
//    public PostgreDataSource(DBRProgressMonitor monitor, DBPDataSourceContainer container) throws DBException {
//        super(monitor, container, new PostgreMetaModel());
//    }

    @NotNull
    @Override
    protected JDBCFactory createJdbcFactory() {
        return new PostgreJdbcFactory();
    }

}

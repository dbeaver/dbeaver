/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2016-2016 Karl Griesser (fullref@gmail.com)
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
package org.jkiss.dbeaver.ext.exasol.model;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPSaveableObject;
import org.jkiss.dbeaver.model.struct.DBSObject;

public class ExasolGlobalObject implements DBSObject, DBPSaveableObject {

    private final ExasolDataSource dataSource;
    private boolean persisted;

    protected ExasolGlobalObject(ExasolDataSource dataSource, boolean persisted) {
        this.dataSource = dataSource;
        this.persisted = persisted;
    }


    @Nullable
    @Override
    public String getName() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isPersisted() {
        // TODO Auto-generated method stub
        return persisted;
    }

    @Override
    public void setPersisted(boolean persisted) {

        this.persisted = persisted;

    }

    @Override
    public String getDescription() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public DBSObject getParentObject() {
        // TODO Auto-generated method stub
        return dataSource.getContainer();
    }

    @Override
    public DBPDataSource getDataSource() {
        // TODO Auto-generated method stub
        return dataSource;
    }

}

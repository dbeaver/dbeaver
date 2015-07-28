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
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.util.List;

/**
 * Simple objects cache.
 * Doesn't provide any reading facilities, just stores objects
 */
public class SimpleObjectCache<OWNER extends DBSObject, OBJECT extends DBSObject> extends AbstractObjectCache<OWNER, OBJECT> {

    @NotNull
    @Override
    public List<OBJECT> getAllObjects(@NotNull DBRProgressMonitor monitor, @Nullable OWNER owner)
        throws DBException
    {
        return getCachedObjects();
    }

    @Nullable
    @Override
    public OBJECT getObject(@NotNull DBRProgressMonitor monitor, @Nullable OWNER owner, @NotNull String name)
    {
        return getCachedObject(name);
    }
}

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

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.util.Collection;

/**
 * Structure objects cache
 */
public interface DBSStructCache<OWNER extends DBSObject, OBJECT extends DBSObject, CHILD extends DBSObject>
    extends DBSObjectCache<OWNER, OBJECT>
{

    DBSObjectCache<OBJECT, CHILD> getChildrenCache(final OBJECT forObject);

    @Nullable
    Collection<CHILD> getChildren(DBRProgressMonitor monitor, OWNER owner, final OBJECT forObject)
        throws DBException;

    @Nullable
    CHILD getChild(DBRProgressMonitor monitor, OWNER owner, final OBJECT forObject, String objectName)
        throws DBException;

    void clearChildrenCache(OBJECT forParent);
}

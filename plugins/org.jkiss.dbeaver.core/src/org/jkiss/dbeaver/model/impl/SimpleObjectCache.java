/*
 * Copyright (C) 2010-2012 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.model.impl;

import org.jkiss.dbeaver.model.impl.AbstractObjectCache;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.util.Collection;

/**
 * Simple objects cache.
 * Doesn't provide any reading facilities, just stores objects
 */
public class SimpleObjectCache<OWNER extends DBSObject, OBJECT extends DBSObject> extends AbstractObjectCache<OWNER, OBJECT> {

    @Override
    public Collection<OBJECT> getObjects(DBRProgressMonitor monitor, OWNER owner)
    {
        return getCachedObjects();
    }

    @Override
    public OBJECT getObject(DBRProgressMonitor monitor, OWNER owner, String name)
    {
        return getCachedObject(name);
    }
}

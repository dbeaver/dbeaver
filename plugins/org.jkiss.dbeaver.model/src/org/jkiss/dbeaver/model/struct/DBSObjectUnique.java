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

package org.jkiss.dbeaver.model.struct;

import org.jkiss.code.NotNull;

/**
 * Object with unique name.
 * Generally all objects have unique name (in context of their parent objects) but sometimes the name isn't unique.
 * For example stored procedures can be overridden, as a result multiple procedures have the same name.
 * Such objects may implements this interface to provide really unique name.
 * Unique name used in some operations like object tree refresh.
 */
public interface DBSObjectUnique extends DBSObject
{

    /**
     * Object's unique name
     *
     * @return object unique name
     */
    @NotNull
    String getUniqueName();

}

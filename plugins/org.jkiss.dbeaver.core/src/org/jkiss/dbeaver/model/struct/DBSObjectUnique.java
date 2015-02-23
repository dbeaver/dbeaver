/*
 * Copyright (C) 2010-2015 Serge Rieder
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

package org.jkiss.dbeaver.model.struct;

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
    String getUniqueName();

}

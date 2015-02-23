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
package org.jkiss.dbeaver.model.data;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;

/**
 * DBD Value Controller
 */
public interface DBDAttributeController extends DBDValueController
{

    /**
     * Row controller
     * @return row controller
     */
    @NotNull
    DBDRowController getRowController();

    /**
     * Attribute meta data
     * @return meta data
     */
    @NotNull
    DBDAttributeBinding getBinding();

    /**
     * Column unique ID string
     * @return string
     */
    @NotNull
    String getColumnId();

    /**
     * Row identifier
     */
    @Nullable
    DBDRowIdentifier getRowIdentifier();

}

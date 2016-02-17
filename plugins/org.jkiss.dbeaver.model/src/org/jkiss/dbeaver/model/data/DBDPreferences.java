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

package org.jkiss.dbeaver.model.data;

import org.jkiss.code.NotNull;

/**
 * Data preferences
 */
public interface DBDPreferences {

    /**
     * Gets current context's data formatter profile
     * @return profile
     */
    DBDDataFormatterProfile getDataFormatterProfile();

    /**
     * Sets current context's data formatter profile
     */
    void setDataFormatterProfile(DBDDataFormatterProfile formatterProfile);

    /**
     * Default value handler
     * @return value handler instance
     */
    @NotNull
    DBDValueHandler getDefaultValueHandler();

}

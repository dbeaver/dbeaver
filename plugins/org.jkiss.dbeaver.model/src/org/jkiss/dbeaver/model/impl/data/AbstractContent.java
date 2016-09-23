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
package org.jkiss.dbeaver.model.impl.data;

import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.data.DBDContent;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;

/**
 * AbstractContent
 *
 * @author Serge Rider
 */
public abstract class AbstractContent implements DBDContent {

    protected final DBPDataSource dataSource;

    protected AbstractContent(DBPDataSource dataSource)
    {
        this.dataSource = dataSource;
    }

    @Override
    public void resetContents()
    {
        // do nothing
    }

    @Override
    public String toString()
    {
        String displayString = getDisplayString(DBDDisplayFormat.UI);
        return displayString == null ? DBConstants.NULL_VALUE_LABEL : displayString;
    }

}

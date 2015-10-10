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
package org.jkiss.dbeaver.registry.driver;

import org.jkiss.dbeaver.model.connection.DBPDriverDependencies;
import org.jkiss.dbeaver.model.connection.DBPDriverLibrary;

import java.util.Collection;
import java.util.List;

/**
 * DriverDependencies
 */
public class DriverDependencies implements DBPDriverDependencies
{
    public DriverDependencies(Collection<? extends DBPDriverLibrary> rootLibraries) {
    }

    @Override
    public List<DBPDriverLibrary> getLibraryList() {
        return null;
    }

    @Override
    public List<DependencyNode> getLibraryMap() {
        return null;
    }

}

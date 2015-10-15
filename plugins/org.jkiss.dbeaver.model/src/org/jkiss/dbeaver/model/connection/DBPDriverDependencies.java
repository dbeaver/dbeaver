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

package org.jkiss.dbeaver.model.connection;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.util.ArrayList;
import java.util.List;

/**
 * Driver library dependencies
 */
public interface DBPDriverDependencies
{
    class DependencyNode {
        public final DependencyNode owner;
        public final DBPDriverLibrary library;
        public final List<DependencyNode> dependencies = new ArrayList<>();
        public final int depth;
        public boolean duplicate;

        public DependencyNode(DependencyNode owner, DBPDriverLibrary library) {
            this.owner = owner;
            this.library = library;
            this.depth = owner == null ? 0 : owner.depth + 1;
        }

        @Override
        public String toString() {
            return library.getPath();
        }
    }

    List<DependencyNode> getLibraryList();

    List<DependencyNode> getLibraryMap();

    void resolveDependencies(DBRProgressMonitor monitor) throws DBException;
}

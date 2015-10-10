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

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.connection.DBPDriverDependencies;
import org.jkiss.dbeaver.model.connection.DBPDriverLibrary;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.io.IOException;
import java.util.*;

/**
 * DriverDependencies
 */
public class DriverDependencies implements DBPDriverDependencies
{
    public DriverDependencies() {
    }

    void resolveDependencies(DBRProgressMonitor monitor, Collection<? extends DBPDriverLibrary> rootLibraries) throws DBException {
        try {
            // Dependency map. Key is artifact version (exact)
            final Map<String, List<DBPDriverLibrary>> depMap = new LinkedHashMap<>();
            for (DBPDriverLibrary library : rootLibraries) {
                resolveDependencies(monitor, library, null, depMap);
            }

/*
            // Replace multiple versions of the same artifact with the first found one
            Map<String, DBPDriverLibrary> flatDependencies = new LinkedHashMap<>();
            List<DependencyNode> nodes = new ArrayList<>();
            for (Map.Entry<String, List<DBPDriverLibrary>> entry : depMap.entrySet()) {

            }
*/

        } catch (IOException e) {
            throw new DBException("IO error while resolving dependencies", e);
        }
    }

    private void resolveDependencies(DBRProgressMonitor monitor, DBPDriverLibrary library, DBPDriverLibrary ownerLibrary, Map<String, List<DBPDriverLibrary>> depMap) throws IOException {
        String libraryPath = library.getPath();
        List<DBPDriverLibrary> deps = depMap.get(libraryPath);
        if (deps != null) {
            return;
        }

        deps = new ArrayList<>();
        depMap.put(libraryPath, deps);

        Collection<? extends DBPDriverLibrary> dependencies = library.getDependencies(monitor, ownerLibrary);
        if (dependencies != null && !dependencies.isEmpty()) {
            for (DBPDriverLibrary dep : dependencies) {
                deps.add(dep);
                resolveDependencies(monitor, dep, library, depMap);
            }
        }
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

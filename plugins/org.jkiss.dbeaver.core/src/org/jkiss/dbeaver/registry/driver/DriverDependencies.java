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
import org.jkiss.dbeaver.model.connection.DBPDriverContext;

import java.io.IOException;
import java.util.*;

/**
 * DriverDependencies
 */
public class DriverDependencies implements DBPDriverDependencies
{
    final List<? extends DBPDriverLibrary> rootLibraries;
    final List<DependencyNode> rootNodes = new ArrayList<>();
    final List<DBPDriverLibrary> libraryList = new ArrayList<>();

    public DriverDependencies(List<? extends DBPDriverLibrary> rootLibraries) {
        this.rootLibraries = rootLibraries;
    }

    @Override
    public void resolveDependencies(DBPDriverContext context) throws DBException {
        try {
            {
                rootNodes.clear();

                final Map<String, DBPDriverLibrary> libMap = new LinkedHashMap<>();
                for (DBPDriverLibrary library : rootLibraries) {
                    DependencyNode node = new DependencyNode(null, library);
                    libMap.put(node.library.getId(), node.library);

                    resolveDependencies(context, node, libMap);
                    rootNodes.add(node);
                }
                libraryList.clear();
                libraryList.addAll(libMap.values());

/*
                    StringBuilder sb = new StringBuilder();
                    Set<String> ns = new TreeSet<>();
                    for (String lib : libMap.keySet()) {
                        String newName = lib.replaceAll(".+\\:", "");
                        if (ns.contains(newName)) {
                            //System.out.println(123);
                        }
                        ns.add(newName);
                    }
                    for (String lib : ns) {
                        sb.append(lib).append("\n");
                    }
                    System.out.println(sb.toString());
*/
/*
                    System.out.println("---------------------------");
                    for (DependencyNode node : rootNodes) {
                        dumpNode(node, 0);
                    }
*/
            }
        } catch (IOException e) {
            throw new DBException("IO error while resolving dependencies", e);
        }
    }

    private void dumpNode(DependencyNode node, int level) {
        if (node.duplicate) {
            return;
        }
        for (int i = 0; i < level; i++) System.out.print("\t");
        System.out.println(node.library.getId() + ":" + node.library.getVersion());
        for (DependencyNode child : node.dependencies) {
            dumpNode(child, level + 1);
        }
    }

    private void resolveDependencies(DBPDriverContext context, DependencyNode ownerNode, Map<String, DBPDriverLibrary> libMap) throws IOException {
        ownerNode.library.resolve(context);
        Collection<? extends DBPDriverLibrary> dependencies = ownerNode.library.getDependencies(context);
        if (dependencies != null && !dependencies.isEmpty()) {
            for (DBPDriverLibrary dep : dependencies) {
                DependencyNode node = new DependencyNode(ownerNode, dep);

                node.duplicate = libMap.containsKey(node.library.getId());
                if (!node.duplicate) {
                    libMap.put(node.library.getId(), node.library);
                }
                ownerNode.dependencies.add(node);
            }
            for (DependencyNode node : ownerNode.dependencies) {
                if (!node.duplicate) {
                    resolveDependencies(context, node, libMap);
                }
            }
        }
    }

    @Override
    public List<DBPDriverLibrary> getLibraryList() {
        return libraryList;
    }

    @Override
    public List<DependencyNode> getLibraryMap() {
        return rootNodes;
    }

}

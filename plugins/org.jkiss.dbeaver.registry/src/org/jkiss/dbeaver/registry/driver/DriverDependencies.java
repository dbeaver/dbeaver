/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.registry.driver;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
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
    private static final Log log = Log.getLog(DriverDependencies.class);

    private final List<DBPDriverLibrary> rootLibraries;
    private final List<DependencyNode> rootNodes = new ArrayList<>();
    private final List<DependencyNode> libraryList = new ArrayList<>();

    public DriverDependencies(Collection<? extends DBPDriverLibrary> rootLibraries) {
        this.rootLibraries = new ArrayList<>(rootLibraries);
    }

    @Override
    public void resolveDependencies(DBRProgressMonitor monitor) throws DBException {
        IOException lastError = null;
        {
            rootNodes.clear();

            final Map<String, DependencyNode> libMap = new LinkedHashMap<>();
            for (DBPDriverLibrary library : rootLibraries) {
                DependencyNode node = new DependencyNode(null, library);

                try {
                    final Map<String, DependencyNode> localLibMap = new LinkedHashMap<>();
                    localLibMap.put(node.library.getId(), node);

                    resolveDependencies(monitor, node, localLibMap);

                    rootNodes.add(node);
                    libMap.putAll(localLibMap);
                } catch (IOException e) {
                    lastError = e;
                    log.error("Error resolving library '" + library.getDisplayName() + "' dependencies", e);
                }
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

                System.out.println("---------------------------");
                for (DependencyNode node : rootNodes) {
                    dumpNode(node, 0);
                }
*/
        }
        if (lastError != null) {
            throw new DBException("Error resolving dependencies", lastError);
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

    private void resolveDependencies(DBRProgressMonitor monitor, DependencyNode ownerNode, Map<String, DependencyNode> libMap) throws IOException {
        Collection<? extends DBPDriverLibrary> dependencies = ownerNode.library.getDependencies(monitor);
        if (dependencies != null && !dependencies.isEmpty()) {
            for (DBPDriverLibrary dep : dependencies) {
                DependencyNode node = new DependencyNode(ownerNode, dep);

                DependencyNode prevNode = libMap.get(node.library.getId());
                if (prevNode == null || prevNode.depth > node.depth) {
                    //if (node.library.isDownloadable()) {
                        libMap.put(node.library.getId(), node);
                    //}
                    if (prevNode != null) {
                        prevNode.duplicate = true;
                    }
                } else {
                    node.duplicate = true;
                }
                ownerNode.dependencies.add(node);
            }
            for (DependencyNode node : ownerNode.dependencies) {
                if (!node.duplicate) {
                    resolveDependencies(monitor, node, libMap);
                }
            }
        }
    }

    @Override
    public List<DependencyNode> getLibraryList() {
        return libraryList;
    }

    @Override
    public List<DependencyNode> getLibraryMap() {
        return rootNodes;
    }

    public void changeLibrary(DBPDriverLibrary oldLibrary, DBPDriverLibrary newLibrary) {
        int index = rootLibraries.indexOf(oldLibrary);
        if (index == -1) {
            rootLibraries.add(newLibrary);
        } else {
            rootLibraries.add(index, newLibrary);
            rootLibraries.remove(oldLibrary);
        }
    }
}

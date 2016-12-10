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
package org.jkiss.dbeaver.ui.navigator.database.load;

import org.jkiss.dbeaver.DBeaverPreferences;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.navigator.DBNContainer;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.runtime.load.DatabaseLoadService;
import org.jkiss.utils.ArrayUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * TreeLoadService
 */
public class TreeLoadService extends DatabaseLoadService<Object[]> {

    private DBNNode parentNode;

    public TreeLoadService(String serviceName, DBNDatabaseNode parentNode)
    {
        super(serviceName, parentNode);
        this.parentNode = parentNode;
    }

    public DBNNode getParentNode() {
        return parentNode;
    }

    @Override
    public Object[] evaluate()
        throws InvocationTargetException, InterruptedException
    {
        try {
            DBNNode[] children = parentNode.getChildren(getProgressMonitor());
            children = filterNavigableChildren(children);
            return children == null ? new Object[0] : children;
        } catch (Throwable ex) {
            throw new InvocationTargetException(ex);
        }
    }

    public static DBNNode[] filterNavigableChildren(DBNNode[] children)
    {
        if (ArrayUtils.isEmpty(children)) {
            return children;
        }
        List<DBNNode> filtered = null;
        for (int i = 0; i < children.length; i++) {
            DBNNode node = children[i];
            if (node instanceof DBNDatabaseNode && !((DBNDatabaseNode) node).getMeta().isNavigable()) {
                if (filtered == null) {
                    filtered = new ArrayList<>(children.length);
                    for (int k = 0; k < i; k++) {
                        filtered.add(children[k]);
                    }
                }
            } else if (filtered != null) {
                filtered.add(node);
            }
        }
        DBNNode[] result = filtered == null ? children : filtered.toArray(new DBNNode[filtered.size()]);
        sortChildren(result);
        return result;
    }

    public static void sortChildren(DBNNode[] children)
    {
        final DBPPreferenceStore prefStore = DBeaverCore.getGlobalPreferenceStore();

        // Sort children is we have this feature on in preferences
        // and if children are not folders
        if (children.length > 0 && prefStore.getBoolean(DBeaverPreferences.NAVIGATOR_SORT_ALPHABETICALLY)) {
            if (!(children[0] instanceof DBNContainer)) {
                Arrays.sort(children, NodeNameComparator.INSTANCE);
            }
        }

        if (children.length > 0 && prefStore.getBoolean(DBeaverPreferences.NAVIGATOR_SORT_FOLDERS_FIRST)) {
            Arrays.sort(children, NodeFolderComparator.INSTANCE);
        }
    }

    private static class NodeNameComparator implements Comparator<DBNNode> {
        static NodeNameComparator INSTANCE = new NodeNameComparator();
        @Override
        public int compare(DBNNode node1, DBNNode node2) {
            return node1.getNodeName().compareToIgnoreCase(node2.getNodeName());
        }
    }

    private static class NodeFolderComparator implements Comparator<DBNNode> {
        static NodeFolderComparator INSTANCE = new NodeFolderComparator();
        @Override
        public int compare(DBNNode node1, DBNNode node2) {
            int first = node1 instanceof DBNContainer ? -1 : 1;
            int second = node2 instanceof DBNContainer ? -1 : 1;
            return first - second;
        }
    }
}

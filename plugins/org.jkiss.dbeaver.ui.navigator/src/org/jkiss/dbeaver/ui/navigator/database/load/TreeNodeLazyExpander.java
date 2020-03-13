/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.navigator.database.load;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Tree;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.navigator.NavigatorPreferences;
import org.jkiss.dbeaver.ui.navigator.database.DatabaseNavigatorTree;


public class TreeNodeLazyExpander extends TreeNodeSpecial {

    private static Image IMG_MORE = DBeaverIcons.getImage(UIIcon.REFRESH);

    private DBNNode[] allChildren;
    private int visibleChildren;

    public TreeNodeLazyExpander(DBNNode parent, DBNNode[] allChildren, int visibleChildren) {
        super(parent);
        this.allChildren = allChildren;
        this.visibleChildren = visibleChildren;
    }

    public int getVisibleChildren() {
        return visibleChildren;
    }

    @Override
    public String getText(Object element) {
        return "More ... (" + visibleChildren + "/" + allChildren.length + ")";
    }

    @Override
    public Image getImage(Object element) {
        return IMG_MORE;
    }

    @Override
    public boolean handleDefaultAction(DatabaseNavigatorTree tree) {
        int longListFetchSize = Math.max(NavigatorPreferences.MIN_LONG_LIST_FETCH_SIZE, DBWorkbench.getPlatform().getPreferenceStore().getInt(NavigatorPreferences.NAVIGATOR_LONG_LIST_FETCH_SIZE));
        boolean lastSegment = visibleChildren + longListFetchSize > allChildren.length;
        int nextSegmentSize = lastSegment ? allChildren.length - visibleChildren : longListFetchSize;
        Object[] nodes = new Object[lastSegment ? nextSegmentSize : nextSegmentSize + 1];
        System.arraycopy(allChildren, visibleChildren, nodes, 0, nextSegmentSize);
        if (!lastSegment) {
            nodes[nextSegmentSize] = new TreeNodeLazyExpander(getParent(), allChildren, visibleChildren + nextSegmentSize);
        }
        Tree treeControl = tree.getViewer().getTree();
        treeControl.setRedraw(false);
        try {
            tree.getViewer().remove(this);
            tree.getViewer().add(getParent(), nodes);
        } finally {
            treeControl.setRedraw(true);
        }
        return true;
    }

}
/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.navigator.database;

import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.ui.DefaultViewerToolTipSupport;

class DatabaseNavigatorToolTipSupport extends DefaultViewerToolTipSupport {
    private DatabaseNavigatorTree databaseNavigatorTree;

    DatabaseNavigatorToolTipSupport(DatabaseNavigatorTree databaseNavigatorTree) {
        super(databaseNavigatorTree.getViewer());
        this.databaseNavigatorTree = databaseNavigatorTree;
    }

    @Override
    protected boolean shouldCreateToolTip(Event event) {
        Tree tree = (Tree) event.widget;
        TreeItem item = tree.getItem(new Point(event.x, event.y));
        if (item == null) {
            return false;
        }
        if (item.getData() instanceof DBNNode node) {
            tree.setToolTipText(databaseNavigatorTree.getItemRenderer().getToolTipText(node, tree, event));
        }
        return false;
    }

    @Override
    protected Object getToolTipArea(Event event) {
        TreeItem item = ((Tree) event.widget).getItem(new Point(event.x, event.y));
        if (item == null) {
            return false;
        }
        Rectangle bounds = item.getBounds(0);
        if (event.x >= bounds.x && event.x <= bounds.x + bounds.width) {
            // Main area
            return super.getToolTipArea(event);
        } else {
            return null;
        }
    }

    @Override
    protected String getText(Event event) {
        return super.getText(event);
    }
}

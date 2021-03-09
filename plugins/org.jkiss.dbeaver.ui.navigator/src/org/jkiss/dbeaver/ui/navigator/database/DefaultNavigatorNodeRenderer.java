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

package org.jkiss.dbeaver.ui.navigator.database;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.navigator.DBNDataSource;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.navigator.INavigatorItemRenderer;

/**
 * Default node renderer.
 * Draws connection type marker next to the item name.
 */
public class DefaultNavigatorNodeRenderer implements INavigatorItemRenderer {

    @Override
    public void drawNodeBackground(DBNNode node, Tree tree, GC gc, Event event) {
//        Color conColor = null;
//        Object element = event.item.getData();
//        if (element instanceof DBNDataSource) {
//            DBPDataSourceContainer ds = ((DBNDataSource) element).getDataSourceContainer();
//            conColor = UIUtils.getConnectionColor(ds.getConnectionConfiguration());
//        }
//        if (conColor != null) {
//
//        }
    }

    public void paintNodeDetails(DBNNode node, Tree tree, GC gc, Event event) {
        Color conColor = null;
        Object element = event.item.getData();
        if (element instanceof DBNDatabaseNode) {
            DBPDataSourceContainer ds = ((DBNDatabaseNode) element).getDataSourceContainer();
            conColor = UIUtils.getConnectionColor(ds.getConnectionConfiguration());
        }

        if (conColor != null) {
            gc.setForeground(conColor);
            if (element instanceof DBNDataSource) {
                //int boxSize = event.height - 4;

                //gc.setLineWidth(2);
                //gc.setLineStyle(SWT.LINE_SOLID);

                //            gc.fillRectangle(0, event.y + 2, textSize.x, textSize.y);
                //            gc.drawRectangle(0, event.y + 2, textSize.x - 1, textSize.y - 1);
                //gc.drawRectangle(event.x - 2, event.y + 1, event.width + 3, event.height - 2/*, event.height / 2, event.height / 2*/);
                //gc.drawLine(event.x, event.y, event.x + event.width, event.y);
                //gc.drawLine(10, event.y + event.height - 1, event.x + event.width, event.y + event.height - 1);
                //gc.drawLine(event.x, event.y + event.height - 1, event.x + event.width, event.y + event.height - 1);
            } else {
                int oldLineWidth = gc.getLineWidth();

                gc.setForeground(conColor);
                gc.setLineWidth(3);
                if (((TreeItem)event.item).getItemCount() > 0) {
                    gc.drawLine(event.x - 20, event.y - 1, event.x - 20, event.y + event.height + 1);
                } else {
                    gc.drawLine(event.x - 4, event.y - 1, event.x - 4, event.y + event.height + 1);
                }
                gc.setLineWidth(oldLineWidth);
            }
        }
    }

    @Override
    public void showDetailsToolTip(DBNNode node, Tree tree, Event event) {

    }

    @Override
    public void performAction(DBNNode node, Tree tree, Event event, boolean defaultAction) {

    }

    @Override
    public void handleHover(DBNNode node, Tree tree, TreeItem item, Event event) {

    }

}

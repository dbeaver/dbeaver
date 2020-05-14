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

package org.jkiss.dbeaver.ui.navigator.database;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Tree;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.navigator.DBNDataSource;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.ui.UIUtils;

/**
 * Default node renderer.
 * Draws connection type marker next to the item name.
 * Draws item statistics in the right part.
 */
public class DefaultNavigatorNodeRenderer implements DatabaseNavigatorItemRenderer {

    public static final int STAT_RECT_WIDTH = 50;

    public void paintNodeDetails(DBNNode node, Tree tree, GC gc, Event event) {
        Color conColor = null;
        Object element = event.item.getData();
        if (element instanceof DBNDataSource) {
            DBPDataSourceContainer ds = ((DBNDataSource) element).getDataSourceContainer();
            conColor = UIUtils.getConnectionColor(ds.getConnectionConfiguration());
        }

        int treeWidth = tree.getClientArea().width;
        int occupiedWidth = 0;
        if (conColor != null) {
            int boxSize = event.height - 4;
            int x = event.x + event.width + 4;

            Point textSize = new Point(boxSize, boxSize);
            Color fg = tree.getDisplay().getSystemColor(SWT.COLOR_WIDGET_NORMAL_SHADOW);
            gc.setForeground(fg);
            gc.setBackground(conColor);

            gc.fillRectangle(x, event.y + 2, textSize.x, textSize.y);
            gc.drawRectangle(x, event.y + 2, textSize.x - 1, textSize.y - 1);
            //gc.drawText(colorSettings, x, event.y);
            x += textSize.x + 4;
            occupiedWidth = x;
        }

        if (false && treeWidth - occupiedWidth > STAT_RECT_WIDTH) {
            // Draw something custom
            gc.setForeground(tree.getForeground());
            int x = treeWidth - STAT_RECT_WIDTH;
            gc.drawText("Stats", x + 2, event.y, true);
            gc.drawRectangle(x, event.y + 2, STAT_RECT_WIDTH - 2, event.height - 4);
        }
    }

}

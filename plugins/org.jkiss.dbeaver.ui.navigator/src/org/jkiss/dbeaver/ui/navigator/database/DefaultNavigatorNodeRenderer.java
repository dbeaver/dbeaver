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
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Tree;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseFolder;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.ui.UIUtils;

/**
 * Default node renderer.
 * Draws connection type marker next to the item name.
 */
public class DefaultNavigatorNodeRenderer implements DatabaseNavigatorItemRenderer {

    public void paintNodeDetails(DBNNode node, Tree tree, GC gc, Event event) {
        Color conColor = null;
        Object element = event.item.getData();
        if (element instanceof DBNDatabaseNode && !(element instanceof DBNDatabaseFolder)) {
            DBPDataSourceContainer ds = ((DBNDatabaseNode) element).getDataSourceContainer();
            conColor = UIUtils.getConnectionColor(ds.getConnectionConfiguration());
        }

        if (conColor != null) {
            int boxSize = event.height - 4;

            //gc.setForeground(fg);
            //gc.setBackground(conColor);
            gc.setForeground(conColor);
            gc.setLineWidth(1);
            gc.setLineStyle(SWT.LINE_SOLID);

//            gc.fillRectangle(0, event.y + 2, textSize.x, textSize.y);
//            gc.drawRectangle(0, event.y + 2, textSize.x - 1, textSize.y - 1);
            gc.drawRectangle(event.x - 2, event.y + 1, event.width + 3, event.height - 2/*, event.height / 2, event.height / 2*/);
        }
    }
}

/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2022 DBeaver Corp and others
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

package org.jkiss.dbeaver.ui.navigator;

import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.jkiss.dbeaver.model.navigator.DBNNode;

/**
 * Tree item renderer
 */
public interface INavigatorItemRenderer {

    void drawNodeBackground(DBNNode element, Tree tree, GC gc, Event event);

    void paintNodeDetails(DBNNode node, Tree tree, GC gc, Event event);

    void showDetailsToolTip(DBNNode node, Tree tree, Event event);

    void performAction(DBNNode node, Tree tree, Event event, boolean defaultAction);

    void handleHover(DBNNode node, Tree tree, TreeItem item, Event event);
}

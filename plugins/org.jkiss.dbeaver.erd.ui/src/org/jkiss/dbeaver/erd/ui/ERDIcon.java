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
package org.jkiss.dbeaver.erd.ui;

import org.jkiss.dbeaver.model.DBIcon;

public class ERDIcon {
    public static final DBIcon ARRANGE_ALL = new DBIcon("arrangeall", "arrangeall.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon LAYER_GRID = new DBIcon("layer_grid", "layer_grid.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon LAYOUT_SAVE = new DBIcon("layout_save", "layout_save.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon SELECT = new DBIcon("select", "select.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon MOVE = new DBIcon("move", "move.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon NOTE = new DBIcon("note", "note.png"); //$NON-NLS-1$ //$NON-NLS-2$

    static {
        DBIcon.loadIcons(ERDIcon.class);
    }
}

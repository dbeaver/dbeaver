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

package org.jkiss.dbeaver.erd.ui;

import org.eclipse.draw2d.PrintFigureOperation;
import org.jkiss.dbeaver.erd.ui.internal.ERDUIActivator;

/**
 * ERD constants
 */
public class ERDUIConstants {

    public static final String ERD_CONTROL_ID = "org.jkiss.dbeaver.erd.ERDEditor";

    public static final String PREF_PRINT_PAGE_MODE = "erd.print.page-mode";
    public static final String PREF_PRINT_MARGIN_TOP = "erd.print.margin-top";
    public static final String PREF_PRINT_MARGIN_BOTTOM = "erd.print.margin-bottom";
    public static final String PREF_PRINT_MARGIN_LEFT = "erd.print.margin-left";
    public static final String PREF_PRINT_MARGIN_RIGHT = "erd.print.margin-right";

    public static final int PRINT_MODE_DEFAULT = PrintFigureOperation.TILE;
    public static final int PRINT_MARGIN_DEFAULT = 0;

    public static final String PROP_DIAGRAM_FONT = "org.jkiss.dbeaver.erd.diagram.font";

    public static final String PREF_DIAGRAM_SHOW_VIEWS = "erd.diagram.show.views";
    public static final String PREF_DIAGRAM_SHOW_PARTITIONS = "erd.diagram.show.partitions";
    public static final String PREF_DIAGRAM_CHANGE_BORDER_COLORS = "erd.diagram.change.borders.colors";
    public static final String PREF_DIAGRAM_CHANGE_HEADER_COLORS = "erd.diagram.change.header.colors";
    public static final String PREF_GRID_ENABLED = "erd.grid.enabled";
    public static final String PREF_GRID_SNAP_ENABLED = "erd.grid.snap";
    public static final String PREF_GRID_WIDTH = "erd.grid.width";
    public static final String PREF_GRID_HEIGHT = "erd.grid.height";
    public static final String COLOR_ERD_DIAGRAM_BACKGROUND = "org.jkiss.dbeaver.erd.diagram.background";
    public static final String COLOR_ERD_ENTITY_PRIMARY_BACKGROUND = "org.jkiss.dbeaver.erd.diagram.entity.primary.background";
    public static final String COLOR_ERD_ENTITY_ASSOCIATION_BACKGROUND = "org.jkiss.dbeaver.erd.diagram.entity.association.background";
    public static final String COLOR_ERD_ENTITY_REGULAR_BACKGROUND = "org.jkiss.dbeaver.erd.diagram.entity.regular.background";
    public static final String COLOR_ERD_ENTITY_NAME_FOREGROUND = "org.jkiss.dbeaver.erd.diagram.entity.name.foreground";
    public static final String COLOR_ERD_LINES_FOREGROUND = "org.jkiss.dbeaver.erd.diagram.lines.foreground";
    public static final String COLOR_ERD_ATTR_BACKGROUND = "org.jkiss.dbeaver.erd.diagram.attributes.background";
    public static final String COLOR_ERD_ATTR_FOREGROUND = "org.jkiss.dbeaver.erd.diagram.attributes.foreground";
    public static final String COLOR_ERD_NOTE_BACKGROUND = "org.jkiss.dbeaver.erd.diagram.notes.background";
    public static final String COLOR_ERD_NOTE_FOREGROUND = "org.jkiss.dbeaver.erd.diagram.notes.foreground";

    public static final String COLOR_ERD_BORDERS_COLOR_1 = "org.jkiss.dbeaver.ui.presentation.erd.borders.color.1"; //$NON-NLS-1$
    public static final String COLOR_ERD_BORDERS_COLOR_2 = "org.jkiss.dbeaver.ui.presentation.erd.borders.color.2"; //$NON-NLS-1$
    public static final String COLOR_ERD_BORDERS_COLOR_3 = "org.jkiss.dbeaver.ui.presentation.erd.borders.color.3"; //$NON-NLS-1$
    public static final String COLOR_ERD_BORDERS_COLOR_4 = "org.jkiss.dbeaver.ui.presentation.erd.borders.color.4"; //$NON-NLS-1$
    public static final String COLOR_ERD_BORDERS_COLOR_5 = "org.jkiss.dbeaver.ui.presentation.erd.borders.color.5"; //$NON-NLS-1$
    public static final String COLOR_ERD_BORDERS_COLOR_6 = "org.jkiss.dbeaver.ui.presentation.erd.borders.color.6"; //$NON-NLS-1$
    public static final String COLOR_ERD_BORDERS_COLOR_7 = "org.jkiss.dbeaver.ui.presentation.erd.borders.color.7"; //$NON-NLS-1$

    public static final String COLOR_ERD_HEADER_COLOR_1 = "org.jkiss.dbeaver.ui.presentation.erd.headers.color.1"; //$NON-NLS-1$
    public static final String COLOR_ERD_HEADER_COLOR_2 = "org.jkiss.dbeaver.ui.presentation.erd.headers.color.2"; //$NON-NLS-1$
    public static final String COLOR_ERD_HEADER_COLOR_3 = "org.jkiss.dbeaver.ui.presentation.erd.headers.color.3"; //$NON-NLS-1$
    public static final String COLOR_ERD_HEADER_COLOR_4 = "org.jkiss.dbeaver.ui.presentation.erd.headers.color.4"; //$NON-NLS-1$
    public static final String COLOR_ERD_HEADER_COLOR_5 = "org.jkiss.dbeaver.ui.presentation.erd.headers.color.5"; //$NON-NLS-1$
    public static final String COLOR_ERD_HEADER_COLOR_6 = "org.jkiss.dbeaver.ui.presentation.erd.headers.color.6"; //$NON-NLS-1$
    public static final String COLOR_ERD_HEADER_COLOR_7 = "org.jkiss.dbeaver.ui.presentation.erd.headers.color.7"; //$NON-NLS-1$

    public static final String ICON_LOCATION_PREFIX = "platform:/plugin/" + ERDUIActivator.PLUGIN_ID + "/icons/";

    public static final int DEFAULT_NOTE_BORDER_WIDTH = 1;
    public static final int DEFAULT_ENTITY_BORDER_WIDTH = 2;

}

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

package org.jkiss.dbeaver.ext.erd;

import org.eclipse.draw2d.PrintFigureOperation;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;

/**
 * ERD constants
 */
public class ERDConstants {

    public static final String ERD_CONTROL_ID = "org.jkiss.dbeaver.erd.ERDEditor";

    public static final String PREF_PRINT_PAGE_MODE = "erd.print.page-mode";
    public static final String PREF_PRINT_MARGIN_TOP = "erd.print.margin-top";
    public static final String PREF_PRINT_MARGIN_BOTTOM = "erd.print.margin-bottom";
    public static final String PREF_PRINT_MARGIN_LEFT = "erd.print.margin-left";
    public static final String PREF_PRINT_MARGIN_RIGHT = "erd.print.margin-right";

    public static final int PRINT_MODE_DEFAULT = PrintFigureOperation.TILE;
    public static final int PRINT_MARGIN_DEFAULT = 0;

    public static final String PREF_DIAGRAM_SHOW_VIEWS = "erd.diagram.show.views";

    public static final String PREF_GRID_ENABLED = "erd.grid.enabled";
    public static final String PREF_GRID_SNAP_ENABLED = "erd.grid.snap";
    public static final String PREF_GRID_WIDTH = "erd.grid.width";
    public static final String PREF_GRID_HEIGHT = "erd.grid.height";
    public static final String PREF_ATTR_VISIBILITY = "erd.attr.visibility";
    public static final String PREF_ATTR_STYLES = "erd.attr.styles";
    public static final String COLOR_ERD_DIAGRAM_BACKGROUND = "org.jkiss.dbeaver.erd.diagram.background";
    public static final String COLOR_ERD_ENTITY_PRIMARY_BACKGROUND = "org.jkiss.dbeaver.erd.diagram.entity.primary.background";
    public static final String COLOR_ERD_ENTITY_ASSOCIATION_BACKGROUND = "org.jkiss.dbeaver.erd.diagram.entity.association.background";
    public static final String COLOR_ERD_ENTITY_REGULAR_BACKGROUND = "org.jkiss.dbeaver.erd.diagram.entity.regular.background";
    public static final String COLOR_ERD_ENTITY_NAME_FOREGROUND = "org.jkiss.dbeaver.erd.diagram.entity.name.foreground";
    public static final String COLOR_ERD_ATTR_BACKGROUND = "org.jkiss.dbeaver.erd.diagram.attributes.background";
    public static final String COLOR_ERD_ATTR_FOREGROUND = "org.jkiss.dbeaver.erd.diagram.attributes.foreground";

    public static final String ICON_LOCATION_PREFIX = "platform:/plugin/" + ERDActivator.PLUGIN_ID + "/icons/";

    public static DBSEntityConstraintType CONSTRAINT_LOGICAL_FK = new DBSEntityConstraintType("erdkey", "Logical Key", null, true, false, false);
}

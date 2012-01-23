/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.erd;

import org.eclipse.draw2d.PrintFigureOperation;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;

/**
 * ERD constants
 */
public class ERDConstants {

    public static final String PREF_PRINT_PAGE_MODE = "erd.print.page-mode";
    public static final String PREF_PRINT_MARGIN_TOP = "erd.print.margin-top";
    public static final String PREF_PRINT_MARGIN_BOTTOM = "erd.print.margin-bottom";
    public static final String PREF_PRINT_MARGIN_LEFT = "erd.print.margin-left";
    public static final String PREF_PRINT_MARGIN_RIGHT = "erd.print.margin-right";

    public static final int PRINT_MODE_DEFAULT = PrintFigureOperation.TILE;
    public static final int PRINT_MARGIN_DEFAULT = 0;

    public static final String PREF_GRID_ENABLED = "erd.grid.enabled";
    public static final String PREF_GRID_SNAP_ENABLED = "erd.grid.snap";
    public static final String PREF_GRID_WIDTH = "erd.grid.width";
    public static final String PREF_GRID_HEIGHT = "erd.grid.height";
    public static final String PREF_ATTR_VISIBILITY = "erd.attr.visibility";

    public static DBSEntityConstraintType CONSTRAINT_LOGICAL_FK = new DBSEntityConstraintType("erdkey", "Logical Key", true, false);
}

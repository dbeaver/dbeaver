/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui;

import org.eclipse.swt.graphics.Image;
import org.eclipse.jface.resource.ImageDescriptor;
import org.jkiss.dbeaver.core.DBeaverIcons;

/**
 * DBIcon
 */
public enum DBIcon
{
    TREE_DATABASE("database", "icons/tree/database.png"),
    TREE_CATALOG("catalog", "icons/tree/catalog.png"),
    TREE_SCHEMA("schema", "icons/tree/schema.png"),
    TREE_TABLES("tables", "icons/tree/tables.png"),
    TREE_TABLE("table", "icons/tree/table.png"),
    TREE_VIEW("view", "icons/tree/view.png"),
    TREE_PROCEDURE("view", "icons/tree/procedure.png"),
    TREE_COLUMNS("columns", "icons/tree/columns.png"),
    TREE_COLUMN("column", "icons/tree/column.png"),

    LOADING1("loading1", "icons/tree/load/loading1.gif"),
    LOADING2("loading2", "icons/tree/load/loading2.gif"),
    LOADING3("loading3", "icons/tree/load/loading3.gif"),
    LOADING4("loading4", "icons/tree/load/loading4.gif"),

    ROTATE1("rotate1", "icons/tree/load/rotate1.png"),
    ROTATE2("rotate2", "icons/tree/load/rotate2.png"),
    ROTATE3("rotate3", "icons/tree/load/rotate3.png"),
    ROTATE4("rotate4", "icons/tree/load/rotate4.png"),

    RS_MODE_GRID("rs_toggle_grid", "/icons/sql/resultset_grid.png"),
    RS_MODE_RECORD("rs_toggle_record", "/icons/sql/resultset_record.png"),
    RS_FIRST("rs_first", "/icons/sql/resultset_first.png"),
    RS_LAST("rs_last", "/icons/sql/resultset_last.png"),
    RS_NEXT("rs_next", "/icons/sql/resultset_next.png"),
    RS_PREV("rs_prev", "/icons/sql/resultset_previous.png"),
    RS_REFRESH("rs_refresh", "/icons/sql/resultset_refresh.png"),

    ZOOM("zoom", "/icons/misc/zoom.png"),
    ZOOM_IN("zoom_in", "/icons/misc/zoom_in.png"),
    ZOOM_OUT("zoom_out", "/icons/misc/zoom_out.png"),
    ROTATE("rotate", "/icons/misc/rotate.png"),
    ROTATE_LEFT("rotate_left", "/icons/misc/rotate_left.png"),
    ROTATE_RIGHT("rotate_right", "/icons/misc/rotate_right.png"),
    FIT_WINDOW("fit_window", "/icons/misc/fit-window.png"),
    ORIGINAL_SIZE("original_size", "/icons/misc/original-size.png"),

    INFO("info", "/icons/file/info.png"),
    SAVE("save", "/icons/file/save.png"),
    SAVE_ALL("save_all", "/icons/file/save_all.png"),
    SAVE_AS("save_as", "/icons/file/save_as.png"),
    LOAD("load", "/icons/file/load.png"),
    ACCEPT("accept", "/icons/sql/accept.png"),
    REJECT("reject", "/icons/sql/cancel.png"),
    IMPORT("import", "/icons/file/import.png"),
    EXPORT("export", "/icons/file/export.png"),

    LOB("lob", "/icons/misc/lob.png"),
    EDIT_DATABSE("edit_database", "/icons/misc/edit_database.png"),
    EDIT_TABLE("edit_table", "/icons/misc/edit_table.png"),
    EDIT_COLUMN("edit_column", "/icons/misc/edit_column.png"),
    LOCKED("locked", "/icons/misc/locked.png"),

    HEX("hex", "/icons/misc/hex.png"),
    TEXT("text", "/icons/misc/text.png"),
    IMAGE("image", "/icons/misc/image.png"),
    ;

    private final String token;
    private final String path;

    DBIcon(String token, String path)
    {
        this.token = token;
        this.path = path;
    }

    public String getToken()
    {
        return token;
    }

    public String getPath()
    {
        return path;
    }

    public Image getImage()
    {
        return DBeaverIcons.getImage(this);
    }

    public ImageDescriptor getImageDescriptor()
    {
        return DBeaverIcons.getImageDescriptor(this);
    }

}

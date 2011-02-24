/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.jkiss.dbeaver.core.DBeaverIcons;

/**
 * DBIcon
 */
public enum DBIcon
{
    GEN_DATABASE("gen_database", "icons/database.png"),
    GEN_DATABASE_TYPE("gen_database_type", "icons/database_type.png"),

    TREE("tree", "icons/tree/tree.png"),
    TREE_DATABASE("database", "icons/tree/database.png"),
    TREE_CATALOG("catalog", "icons/tree/catalog.png"),
    TREE_SCHEMA("schema", "icons/tree/schema.png"),
    TREE_TABLES("tables", "icons/tree/tables.png"),
    TREE_TABLE("table", "icons/tree/table.png"),
    TREE_TABLE_SORT("table_sort", "icons/tree/table_sort.png"),
    TREE_VIEW("view", "icons/tree/view.png"),
    TREE_PROCEDURE("procedure", "icons/tree/procedure.png"),
    TREE_COLUMNS("columns", "icons/tree/columns.png"),
    TREE_COLUMN("column", "icons/tree/column.png"),
    TREE_INDEX("index", "icons/tree/index.png"),
    TREE_CONSTRAINT("constraint", "icons/tree/constraint.png"),
    TREE_UNIQUE_KEY("unique-key", "icons/tree/unique_constraint.png"),
    TREE_FOREIGN_KEY("foreign-key", "icons/tree/foreign_key.png"),
    TREE_FOREIGN_KEY_COLUMN("foreign-key-column", "icons/tree/foreign_key_column.png"),
    TREE_REFERENCE("reference", "icons/tree/reference.png"),
    TREE_TRIGGER("trigger", "icons/tree/trigger.png"),
    TREE_USER("user", "icons/tree/user.png"),
    TREE_PACKAGE("package", "icons/tree/package.png"),

    PROJECT("project", "icons/project.png"),
    PROJECTS("projects", "icons/projects.png"),
    CONNECTIONS("connections", "icons/connections.png"),
    DATABASES("databases", "icons/databases.png"),
    SCRIPTS("scripts", "icons/scripts.png"),
    BOOKMARK_FOLDER("bookmark_folder", "icons/bookmark_folder.png"),
    BOOKMARK("bookmark", "icons/bookmark.png"),

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

    FIND("find", "/icons/misc/find.png"),
    CHECK("check", "/icons/misc/check.png"),
    CHECK2("check2", "/icons/misc/check2.png"),
    ZOOM("zoom", "/icons/misc/zoom.png"),
    ZOOM_IN("zoom_in", "/icons/misc/zoom_in.png"),
    ZOOM_OUT("zoom_out", "/icons/misc/zoom_out.png"),
    ROTATE("rotate", "/icons/misc/rotate.png"),
    ROTATE_LEFT("rotate_left", "/icons/misc/rotate_left.png"),
    ROTATE_RIGHT("rotate_right", "/icons/misc/rotate_right.png"),
    FIT_WINDOW("fit_window", "/icons/misc/fit-window.png"),
    ORIGINAL_SIZE("original_size", "/icons/misc/original-size.png"),
    ASTERISK("asterisk", "/icons/misc/asterisk.png"),
    BULLET_STAR("bullet_star", "/icons/misc/bullet_star.png"),
    ARROW_UP("arrow_up", "/icons/misc/arrow_up.png"),
    ARROW_DOWN("arrow_down", "/icons/misc/arrow_down.png"),
    ARROW_LEFT("arrow_left", "/icons/misc/arrow_left.png"),
    ARROW_LEFT_ALL("arrow_left_all", "/icons/misc/arrow_left_all.png"),
    ARROW_RIGHT("arrow_right", "/icons/misc/arrow_right.png"),
    ARROW_RIGHT_ALL("arrow_right_all", "/icons/misc/arrow_right_all.png"),
    ARROW_RESET("arrow_reset", "/icons/misc/arrow_reset.png"),
    SORT_INCREASE("sort_increase", "/icons/misc/sort_increase.png"),
    SORT_DECREASE("sort_decrease", "/icons/misc/sort_decrease.png"),
    SORT_UNKNOWN("sort_unknown", "/icons/misc/sort_unknown.png"),
    FRAME_QUERY("frame_query", "/icons/misc/frame_query.png"),
    FILTER("filter", "/icons/misc/filter.png"),

    INFO("info", "/icons/file/info.png"),
    SAVE("save", "/icons/file/save.png"),
    SAVE_ALL("save_all", "/icons/file/save_all.png"),
    SAVE_AS("save_as", "/icons/file/save_as.png"),
    LOAD("load", "/icons/file/load.png"),
    ACCEPT("accept", "/icons/sql/accept.png"),
    REJECT("reject", "/icons/sql/cancel.png"),
    REVERT("revert", "/icons/sql/revert.png"),
    IMPORT("import", "/icons/file/import.png"),
    EXPORT("export", "/icons/file/export.png"),
    REFRESH("refresh", "/icons/refresh.png"),
    JAR("jar", "/icons/misc/jar.png"),

    ROW_ADD("row_add", "/icons/sql/row_add.png"),
    ROW_COPY("row_copy", "/icons/sql/row_copy.png"),
    ROW_EDIT("row_edit", "/icons/sql/row_edit.png"),
    ROW_DELETE("row_delete", "/icons/sql/row_delete.png"),

    EDIT_DATABSE("edit_database", "/icons/misc/edit_database.png"),
    EDIT_TABLE("edit_table", "/icons/misc/edit_table.png"),
    EDIT_COLUMN("edit_column", "/icons/misc/edit_column.png"),
    LOCKED("locked", "/icons/misc/locked.png"),

    TYPE_BOOLEAN("boolean", "/icons/sql/types/boolean.png"),
    TYPE_NUMBER("number", "/icons/sql/types/number.png"),
    TYPE_STRING("string", "/icons/sql/types/string.png"),
    TYPE_DATETIME("datetime", "/icons/sql/types/datetime.png"),
    TYPE_BINARY("binary", "/icons/sql/types/binary.png"),
    TYPE_TEXT("text", "/icons/sql/types/text.png"),
    TYPE_LOB("lob", "/icons/sql/types/lob.png"),
    TYPE_ARRAY("array", "/icons/sql/types/array.png"),
    TYPE_STRUCT("struct", "/icons/sql/types/struct.png"),
    TYPE_IMAGE("image", "/icons/sql/types/image.png"),
    TYPE_UNKNOWN("unknown", "/icons/sql/types/unknown.png"),

    SQL_SCRIPT("sql_script", "/icons/sql/sql_script.png"),
    SQL_EXECUTE("sql_exec", "/icons/sql/sql_exec.png"),
    SQL_SCRIPT_EXECUTE("sql_script_exec", "/icons/sql/sql_script_exec.png"),
    SQL_ANALYSE("sql_analyse", "/icons/sql/sql_analyse.png"),
    SQL_EXPLAIN_PLAN("sql_explain", "/icons/sql/sql_plan.png"),
    SQL_VALIDATE("sql_validate", "/icons/sql/sql_validate.png"),
    SQL_PREVIEW("sql_preview", "/icons/sql/sql_preview.png"),
    SAVE_TO_DATABASE("save_to_db", "/icons/sql/save_to_database.png"),

    OVER_SUCCESS("over_success", "/icons/over/success_ovr.png"),
    OVER_FAILED("over_failed", "/icons/over/failed_ovr.png"),
    OVER_ERROR("over_failed", "/icons/over/error_ovr.png"),
    OVER_CONDITION("over_condition", "/icons/over/conditional_ovr.png"),

    ABOUT("about", "/icons/about_circle.png");

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

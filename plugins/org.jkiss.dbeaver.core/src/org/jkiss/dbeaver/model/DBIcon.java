/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
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

package org.jkiss.dbeaver.model;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.jkiss.dbeaver.runtime.RuntimeUtils;
import org.jkiss.dbeaver.ui.DBeaverIcons;

import java.io.File;
import java.io.IOException;

/**
 * DBIcon
 */
public enum DBIcon implements DBPImage
{
    DBEAVER_LOGO("dbeaver_logo", "dbeaver.png"), //$NON-NLS-1$ //$NON-NLS-2$

    GEN_DATABASE("gen_database", "database.png"), //$NON-NLS-1$ //$NON-NLS-2$
    GEN_DATABASE_TYPE("gen_database_type", "database_type.png"), //$NON-NLS-1$ //$NON-NLS-2$

    TREE("tree", "tree/tree.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TREE_ADMIN("admin", "tree/admin.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TREE_DATABASE("database", "tree/database.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TREE_DATABASE_CATEGORY("database_category", "tree/database_category.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TREE_SCHEMA("schema", "tree/schema.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TREE_TABLE("table", "tree/table.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TREE_TABLE_ALIAS("table_alias", "tree/table_alias.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TREE_TABLE_SORT("table_sort", "tree/table_sort.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TREE_VIEW("view", "tree/view.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TREE_FUNCTION("function", "tree/function.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TREE_PROCEDURE("procedure", "tree/procedure.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TREE_COLUMNS("columns", "tree/columns.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TREE_COLUMN("column", "tree/column.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TREE_INDEX("index", "tree/index.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TREE_CONSTRAINT("constraint", "tree/constraint.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TREE_UNIQUE_KEY("unique-key", "tree/unique_constraint.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TREE_FOREIGN_KEY("foreign-key", "tree/foreign_key.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TREE_FOREIGN_KEY_COLUMN("foreign-key-column", "tree/foreign_key_column.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TREE_REFERENCE("reference", "tree/reference.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TREE_TRIGGER("trigger", "tree/trigger.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TREE_USER("user", "tree/user.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TREE_PAGE("page", "tree/page.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TREE_FOLDER("folder", "tree/folder.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TREE_FOLDER_LINK("folder_linked", "tree/folder_link.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TREE_FOLDER_DATABASE("folder_database", "tree/folder_database.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TREE_FOLDER_SCHEMA("folder_schema", "tree/folder_schema.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TREE_FOLDER_TABLE("folder_table", "tree/folder_table.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TREE_FOLDER_VIEW("folder_view", "tree/folder_view.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TREE_FOLDER_USER("folder_user", "tree/folder_user.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TREE_FOLDER_ADMIN("folder_admin", "tree/folder_admin.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TREE_SESSIONS("sessions", "tree/sessions.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TREE_PACKAGE("package", "tree/package.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TREE_DATA_TYPE("data_type", "tree/data_type.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TREE_SEQUENCE("sequence", "tree/sequence.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TREE_SYNONYM("synonym", "tree/synonym.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TREE_TABLESPACE("tablespace", "tree/tablespace.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TREE_PARTITION("partition", "tree/partition.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TREE_ATTRIBUTE("attribute", "tree/attribute.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TREE_ARGUMENT("argument", "tree/argument.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TREE_JAVA_CLASS("javaClass", "tree/java_class.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TREE_JAVA_INTERFACE("javaInterface", "tree/java_interface.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TREE_LINK("link", "tree/link.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TREE_FILE("file", "tree/file.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TREE_CLASS("class", "tree/class.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TREE_ASSOCIATION("association", "tree/association.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TREE_SERVER("server", "tree/server.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TREE_SERVERS("servers", "tree/servers.png"), //$NON-NLS-1$ //$NON-NLS-2$

    TREE_INFO("info", "/tree/info.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TREE_FOLDER_INFO("folder_info", "/tree/folder_info.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TREE_RECYCLE_BIN("recycle_bin", "/tree/recycle_bin.png"), //$NON-NLS-1$ //$NON-NLS-2$

    TREE_EXPAND("expand", "/misc/expand.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TREE_COLLAPSE("collapse", "/misc/collapse.png"), //$NON-NLS-1$ //$NON-NLS-2$

    PROJECT("project", "project.png"), //$NON-NLS-1$ //$NON-NLS-2$
    PROJECTS("projects", "projects.png"), //$NON-NLS-1$ //$NON-NLS-2$
    CONNECTIONS("connections", "connections.png"), //$NON-NLS-1$ //$NON-NLS-2$
    DATABASES("databases", "databases.png"), //$NON-NLS-1$ //$NON-NLS-2$
    SCRIPTS("scripts", "scripts.png"), //$NON-NLS-1$ //$NON-NLS-2$
    BOOKMARK_FOLDER("bookmark_folder", "bookmark_folder.png"), //$NON-NLS-1$ //$NON-NLS-2$
    BOOKMARK("bookmark", "bookmark.png"), //$NON-NLS-1$ //$NON-NLS-2$
    PICTURE("picture", "picture.png"), //$NON-NLS-1$ //$NON-NLS-2$
    PICTURE_SAVE("picture_save", "picture_save.png"), //$NON-NLS-1$ //$NON-NLS-2$
    PROPERTIES("properties", "properties.png"), //$NON-NLS-1$ //$NON-NLS-2$
    CONFIGURATION("configuration", "configuration.png"), //$NON-NLS-1$ //$NON-NLS-2$
    LINK("link", "misc/link.png"), //$NON-NLS-1$ //$NON-NLS-2$
    CURSOR("cursor", "misc/cursor.png"), //$NON-NLS-1$ //$NON-NLS-2$

    LOADING1("loading1", "tree/load/loading1.gif"), //$NON-NLS-1$ //$NON-NLS-2$
    LOADING2("loading2", "tree/load/loading2.gif"), //$NON-NLS-1$ //$NON-NLS-2$
    LOADING3("loading3", "tree/load/loading3.gif"), //$NON-NLS-1$ //$NON-NLS-2$
    LOADING4("loading4", "tree/load/loading4.gif"), //$NON-NLS-1$ //$NON-NLS-2$

    PROGRESS0("progress0", "misc/progress_0.gif"), //$NON-NLS-1$ //$NON-NLS-2$
    PROGRESS1("progress1", "misc/progress_1.gif"), //$NON-NLS-1$ //$NON-NLS-2$
    PROGRESS2("progress2", "misc/progress_2.gif"), //$NON-NLS-1$ //$NON-NLS-2$
    PROGRESS3("progress3", "misc/progress_3.gif"), //$NON-NLS-1$ //$NON-NLS-2$
    PROGRESS4("progress4", "misc/progress_4.gif"), //$NON-NLS-1$ //$NON-NLS-2$
    PROGRESS5("progress5", "misc/progress_5.gif"), //$NON-NLS-1$ //$NON-NLS-2$
    PROGRESS6("progress6", "misc/progress_6.gif"), //$NON-NLS-1$ //$NON-NLS-2$
    PROGRESS7("progress7", "misc/progress_7.gif"), //$NON-NLS-1$ //$NON-NLS-2$
    PROGRESS8("progress8", "misc/progress_8.gif"), //$NON-NLS-1$ //$NON-NLS-2$
    PROGRESS9("progress9", "misc/progress_9.gif"), //$NON-NLS-1$ //$NON-NLS-2$

//    RS_FIRST("rs_first", "/sql/resultset_first.png"), //$NON-NLS-1$ //$NON-NLS-2$
//    RS_LAST("rs_last", "/sql/resultset_last.png"), //$NON-NLS-1$ //$NON-NLS-2$
//    RS_NEXT("rs_next", "/sql/resultset_next.png"), //$NON-NLS-1$ //$NON-NLS-2$
//    RS_PREV("rs_prev", "/sql/resultset_previous.png"), //$NON-NLS-1$ //$NON-NLS-2$
//    RS_FETCH_PAGE("resultset_fetch_page.png", "sql/resultset_fetch_page.png"), //$NON-NLS-1$ //$NON-NLS-2$
    RS_REFRESH("rs_refresh", "/sql/resultset_refresh.png"), //$NON-NLS-1$ //$NON-NLS-2$
    RS_GRID("rs_mode_grid", "sql/grid.png"), //$NON-NLS-1$ //$NON-NLS-2$
    RS_DETAILS("rs_details", "sql/details.png"), //$NON-NLS-1$ //$NON-NLS-2$
    RS_FORWARD("rs_forward", "sql/forward.png"), //$NON-NLS-1$ //$NON-NLS-2$
    RS_BACK("rs_back", "sql/back.png"), //$NON-NLS-1$ //$NON-NLS-2$

    TXN_COMMIT_AUTO("txn_commit_auto", "sql/txn_auto.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TXN_COMMIT_MANUAL("txn_commit_manual", "sql/txn_manual.png"), //$NON-NLS-1$ //$NON-NLS-2$

    RULER_POSITION("ruler_position", "/misc/ruler_position.png"), //$NON-NLS-1$ //$NON-NLS-2$
    FILTER_VALUE("filter_value", "/misc/filter_value.png"), //$NON-NLS-1$ //$NON-NLS-2$
    FILTER_INPUT("filter_input", "/misc/filter_input.png"), //$NON-NLS-1$ //$NON-NLS-2$
    FILTER_CLIPBOARD("filter_clipboard", "/misc/filter_clipboard.png"), //$NON-NLS-1$ //$NON-NLS-2$

    FIND("find", "/misc/find.png"), //$NON-NLS-1$ //$NON-NLS-2$
    FIND_TEXT("find_text", "/misc/find_text.png"), //$NON-NLS-1$ //$NON-NLS-2$
    SEARCH("search", "/misc/search.png"), //$NON-NLS-1$ //$NON-NLS-2$
    CHECK("check", "/misc/check.png"), //$NON-NLS-1$ //$NON-NLS-2$
    CHECK2("check2", "/misc/check2.png"), //$NON-NLS-1$ //$NON-NLS-2$
    CHECK_ON("checked", "/misc/checked.png"), //$NON-NLS-1$ //$NON-NLS-2$
    CHECK_OFF("unchecked", "/misc/unchecked.png"), //$NON-NLS-1$ //$NON-NLS-2$
    ZOOM("zoom", "/misc/zoom.png"), //$NON-NLS-1$ //$NON-NLS-2$
    ZOOM_IN("zoom_in", "/misc/zoom_in.png"), //$NON-NLS-1$ //$NON-NLS-2$
    ZOOM_OUT("zoom_out", "/misc/zoom_out.png"), //$NON-NLS-1$ //$NON-NLS-2$
    ROTATE("rotate", "/misc/rotate.png"), //$NON-NLS-1$ //$NON-NLS-2$
    ROTATE_LEFT("rotate_left", "/misc/rotate_left.png"), //$NON-NLS-1$ //$NON-NLS-2$
    ROTATE_RIGHT("rotate_right", "/misc/rotate_right.png"), //$NON-NLS-1$ //$NON-NLS-2$
    FIT_WINDOW("fit_window", "/misc/fit-window.png"), //$NON-NLS-1$ //$NON-NLS-2$
    ORIGINAL_SIZE("original_size", "/misc/original-size.png"), //$NON-NLS-1$ //$NON-NLS-2$
    ASTERISK("asterisk", "/misc/asterisk.png"), //$NON-NLS-1$ //$NON-NLS-2$
    BULLET_BLACK("bullet_black", "/misc/bullet_black.png"), //$NON-NLS-1$ //$NON-NLS-2$
    BULLET_GREEN("bullet_green", "/misc/bullet_green.png"), //$NON-NLS-1$ //$NON-NLS-2$
    BULLET_RED("bullet_red", "/misc/bullet_red.png"), //$NON-NLS-1$ //$NON-NLS-2$
    BULLET_STAR("bullet_star", "/misc/bullet_star.png"), //$NON-NLS-1$ //$NON-NLS-2$
    ARROW_UP("arrow_up", "/misc/arrow_up.png"), //$NON-NLS-1$ //$NON-NLS-2$
    ARROW_DOWN("arrow_down", "/misc/arrow_down.png"), //$NON-NLS-1$ //$NON-NLS-2$
    ARROW_LEFT("arrow_left", "/misc/arrow_left.png"), //$NON-NLS-1$ //$NON-NLS-2$
    ARROW_LEFT_ALL("arrow_left_all", "/misc/arrow_left_all.png"), //$NON-NLS-1$ //$NON-NLS-2$
    ARROW_RIGHT("arrow_right", "/misc/arrow_right.png"), //$NON-NLS-1$ //$NON-NLS-2$
    ARROW_RIGHT_ALL("arrow_right_all", "/misc/arrow_right_all.png"), //$NON-NLS-1$ //$NON-NLS-2$
    ARROW_RESET("arrow_reset", "/misc/arrow_reset.png"), //$NON-NLS-1$ //$NON-NLS-2$
    SORT_INCREASE("sort_increase", "/misc/sort_increase.png"), //$NON-NLS-1$ //$NON-NLS-2$
    SORT_DECREASE("sort_decrease", "/misc/sort_decrease.png"), //$NON-NLS-1$ //$NON-NLS-2$
    SORT_UNKNOWN("sort_unknown", "/misc/sort_unknown.png"), //$NON-NLS-1$ //$NON-NLS-2$
    FRAME_QUERY("frame_query", "/misc/frame_query.png"), //$NON-NLS-1$ //$NON-NLS-2$
    FILTER("filter", "/misc/filter.png"), //$NON-NLS-1$ //$NON-NLS-2$
    FILTER_APPLY("filter_apply", "/misc/filter_apply.png"), //$NON-NLS-1$ //$NON-NLS-2$
    FILTER_RESET("filter_reset", "/misc/filter_reset.png"), //$NON-NLS-1$ //$NON-NLS-2$
    EVENT("event", "/misc/event.png"), //$NON-NLS-1$ //$NON-NLS-2$
    HOME("home", "/misc/home.png"), //$NON-NLS-1$ //$NON-NLS-2$
    COMPILE("compile", "/misc/compile.png"), //$NON-NLS-1$ //$NON-NLS-2$
    COMPILE_LOG("compile_log", "/misc/compile_log.png"), //$NON-NLS-1$ //$NON-NLS-2$

    SAVE("save", "/file/save.png"), //$NON-NLS-1$ //$NON-NLS-2$
    SAVE_AS("save_as", "/file/save_as.png"), //$NON-NLS-1$ //$NON-NLS-2$
    LOAD("load", "/file/load.png"), //$NON-NLS-1$ //$NON-NLS-2$
    RESET("reset", "/file/reset.png"), //$NON-NLS-1$ //$NON-NLS-2$
    COMPARE("compare", "/file/compare.png"), //$NON-NLS-1$ //$NON-NLS-2$
    ACCEPT("accept", "/sql/accept.png"), //$NON-NLS-1$ //$NON-NLS-2$
    REJECT("reject", "/sql/cancel.png"), //$NON-NLS-1$ //$NON-NLS-2$
    REVERT("revert", "/sql/revert.png"), //$NON-NLS-1$ //$NON-NLS-2$
    IMPORT("import", "/file/import.png"), //$NON-NLS-1$ //$NON-NLS-2$
    EXPORT("export", "/file/export.png"), //$NON-NLS-1$ //$NON-NLS-2$
    REFRESH("refresh", "/refresh.png"), //$NON-NLS-1$ //$NON-NLS-2$
    CONFIRM("confirm", "/misc/confirm.png"), //$NON-NLS-1$ //$NON-NLS-2$
    CLOSE("close", "/misc/close.png"), //$NON-NLS-1$ //$NON-NLS-2$
    JAR("jar", "/misc/jar.png"), //$NON-NLS-1$ //$NON-NLS-2$
    SOURCES("sources", "/misc/sources.png"), //$NON-NLS-1$ //$NON-NLS-2$
    CANCEL("cancel", "/misc/cancel.png"), //$NON-NLS-1$ //$NON-NLS-2$

    ROW_ADD("row_add", "/sql/row_add.png"), //$NON-NLS-1$ //$NON-NLS-2$
    ROW_COPY("row_copy", "/sql/row_copy.png"), //$NON-NLS-1$ //$NON-NLS-2$
    ROW_EDIT("row_edit", "/sql/row_edit.png"), //$NON-NLS-1$ //$NON-NLS-2$
    ROW_DELETE("row_delete", "/sql/row_delete.png"), //$NON-NLS-1$ //$NON-NLS-2$

    EDIT_DATABASE("edit_database", "/misc/edit_database.png"), //$NON-NLS-1$ //$NON-NLS-2$
    EDIT_TABLE("edit_table", "/misc/edit_table.png"), //$NON-NLS-1$ //$NON-NLS-2$
    EDIT_COLUMN("edit_column", "/misc/edit_column.png"), //$NON-NLS-1$ //$NON-NLS-2$
    CONFIG_TABLE("edit_table", "/misc/config_table.png"), //$NON-NLS-1$ //$NON-NLS-2$

    TYPE_BOOLEAN("boolean", "/sql/types/boolean.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TYPE_NUMBER("number", "/sql/types/number.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TYPE_STRING("string", "/sql/types/string.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TYPE_DATETIME("datetime", "/sql/types/datetime.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TYPE_BINARY("binary", "/sql/types/binary.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TYPE_TEXT("text", "/sql/types/text.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TYPE_XML("xml", "/sql/types/xml.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TYPE_LOB("lob", "/sql/types/lob.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TYPE_ARRAY("array", "/sql/types/array.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TYPE_STRUCT("struct", "/sql/types/struct.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TYPE_DOCUMENT("document", "/sql/types/document.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TYPE_OBJECT("object", "/sql/types/object.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TYPE_IMAGE("image", "/sql/types/image.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TYPE_REFERENCE("reference", "/sql/types/reference.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TYPE_ROWID("rowid", "/sql/types/rowid.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TYPE_ANY("any", "/sql/types/any.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TYPE_UUID("uuid", "/sql/types/uuid.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TYPE_UNKNOWN("unknown", "/sql/types/unknown.png"), //$NON-NLS-1$ //$NON-NLS-2$

    SQL_CONNECT("sql_connect", "/sql/connect.png"), //$NON-NLS-1$ //$NON-NLS-2$
    SQL_DISCONNECT("sql_disconnect", "/sql/disconnect.png"), //$NON-NLS-1$ //$NON-NLS-2$
    SQL_SCRIPT("sql_script", "/sql/sql_script.png"), //$NON-NLS-1$ //$NON-NLS-2$
    SQL_EXECUTE("sql_exec", "/sql/sql_exec.png"), //$NON-NLS-1$ //$NON-NLS-2$
    SQL_SCRIPT_EXECUTE("sql_script_exec", "/sql/sql_script_exec.png"), //$NON-NLS-1$ //$NON-NLS-2$
    SQL_ANALYSE("sql_analyse", "/sql/sql_analyse.png"), //$NON-NLS-1$ //$NON-NLS-2$
    SQL_EXPLAIN_PLAN("sql_explain", "/sql/sql_plan.png"), //$NON-NLS-1$ //$NON-NLS-2$
    SQL_VALIDATE("sql_validate", "/sql/sql_validate.png"), //$NON-NLS-1$ //$NON-NLS-2$
    SQL_PREVIEW("sql_preview", "/sql/sql_preview.png"), //$NON-NLS-1$ //$NON-NLS-2$
    SQL_TEXT("sql_text", "/sql/sql_text.png"), //$NON-NLS-1$ //$NON-NLS-2$
    SAVE_TO_DATABASE("save_to_db", "/sql/save_to_database.png"), //$NON-NLS-1$ //$NON-NLS-2$

    OVER_SUCCESS("over_success", "/over/success_ovr.png"), //$NON-NLS-1$ //$NON-NLS-2$
    OVER_FAILED("over_failed", "/over/failed_ovr.png"), //$NON-NLS-1$ //$NON-NLS-2$
    OVER_ERROR("over_error", "/over/error_ovr.png"), //$NON-NLS-1$ //$NON-NLS-2$
    OVER_UNKNOWN("over_condition", "/over/conditional_ovr.png"), //$NON-NLS-1$ //$NON-NLS-2$
    OVER_LAMP("over_lamp", "/over/lamp_ovr.png"), //$NON-NLS-1$ //$NON-NLS-2$
    OVER_KEY("over_key", "/over/key_ovr.png"), //$NON-NLS-1$ //$NON-NLS-2$
    OVER_LOCK("over_lock", "/over/lock_ovr.png"), //$NON-NLS-1$ //$NON-NLS-2$
    OVER_EXTERNAL("over_external", "/over/external_ovr.png"), //$NON-NLS-1$ //$NON-NLS-2$
    OVER_REFERENCE("over_reference", "/over/reference_ovr.png"); //$NON-NLS-1$ //$NON-NLS-2$

    private final String token;
    private final String path;

    DBIcon(String token, String path)
    {
        this.token = token;
        this.path = ICON_LOCATION_PREFIX + path;
    }


    /**
     * Token is icon id. It can be used to refer on icons in plugin extensions
     * @return unique token
     */
    public String getToken()
    {
        return token;
    }

    public Image getImage()
    {
        return DBeaverIcons.getImage(this);
    }

    public ImageDescriptor getImageDescriptor()
    {
        return DBeaverIcons.getImageDescriptor(this);
    }

    @Override
    public String getLocation() {
        return path;
    }

    @Override
    public File getFile() throws IOException {
        return RuntimeUtils.getPlatformFile(path);
    }

    static final String ICON_LOCATION_PREFIX = "platform:/plugin/org.jkiss.dbeaver.core/icons/";
}

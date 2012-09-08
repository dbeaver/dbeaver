/*
 * Copyright (C) 2010-2012 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
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
    GEN_DATABASE("icons/database.png"), //$NON-NLS-1$ //$NON-NLS-2$
    GEN_DATABASE_TYPE("icons/database_type.png"), //$NON-NLS-1$ //$NON-NLS-2$

    TREE("icons/tree/tree.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TREE_ADMIN("icons/tree/admin.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TREE_DATABASE("icons/tree/database.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TREE_SCHEMA("icons/tree/schema.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TREE_TABLES("icons/tree/tables.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TREE_TABLE("icons/tree/table.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TREE_TABLE_ALIAS("icons/tree/table_alias.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TREE_TABLE_SORT("icons/tree/table_sort.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TREE_VIEW("icons/tree/view.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TREE_PROCEDURE("icons/tree/procedure.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TREE_COLUMNS("icons/tree/columns.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TREE_COLUMN("icons/tree/column.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TREE_INDEX("icons/tree/index.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TREE_CONSTRAINT("icons/tree/constraint.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TREE_UNIQUE_KEY("icons/tree/unique_constraint.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TREE_FOREIGN_KEY("icons/tree/foreign_key.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TREE_FOREIGN_KEY_COLUMN("icons/tree/foreign_key_column.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TREE_REFERENCE("icons/tree/reference.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TREE_TRIGGER("icons/tree/trigger.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TREE_USER("icons/tree/user.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TREE_PAGE("icons/tree/page.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TREE_FOLDER("icons/tree/folder.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TREE_FOLDER_LINK("icons/tree/folder_link.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TREE_FOLDER_DATABASE("icons/tree/folder_database.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TREE_FOLDER_SCHEMA("icons/tree/folder_schema.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TREE_FOLDER_TABLE("icons/tree/folder_table.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TREE_FOLDER_VIEW("icons/tree/folder_view.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TREE_FOLDER_USER("icons/tree/folder_user.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TREE_FOLDER_ADMIN("icons/tree/folder_admin.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TREE_PACKAGE("icons/tree/package.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TREE_DATA_TYPE("icons/tree/data_type.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TREE_SEQUENCE("icons/tree/sequence.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TREE_SYNONYM("icons/tree/synonym.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TREE_TABLESPACE("icons/tree/tablespace.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TREE_PARTITION("icons/tree/partition.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TREE_ATTRIBUTE("icons/tree/attribute.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TREE_ARGUMENT("icons/tree/argument.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TREE_JAVA_CLASS("icons/tree/java_class.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TREE_JAVA_INTERFACE("icons/tree/java_interface.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TREE_LINK("icons/tree/link.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TREE_FILE("icons/tree/file.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TREE_CLASS("icons/tree/class.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TREE_ASSOCIATION("icons/tree/association.png"), //$NON-NLS-1$ //$NON-NLS-2$

    TREE_INFO("/icons/tree/info.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TREE_FOLDER_INFO("/icons/tree/folder_info.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TREE_RECYCLE_BIN("/icons/tree/recycle_bin.png"), //$NON-NLS-1$ //$NON-NLS-2$

    PROJECT("icons/project.png"), //$NON-NLS-1$ //$NON-NLS-2$
    PROJECTS("icons/projects.png"), //$NON-NLS-1$ //$NON-NLS-2$
    CONNECTIONS("icons/connections.png"), //$NON-NLS-1$ //$NON-NLS-2$
    DATABASES("icons/databases.png"), //$NON-NLS-1$ //$NON-NLS-2$
    SCRIPTS("icons/scripts.png"), //$NON-NLS-1$ //$NON-NLS-2$
    BOOKMARK_FOLDER("icons/bookmark_folder.png"), //$NON-NLS-1$ //$NON-NLS-2$
    BOOKMARK("icons/bookmark.png"), //$NON-NLS-1$ //$NON-NLS-2$
    PICTURE("icons/picture.png"), //$NON-NLS-1$ //$NON-NLS-2$
    PICTURE_SAVE("icons/picture_save.png"), //$NON-NLS-1$ //$NON-NLS-2$
    PROPERTIES("icons/properties.png"), //$NON-NLS-1$ //$NON-NLS-2$
    CONFIGURATION("icons/configuration.png"), //$NON-NLS-1$ //$NON-NLS-2$

    LOADING1("icons/tree/load/loading1.gif"), //$NON-NLS-1$ //$NON-NLS-2$
    LOADING2("icons/tree/load/loading2.gif"), //$NON-NLS-1$ //$NON-NLS-2$
    LOADING3("icons/tree/load/loading3.gif"), //$NON-NLS-1$ //$NON-NLS-2$
    LOADING4("icons/tree/load/loading4.gif"), //$NON-NLS-1$ //$NON-NLS-2$

    ROTATE1("icons/tree/load/rotate1.png"), //$NON-NLS-1$ //$NON-NLS-2$
    ROTATE2("icons/tree/load/rotate2.png"), //$NON-NLS-1$ //$NON-NLS-2$
    ROTATE3("icons/tree/load/rotate3.png"), //$NON-NLS-1$ //$NON-NLS-2$
    ROTATE4("icons/tree/load/rotate4.png"), //$NON-NLS-1$ //$NON-NLS-2$

    RS_MODE_GRID("/icons/sql/resultset_grid.png"), //$NON-NLS-1$ //$NON-NLS-2$
    RS_MODE_RECORD("/icons/sql/resultset_record.png"), //$NON-NLS-1$ //$NON-NLS-2$
    RS_FIRST("/icons/sql/resultset_first.png"), //$NON-NLS-1$ //$NON-NLS-2$
    RS_LAST("/icons/sql/resultset_last.png"), //$NON-NLS-1$ //$NON-NLS-2$
    RS_NEXT("/icons/sql/resultset_next.png"), //$NON-NLS-1$ //$NON-NLS-2$
    RS_PREV("/icons/sql/resultset_previous.png"), //$NON-NLS-1$ //$NON-NLS-2$
    RS_REFRESH("/icons/sql/resultset_refresh.png"), //$NON-NLS-1$ //$NON-NLS-2$
    RS_TOGGLE_RECORD("icons/sql/resultset_record.png"), //$NON-NLS-1$ //$NON-NLS-2$
    RS_TOGGLE_GRID("icons/sql/resultset_grid.png"), //$NON-NLS-1$ //$NON-NLS-2$

    TXN_COMMIT_AUTO("icons/sql/txn_auto.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TXN_COMMIT_MANUAL("icons/sql/txn_manual.png"), //$NON-NLS-1$ //$NON-NLS-2$

    FIND("/icons/misc/find.png"), //$NON-NLS-1$ //$NON-NLS-2$
    SEARCH("/icons/misc/search.png"), //$NON-NLS-1$ //$NON-NLS-2$
    CHECK("/icons/misc/check.png"), //$NON-NLS-1$ //$NON-NLS-2$
    CHECK2("/icons/misc/check2.png"), //$NON-NLS-1$ //$NON-NLS-2$
    CHECK_ON("/icons/misc/checked.png"), //$NON-NLS-1$ //$NON-NLS-2$
    CHECK_OFF("/icons/misc/unchecked.png"), //$NON-NLS-1$ //$NON-NLS-2$
    ZOOM("/icons/misc/zoom.png"), //$NON-NLS-1$ //$NON-NLS-2$
    ZOOM_IN("/icons/misc/zoom_in.png"), //$NON-NLS-1$ //$NON-NLS-2$
    ZOOM_OUT("/icons/misc/zoom_out.png"), //$NON-NLS-1$ //$NON-NLS-2$
    ROTATE("/icons/misc/rotate.png"), //$NON-NLS-1$ //$NON-NLS-2$
    ROTATE_LEFT("/icons/misc/rotate_left.png"), //$NON-NLS-1$ //$NON-NLS-2$
    ROTATE_RIGHT("/icons/misc/rotate_right.png"), //$NON-NLS-1$ //$NON-NLS-2$
    FIT_WINDOW("/icons/misc/fit-window.png"), //$NON-NLS-1$ //$NON-NLS-2$
    ORIGINAL_SIZE("/icons/misc/original-size.png"), //$NON-NLS-1$ //$NON-NLS-2$
    ASTERISK("/icons/misc/asterisk.png"), //$NON-NLS-1$ //$NON-NLS-2$
    BULLET_BLACK("/icons/misc/bullet_black.png"), //$NON-NLS-1$ //$NON-NLS-2$
    BULLET_GREEN("/icons/misc/bullet_green.png"), //$NON-NLS-1$ //$NON-NLS-2$
    BULLET_RED("/icons/misc/bullet_red.png"), //$NON-NLS-1$ //$NON-NLS-2$
    BULLET_STAR("/icons/misc/bullet_star.png"), //$NON-NLS-1$ //$NON-NLS-2$
    ARROW_UP("/icons/misc/arrow_up.png"), //$NON-NLS-1$ //$NON-NLS-2$
    ARROW_DOWN("/icons/misc/arrow_down.png"), //$NON-NLS-1$ //$NON-NLS-2$
    ARROW_LEFT("/icons/misc/arrow_left.png"), //$NON-NLS-1$ //$NON-NLS-2$
    ARROW_LEFT_ALL("/icons/misc/arrow_left_all.png"), //$NON-NLS-1$ //$NON-NLS-2$
    ARROW_RIGHT("/icons/misc/arrow_right.png"), //$NON-NLS-1$ //$NON-NLS-2$
    ARROW_RIGHT_ALL("/icons/misc/arrow_right_all.png"), //$NON-NLS-1$ //$NON-NLS-2$
    ARROW_RESET("/icons/misc/arrow_reset.png"), //$NON-NLS-1$ //$NON-NLS-2$
    SORT_INCREASE("/icons/misc/sort_increase.png"), //$NON-NLS-1$ //$NON-NLS-2$
    SORT_DECREASE("/icons/misc/sort_decrease.png"), //$NON-NLS-1$ //$NON-NLS-2$
    SORT_UNKNOWN("/icons/misc/sort_unknown.png"), //$NON-NLS-1$ //$NON-NLS-2$
    FRAME_QUERY("/icons/misc/frame_query.png"), //$NON-NLS-1$ //$NON-NLS-2$
    FILTER("/icons/misc/filter.png"), //$NON-NLS-1$ //$NON-NLS-2$
    EVENT("/icons/misc/event.png"), //$NON-NLS-1$ //$NON-NLS-2$
    HOME("/icons/misc/home.png"), //$NON-NLS-1$ //$NON-NLS-2$
    COMPILE("/icons/misc/compile.png"), //$NON-NLS-1$ //$NON-NLS-2$
    COMPILE_LOG("/icons/misc/compile_log.png"), //$NON-NLS-1$ //$NON-NLS-2$

    SAVE("/icons/file/save.png"), //$NON-NLS-1$ //$NON-NLS-2$
    SAVE_ALL("/icons/file/save_all.png"), //$NON-NLS-1$ //$NON-NLS-2$
    SAVE_AS("/icons/file/save_as.png"), //$NON-NLS-1$ //$NON-NLS-2$
    LOAD("/icons/file/load.png"), //$NON-NLS-1$ //$NON-NLS-2$
    RESET("/icons/file/reset.png"), //$NON-NLS-1$ //$NON-NLS-2$
    ACCEPT("/icons/sql/accept.png"), //$NON-NLS-1$ //$NON-NLS-2$
    REJECT("/icons/sql/cancel.png"), //$NON-NLS-1$ //$NON-NLS-2$
    REVERT("/icons/sql/revert.png"), //$NON-NLS-1$ //$NON-NLS-2$
    IMPORT("/icons/file/import.png"), //$NON-NLS-1$ //$NON-NLS-2$
    EXPORT("/icons/file/export.png"), //$NON-NLS-1$ //$NON-NLS-2$
    REFRESH("/icons/refresh.png"), //$NON-NLS-1$ //$NON-NLS-2$
    JAR("/icons/misc/jar.png"), //$NON-NLS-1$ //$NON-NLS-2$
    SOURCES("/icons/misc/sources.png"), //$NON-NLS-1$ //$NON-NLS-2$

    ROW_ADD("/icons/sql/row_add.png"), //$NON-NLS-1$ //$NON-NLS-2$
    ROW_COPY("/icons/sql/row_copy.png"), //$NON-NLS-1$ //$NON-NLS-2$
    ROW_EDIT("/icons/sql/row_edit.png"), //$NON-NLS-1$ //$NON-NLS-2$
    ROW_DELETE("/icons/sql/row_delete.png"), //$NON-NLS-1$ //$NON-NLS-2$

    EDIT_DATABSE("/icons/misc/edit_database.png"), //$NON-NLS-1$ //$NON-NLS-2$
    EDIT_TABLE("/icons/misc/edit_table.png"), //$NON-NLS-1$ //$NON-NLS-2$
    EDIT_COLUMN("/icons/misc/edit_column.png"), //$NON-NLS-1$ //$NON-NLS-2$
    CONFIG_TABLE("/icons/misc/config_table.png"), //$NON-NLS-1$ //$NON-NLS-2$
    LOCKED("/icons/misc/locked.png"), //$NON-NLS-1$ //$NON-NLS-2$

    TYPE_BOOLEAN("/icons/sql/types/boolean.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TYPE_NUMBER("/icons/sql/types/number.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TYPE_STRING("/icons/sql/types/string.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TYPE_DATETIME("/icons/sql/types/datetime.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TYPE_BINARY("/icons/sql/types/binary.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TYPE_TEXT("/icons/sql/types/text.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TYPE_XML("/icons/sql/types/xml.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TYPE_LOB("/icons/sql/types/lob.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TYPE_ARRAY("/icons/sql/types/array.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TYPE_STRUCT("/icons/sql/types/struct.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TYPE_OBJECT("/icons/sql/types/object.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TYPE_IMAGE("/icons/sql/types/image.png"), //$NON-NLS-1$ //$NON-NLS-2$
    TYPE_UNKNOWN("/icons/sql/types/unknown.png"), //$NON-NLS-1$ //$NON-NLS-2$

    SQL_CONNECT("/icons/sql/connect.png"), //$NON-NLS-1$ //$NON-NLS-2$
    SQL_DISCONNECT("/icons/sql/disconnect.png"), //$NON-NLS-1$ //$NON-NLS-2$
    SQL_SCRIPT("/icons/sql/sql_script.png"), //$NON-NLS-1$ //$NON-NLS-2$
    SQL_EXECUTE("/icons/sql/sql_exec.png"), //$NON-NLS-1$ //$NON-NLS-2$
    SQL_SCRIPT_EXECUTE("/icons/sql/sql_script_exec.png"), //$NON-NLS-1$ //$NON-NLS-2$
    SQL_ANALYSE("/icons/sql/sql_analyse.png"), //$NON-NLS-1$ //$NON-NLS-2$
    SQL_EXPLAIN_PLAN("/icons/sql/sql_plan.png"), //$NON-NLS-1$ //$NON-NLS-2$
    SQL_VALIDATE("/icons/sql/sql_validate.png"), //$NON-NLS-1$ //$NON-NLS-2$
    SQL_PREVIEW("/icons/sql/sql_preview.png"), //$NON-NLS-1$ //$NON-NLS-2$
    SQL_TEXT("/icons/sql/sql_text.png"), //$NON-NLS-1$ //$NON-NLS-2$
    SAVE_TO_DATABASE("/icons/sql/save_to_database.png"), //$NON-NLS-1$ //$NON-NLS-2$

    OVER_SUCCESS("/icons/over/success_ovr.png"), //$NON-NLS-1$ //$NON-NLS-2$
    OVER_FAILED("/icons/over/failed_ovr.png"), //$NON-NLS-1$ //$NON-NLS-2$
    OVER_ERROR("/icons/over/error_ovr.png"), //$NON-NLS-1$ //$NON-NLS-2$
    OVER_UNKNOWN("/icons/over/conditional_ovr.png"), //$NON-NLS-1$ //$NON-NLS-2$
    OVER_LAMP("/icons/over/lamp_ovr.png"), //$NON-NLS-1$ //$NON-NLS-2$
    OVER_KEY("/icons/over/key_ovr.png"), //$NON-NLS-1$ //$NON-NLS-2$
    OVER_REFERENCE("/icons/over/reference_ovr.png"), //$NON-NLS-1$ //$NON-NLS-2$

    ABOUT("/icons/about_circle.png"); //$NON-NLS-1$ //$NON-NLS-2$

    private final String path;

    DBIcon(String path)
    {
        this.path = path;
    }

    public String getToken()
    {
        return name();
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

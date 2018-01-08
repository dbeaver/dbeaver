/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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

package org.jkiss.dbeaver.model;

import org.eclipse.core.runtime.FileLocator;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * DBIcon
 */
public class DBIcon implements DBPImage
{
    private static final Log log = Log.getLog(DBIcon.class);

    public static final DBIcon TREE_ADMIN = new DBIcon("admin", "tree/admin.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon TREE_DATABASE = new DBIcon("database", "tree/database.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon TREE_DATABASE_CATEGORY = new DBIcon("database_category", "tree/database_category.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon TREE_SCHEMA = new DBIcon("schema", "tree/schema.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon TREE_SCHEMA_SYSTEM = new DBIcon("schema_system", "tree/schema_system.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon TREE_SCHEMA_UTIL = new DBIcon("schema_util", "tree/schema_util.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon TREE_TABLE = new DBIcon("table", "tree/table.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon TREE_TABLE_ALIAS = new DBIcon("table_alias", "tree/table_alias.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon TREE_TABLE_LINK = new DBIcon("table_link", "tree/table_link.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon TREE_TABLE_INDEX = new DBIcon("table_index", "tree/table_index.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon TREE_VIEW = new DBIcon("view", "tree/view.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon TREE_FUNCTION = new DBIcon("function", "tree/function.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon TREE_PROCEDURE = new DBIcon("procedure", "tree/procedure.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon TREE_COLUMNS = new DBIcon("columns", "tree/columns.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon TREE_COLUMN = new DBIcon("column", "tree/column.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon TREE_INDEX = new DBIcon("index", "tree/index.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon TREE_CONSTRAINT = new DBIcon("constraint", "tree/constraint.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon TREE_UNIQUE_KEY = new DBIcon("unique-key", "tree/unique_constraint.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon TREE_FOREIGN_KEY = new DBIcon("foreign-key", "tree/foreign_key.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon TREE_FOREIGN_KEY_COLUMN = new DBIcon("foreign-key-column", "tree/foreign_key_column.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon TREE_REFERENCE = new DBIcon("reference", "tree/reference.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon TREE_TRIGGER = new DBIcon("trigger", "tree/trigger.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon TREE_USER = new DBIcon("user", "tree/user.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon TREE_USER_GROUP = new DBIcon("user_group", "tree/group.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon TREE_PERMISSIONS = new DBIcon("permissions", "tree/permissions.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon TREE_PAGE = new DBIcon("page", "tree/page.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon TREE_INFO = new DBIcon("info", "tree/info.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon TREE_SESSIONS = new DBIcon("sessions", "tree/sessions.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon TREE_LOCKS = new DBIcon("locks", "tree/locks.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon TREE_PACKAGE = new DBIcon("package", "tree/package.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon TREE_DATA_TYPE = new DBIcon("data_type", "tree/data_type.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon TREE_SEQUENCE = new DBIcon("sequence", "tree/sequence.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon TREE_SYNONYM = new DBIcon("synonym", "tree/synonym.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon TREE_TABLESPACE = new DBIcon("tablespace", "tree/tablespace.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon TREE_PARTITION = new DBIcon("partition", "tree/partition.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon TREE_ATTRIBUTE = new DBIcon("attribute", "tree/attribute.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon TREE_ARGUMENT = new DBIcon("argument", "tree/argument.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon TREE_JAVA_CLASS = new DBIcon("javaClass", "tree/java_class.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon TREE_JAVA_INTERFACE = new DBIcon("javaInterface", "tree/java_interface.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon TREE_LINK = new DBIcon("link", "tree/link.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon TREE_FILE = new DBIcon("file", "tree/data_file.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon TREE_CLASS = new DBIcon("class", "tree/class.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon TREE_ASSOCIATION = new DBIcon("association", "tree/association.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon TREE_SERVER = new DBIcon("server", "tree/server.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon TREE_SERVERS = new DBIcon("servers", "tree/servers.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon TREE_RECYCLE_BIN = new DBIcon("recycle_bin", "tree/recycle_bin.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon TREE_EVENT = new DBIcon("event", "tree/event.png"); //$NON-NLS-1$ //$NON-NLS-2$

    public static final DBIcon TREE_FOLDER = new DBIcon("folder", "tree/folder.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon TREE_FOLDER_LINK = new DBIcon("folder_link", "tree/folder_link.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon TREE_FOLDER_DATABASE = new DBIcon("folder_database", "tree/folder_database.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon TREE_FOLDER_SCHEMA = new DBIcon("folder_schema", "tree/folder_schema.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon TREE_FOLDER_TABLE = new DBIcon("folder_table", "tree/folder_table.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon TREE_FOLDER_VIEW = new DBIcon("folder_view", "tree/folder_view.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon TREE_FOLDER_USER = new DBIcon("folder_user", "tree/folder_user.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon TREE_FOLDER_ADMIN = new DBIcon("folder_admin", "tree/folder_admin.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon TREE_FOLDER_CONSTRAINT = new DBIcon("constraints", "tree/folder_constraint.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon TREE_FOLDER_INFO = new DBIcon("folder_info", "tree/folder_info.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon TREE_KEY = new DBIcon("key", "tree/key.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon TREE_FOLDER_KEY = new DBIcon("folder_key", "tree/folder_key.png"); //$NON-NLS-1$ //$NON-NLS-2$

    public static final DBIcon PROJECT = new DBIcon("project", "project.png"); //$NON-NLS-1$ //$NON-NLS-2$

    public static final DBIcon TYPE_BOOLEAN = new DBIcon("boolean", "types/boolean.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon TYPE_NUMBER = new DBIcon("number", "types/number.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon TYPE_STRING = new DBIcon("string", "types/string.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon TYPE_DATETIME = new DBIcon("datetime", "types/datetime.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon TYPE_BINARY = new DBIcon("binary", "types/binary.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon TYPE_TEXT = new DBIcon("text", "types/text.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon TYPE_JSON = new DBIcon("json", "types/json.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon TYPE_XML = new DBIcon("xml", "types/xml.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon TYPE_LOB = new DBIcon("lob", "types/lob.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon TYPE_ARRAY = new DBIcon("array", "types/array.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon TYPE_STRUCT = new DBIcon("struct", "types/struct.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon TYPE_DOCUMENT = new DBIcon("document", "types/document.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon TYPE_OBJECT = new DBIcon("object", "types/object.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon TYPE_IMAGE = new DBIcon("image", "types/image.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon TYPE_REFERENCE = new DBIcon("reference", "types/reference.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon TYPE_ROWID = new DBIcon("rowid", "types/rowid.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon TYPE_ANY = new DBIcon("any", "types/any.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon TYPE_UUID = new DBIcon("uuid", "types/uuid.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon TYPE_UNKNOWN = new DBIcon("unknown", "types/unknown.png"); //$NON-NLS-1$ //$NON-NLS-2$

    public static final DBIcon OVER_SUCCESS = new DBIcon("over_success", "over/success_ovr.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon OVER_FAILED = new DBIcon("over_failed", "over/failed_ovr.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon OVER_ERROR = new DBIcon("over_error", "over/error_ovr.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon OVER_UNKNOWN = new DBIcon("over_condition", "over/conditional_ovr.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon OVER_LAMP = new DBIcon("over_lamp", "over/lamp_ovr.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon OVER_KEY = new DBIcon("over_key", "over/key_ovr.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon OVER_LOCK = new DBIcon("over_lock", "over/lock_ovr.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon OVER_EXTERNAL = new DBIcon("over_external", "over/external_ovr.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon OVER_REFERENCE = new DBIcon("over_reference", "over/reference_ovr.png"); //$NON-NLS-1$ //$NON-NLS-2$
    public static final DBIcon OVER_FOLDER = new DBIcon("over_folder", "over/folder_ovr.png"); //$NON-NLS-1$ //$NON-NLS-2$

    private static Map<String, DBPImage> iconMap = new HashMap<>();


    static  {
        loadIcons(DBIcon.class);
    }

    private final String token;
    private String path;

    static public void loadIcons(Class<?> aClass) {
        Bundle iconBundle = FrameworkUtil.getBundle(aClass);
        if (iconBundle == null) {
            log.error("Can't find bundle for class '" + aClass.getName() + "'");
            return;
        }
        for (Field field : aClass.getDeclaredFields()) {
            if ((field.getModifiers() & Modifier.STATIC) == 0 || field.getType() != DBIcon.class) {
                continue;
            }
            try {
                DBIcon icon = (DBIcon) field.get(null);
                if (!icon.path.startsWith("platform:")) {
                    icon.path = "platform:/plugin/" + iconBundle.getSymbolicName() + "/icons/" + icon.path;
                }
                URL fileURL = FileLocator.toFileURL(new URL(icon.path));
                try {
                    URI filePath = GeneralUtils.makeURIFromFilePath(fileURL.toString());
                    File file = new File(filePath);
                    if (!file.exists()) {
                        log.warn("Bad image '" + icon.getToken() + "' location: " + icon.getLocation());
                        continue;
                    }
                    DBIcon.iconMap.put(icon.getToken(), icon);
                } catch (URISyntaxException e) {
                    throw new IOException("Bad local file path: " + fileURL, e);
                }
            } catch (Exception e) {
                log.error(e);
            }
        }
    }

    public DBIcon(String path)
    {
        this.token = null;
        this.path = path;
    }

    public DBIcon(String token, String path)
    {
        this.token = token;
        this.path = path;
    }

    public static DBPImage getImageById(String token)
    {
        return iconMap.get(token);
    }


    /**
     * Token is icon id. It can be used to refer on icons in plugin extensions
     * @return unique token
     */
    public String getToken()
    {
        return token;
    }

    @Override
    public String getLocation() {
        return path;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof DBIcon) {
            return CommonUtils.equalObjects(token, ((DBIcon) obj).token) &&
                CommonUtils.equalObjects(path, ((DBIcon) obj).path);
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        return token + ":" + path;
    }


}

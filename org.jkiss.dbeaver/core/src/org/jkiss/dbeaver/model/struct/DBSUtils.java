/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.struct;

import net.sf.jkiss.utils.CommonUtils;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.util.Collection;
import java.util.List;

/**
 * DBSUtils
 */
public final class DBSUtils
{

    public static String getQuotedIdentifier(DBPDataSource dataSource, String str)
    {
        String quoteString;
        quoteString = dataSource.getInfo().getIdentifierQuoteString();
        String delimString;
        delimString = dataSource.getInfo().getCatalogSeparator();

        boolean hasBadChars = false;
        for (int i = 0; i < str.length(); i++) {
            if (!Character.isUnicodeIdentifierPart(str.charAt(i)) && delimString.indexOf(str.charAt(i)) == -1) {
                hasBadChars = true;
                break;
            }
        } 

        if (!hasBadChars) {
            return str;
        }
        if (quoteString != null && quoteString.length() != 0 && !quoteString.equals(" ")) {
            str = quoteString + str + quoteString;
        }
        return str;
    }

    public static String getFullTableName(DBPDataSource dataSource, String catalogName, String schemaName, String tableName)
    {
        String catalogSeparator = dataSource.getInfo().getCatalogSeparator();
        StringBuilder name=  new StringBuilder();
        if (!CommonUtils.isEmpty(catalogName)) {
            name.append(DBSUtils.getQuotedIdentifier(dataSource, catalogName)).append(catalogSeparator);
        }
        if (!CommonUtils.isEmpty(schemaName)) {
            name.append(DBSUtils.getQuotedIdentifier(dataSource, schemaName)).append(catalogSeparator);
        }
        name.append(DBSUtils.getQuotedIdentifier(dataSource, tableName));
        return name.toString();
    }

    public static DBSObject getTableByPath(DBRProgressMonitor monitor, DBSStructureContainer rootSC, DBSTablePath path)
        throws DBException
    {
        return getObjectByPath(monitor, rootSC, path.getCatalogName(), path.getSchemaName(), path.getTableName());
    }

    public static DBSObject getObjectByPath(DBRProgressMonitor monitor, DBSStructureContainer rootSC,
                                            String catalogName, String schemaName, String tableName)
        throws DBException
    {
        if (!CommonUtils.isEmpty(catalogName)) {
            DBSObject catalog = rootSC.getChild(monitor, catalogName);
            if (!(catalog instanceof DBSStructureContainer)) {
                return null;
            }
            rootSC = (DBSStructureContainer) catalog;
        }
        if (!CommonUtils.isEmpty(schemaName)) {
            DBSObject schema = rootSC.getChild(monitor, schemaName);
            if (!(schema instanceof DBSStructureContainer)) {
                return null;
            }
            rootSC = (DBSStructureContainer) schema;
        }
        return rootSC.getChild(monitor, tableName);
    }

    public static DBSObject findNestedObject(DBRProgressMonitor monitor, DBSStructureContainer parent,
                                             List<String> names
    )
        throws DBException
    {
        for (int i = 0; i < names.size(); i++) {
            String childName = names.get(i);
            DBSObject child = parent.getChild(monitor, childName);
            if (child == null) {
                break;
            }
            if (i == names.size() - 1) {
                return child;
            }
            if (child instanceof DBSStructureContainer) {
                parent = DBSStructureContainer.class.cast(child);
            } else {
                break;
            }
        }
        return null;
    }

    /**
     * Finds object by its name (case insensitive)
     * @param theList object list
     * @param objectName object name
     * @return object or null
     */
    public static <T extends DBSObject> T findObject(Collection<T> theList, String objectName)
    {
        if (!CommonUtils.isEmpty(theList)) {
            for (T object : theList) {
                if (object.getName().equalsIgnoreCase(objectName)) {
                    return object;
                }
            }
        }
        return null;
    }

    public static <T> T queryParentInterface(Class<T> i, DBSObject object)
    {
        if (object == null || object.getParentObject() == null) {
            return null;
        }
        DBSObject parent = object.getParentObject();
        if (i.isAssignableFrom(parent.getClass())) {
            return i.cast(object.getParentObject());
        } else if (parent instanceof DBSDataSourceContainer && object.getDataSource() != null && i.isAssignableFrom(object.getDataSource().getClass())) {
            // Root object's parent is data source container (not datasource)
            // So try to extract this info from real datasource object
            return i.cast(object.getDataSource());
        } else {
            return null;
        }
    }
/*
    public static <T> Collection<T> getSafeCollection(Class<T> theClass, Collection theList)
    {
        if (theList == null) {
            return Collections.emptyList();
        }
        return (Collection<T>)theList;
    }
*/
}

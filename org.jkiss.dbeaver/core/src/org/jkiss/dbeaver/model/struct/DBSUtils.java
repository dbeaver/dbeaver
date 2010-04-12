package org.jkiss.dbeaver.model.struct;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSource;
import net.sf.jkiss.utils.CommonUtils;

import java.util.List;
import java.util.Collection;

/**
 * DBSUtils
 */
public final class DBSUtils
{

    public static String getQuotedIdentifier(DBPDataSource dataSource, String str)
    {
        String quoteString;
        try {
            quoteString = dataSource.getInfo().getIdentifierQuoteString();
        } catch (DBException e) {
            quoteString = " ";
        }
        String delimString;
        try {
            delimString = dataSource.getInfo().getCatalogSeparator();
        } catch (DBException e) {
            delimString = ".";
        }

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

    public static DBSObject getTableByPath(DBSStructureContainer rootSC, DBSTablePath path)
        throws DBException
    {
        return getObjectByPath(rootSC, path.getCatalogName(), path.getSchemaName(), path.getTableName());
    }

    public static DBSObject getObjectByPath(DBSStructureContainer rootSC, String catalogName, String schemaName, String tableName)
        throws DBException
    {
        if (!CommonUtils.isEmpty(catalogName)) {
            DBSObject catalog = rootSC.getChild(catalogName);
            if (!(catalog instanceof DBSStructureContainer)) {
                return null;
            }
            rootSC = (DBSStructureContainer) catalog;
        }
        if (!CommonUtils.isEmpty(schemaName)) {
            DBSObject schema = rootSC.getChild(schemaName);
            if (!(schema instanceof DBSStructureContainer)) {
                return null;
            }
            rootSC = (DBSStructureContainer) schema;
        }
        return rootSC.getChild(tableName);
    }

    public static DBSObject findNestedObject(DBSStructureContainer parent, List<String> names)
        throws DBException
    {
        for (int i = 0; i < names.size(); i++) {
            String childName = names.get(i);
            DBSObject child = parent.getChild(childName);
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

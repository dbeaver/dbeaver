/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.struct;

import net.sf.jkiss.utils.CommonUtils;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.dbc.DBCColumnMetaData;
import org.jkiss.dbeaver.model.data.DBDValue;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;

import java.util.Collection;
import java.util.List;
import java.lang.reflect.InvocationTargetException;

/**
 * DBSUtils
 */
public final class DBSUtils {

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

    public static String getFullTableName(DBPDataSource dataSource, String catalogName, String schemaName,
                                          String tableName)
    {
        String catalogSeparator = dataSource.getInfo().getCatalogSeparator();
        StringBuilder name = new StringBuilder();
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
     *
     * @param theList    object list
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
        } else if (parent instanceof DBSDataSourceContainer && object.getDataSource() != null && i.isAssignableFrom(
            object.getDataSource().getClass())) {
            // Root object's parent is data source container (not datasource)
            // So try to extract this info from real datasource object
            return i.cast(object.getDataSource());
        } else {
            return null;
        }
    }

    public static boolean isNullValue(Object value)
    {
        return (value == null || (value instanceof DBDValue && ((DBDValue) value).isNull()));
    }

    public static Object makeNullValue(Object value)
    {
        if (value instanceof DBDValue) {
            return ((DBDValue) value).makeNull();
        } else {
            return null;
        }
    }

    public static DBSTableColumn getUniqueReferenceColumn(DBCColumnMetaData column)
    {
        return getUniqueReferenceColumn(null, column);
    }

    public static DBSTableColumn getUniqueReferenceColumn(DBRProgressMonitor monitor, DBCColumnMetaData column)
    {
        RefColumnFinder finder = new RefColumnFinder(column);
        try {
            if (monitor != null) {
                finder.run(monitor);
            } else {
                DBeaverCore.getInstance().runAndWait2(true, true, finder);
            }
        }
        catch (InvocationTargetException e) {
            // ignore
        }
        catch (InterruptedException e) {
            // do nothing
        }
        return finder.refTableColumn;
    }

    private static class RefColumnFinder implements DBRRunnableWithProgress {
        private DBCColumnMetaData column;
        private DBSTableColumn refTableColumn;

        private RefColumnFinder(DBCColumnMetaData column)
        {
            this.column = column;
        }

        public void run(DBRProgressMonitor monitor)
            throws InvocationTargetException, InterruptedException
        {
            try {
                List<DBSForeignKey> refs = column.getForeignKeys(monitor, true);
                if (refs != null && !refs.isEmpty()) {
                    DBSForeignKey fk = refs.get(0);
                    DBSConstraint refKey = fk.getReferencedKey();
                    if (refKey != null) {
                        DBSTable refTable = refKey.getTable();
                        if (refTable != null) {
                            Collection<? extends DBSConstraintColumn> refColumns = refKey.getColumns(monitor);
                            if (refColumns != null && refColumns.size() == 1) {
                                DBSConstraintColumn refColumn = refColumns.iterator().next();
                                DBSTableColumn refTableColumn = refColumn.getTableColumn();
                                if (refTableColumn != null) {
                                    this.refTableColumn = refTableColumn;
                                }
                            }
                        }
                    }

                }
            }
            catch (DBException e) {
                throw new InvocationTargetException(e);
            }
        }
    }
}

/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.postgresql.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.List;

/**
* Dependency
*/
public class PostgreDependency implements PostgreObject, DBPOverloadedObject, DBPImageProvider {

    private static final Log log = Log.getLog(PostgreDependency.class);

    private final PostgreDatabase database;
    private final long objectId;
    private String depType;
    private String name;
    private String description;
    private String objectType;
    private String tableName;
    private String schemaName;
    private PostgreObject targetObject;

    public PostgreDependency(PostgreDatabase database, long objectId, String depType, String name, String description, String objectType, String tableName, String schemaName) {
        this.database = database;
        this.objectId = objectId;
        this.depType = depType;
        this.name = name;
        this.description = description;
        this.objectType = objectType;
        this.tableName = tableName;
        this.schemaName = schemaName;
    }

    @Override
    @Property(viewable = false)
    public long getObjectId() {
        return objectId;
    }

    @Override
    public boolean isPersisted() {
        return true;
    }

    @NotNull
    @Override
    public PostgreDataSource getDataSource() {
        return database.getDataSource();
    }

    @NotNull
    @Override
    public PostgreDatabase getDatabase() {
        return database;
    }

    @Override
    @Property(viewable = true, order = 1)
    public String getName() {
        return name;
    }

    @Override
    public String getOverloadedName() {
        return name + " (" + depType + ")";
    }

    @Property(viewable = true, order = 2)
    public String getObjectType() {
        if (objectType.startsWith("i")) {
            return "Index";
        } else if (objectType.startsWith("R")) {
            return "Rule";
        } else if (objectType.startsWith("C")) {
            if (objectType.endsWith("f")) {
                return "Foreign Key";
            } else if (objectType.endsWith("p")) {
                return "Primary Key";
            } else {
                return "Constraint";
            }
        } else if (objectType.startsWith("r")) {
            return "Table";
        } else if (objectType.startsWith("A")) {
            return "Attribute";
        }
        return objectType;
    }

    @Property(viewable = true, order = 3)
    public String getTableName() {
        return tableName;
    }

    @Property(viewable = true, order = 4)
    public String getSchemaName() {
        return schemaName;
    }

    public PostgreObject getTargetObject() {
        return targetObject;
    }

    private void setTargetObject(PostgreObject targetObject) {
        this.targetObject = targetObject;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public DBPImage getObjectImage() {
        if (objectType.startsWith("i")) {
            return DBIcon.TREE_INDEX;
        } else if (objectType.startsWith("R")) {
            return DBIcon.TREE_FUNCTION;
        } else if (objectType.startsWith("C")) {
            if (objectType.endsWith("f")) {
                return DBIcon.TREE_FOREIGN_KEY;
            } else if (objectType.endsWith("p")) {
                return DBIcon.TREE_UNIQUE_KEY;
            } else {
                return DBIcon.TREE_CONSTRAINT;
            }
        } else if (objectType.startsWith("r")) {
            return DBIcon.TREE_TABLE;
        } else if (objectType.startsWith("A")) {
            return DBIcon.TREE_COLUMN;
        }
        return DBIcon.TREE_REFERENCE;
    }

    @Override
    public DBSObject getParentObject() {
        return database;
    }

    /**
     * Reads list of dependent objects.
     * SQL query originally copy-pasted from pgAdmin sources with some modifications.
     */
    public static List<PostgreDependency> readDependencies(DBRProgressMonitor monitor, PostgreObject object, boolean dependents) throws DBCException {
        List<PostgreDependency> dependencies = new ArrayList<>();

        try (JDBCSession session = DBUtils.openMetaSession(monitor, object, "Load object dependencies")) {
            String queryObjId = dependents ? "objid" : "refobjid";
            String condObjId = dependents ? "refobjid" : "objid";
            try (JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT DISTINCT dep.deptype, dep.classid, dep." + queryObjId + ", cl.relkind, attr.attname,pg_get_expr(ad.adbin, ad.adrelid) adefval,\n" +
                    "    CASE WHEN cl.relkind IS NOT NULL THEN cl.relkind || COALESCE(dep.objsubid::text, '')\n" +
                    "        WHEN tg.oid IS NOT NULL THEN 'T'::text\n" +
                    "        WHEN ty.oid IS NOT NULL THEN 'y'::text\n" +
                    "        WHEN ns.oid IS NOT NULL THEN 'n'::text\n" +
                    "        WHEN pr.oid IS NOT NULL THEN 'p'::text\n" +
                    "        WHEN la.oid IS NOT NULL THEN 'l'::text\n" +
                    "        WHEN rw.oid IS NOT NULL THEN 'R'::text\n" +
                    "        WHEN co.oid IS NOT NULL THEN 'C'::text || contype\n" +
                    "        WHEN ad.oid IS NOT NULL THEN 'A'::text\n" +
                    "        ELSE ''\n" +
                    "    END AS type,\n" +
                    "    COALESCE(coc.relname, clrw.relname) AS ownertable,\n" +
                    "    CASE WHEN cl.relname IS NOT NULL AND att.attname IS NOT NULL THEN cl.relname || '.' || att.attname\n" +
                    "    ELSE COALESCE(cl.relname, co.conname, pr.proname, tg.tgname, ty.typname, la.lanname, rw.rulename, ns.nspname)\n" +
                    "    END AS refname,\n" +
                    "    COALESCE(nsc.nspname, nso.nspname, nsp.nspname, nst.nspname, nsrw.nspname) AS nspname\n" +
                    "FROM pg_depend dep\n" +
                    "LEFT JOIN pg_class cl ON dep." + queryObjId + "=cl.oid\n" +
                    "LEFT JOIN pg_attribute att ON dep." + queryObjId + "=att.attrelid AND dep.objsubid=att.attnum\n" +
                    "LEFT JOIN pg_namespace nsc ON cl.relnamespace=nsc.oid\n" +
                    "LEFT JOIN pg_proc pr ON dep." + queryObjId + "=pr.oid\n" +
                    "LEFT JOIN pg_namespace nsp ON pr.pronamespace=nsp.oid\n" +
                    "LEFT JOIN pg_trigger tg ON dep." + queryObjId + "=tg.oid\n" +
                    "LEFT JOIN pg_type ty ON dep." + queryObjId + "=ty.oid\n" +
                    "LEFT JOIN pg_namespace nst ON ty.typnamespace=nst.oid\n" +
                    "LEFT JOIN pg_constraint co ON dep." + queryObjId + "=co.oid\n" +
                    "LEFT JOIN pg_class coc ON co.conrelid=coc.oid\n" +
                    "LEFT JOIN pg_namespace nso ON co.connamespace=nso.oid\n" +
                    "LEFT JOIN pg_rewrite rw ON dep." + queryObjId + "=rw.oid\n" +
                    "LEFT JOIN pg_class clrw ON clrw.oid=rw.ev_class\n" +
                    "LEFT JOIN pg_namespace nsrw ON clrw.relnamespace=nsrw.oid\n" +
                    "LEFT JOIN pg_language la ON dep." + queryObjId + "=la.oid\n" +
                    "LEFT JOIN pg_namespace ns ON dep." + queryObjId + "=ns.oid\n" +
                    "LEFT JOIN pg_attrdef ad ON ad.oid=dep." + queryObjId + "\n" +
                    "LEFT JOIN pg_attribute attr ON attr.attrelid=ad.adrelid and attr.attnum=ad.adnum\n" +
                    "WHERE dep." + condObjId + "=?\n" +
                    "ORDER BY type"))
            {
                dbStat.setLong(1, object.getObjectId());
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    while (dbResult.next()) {
                        String tableName = JDBCUtils.safeGetString(dbResult, "ownertable");
                        String schemaName = JDBCUtils.safeGetString(dbResult, "nspname");
                        String objName = JDBCUtils.safeGetString(dbResult, "refname");
                        if (CommonUtils.isEmpty(objName)) {
                            objName = JDBCUtils.safeGetString(dbResult, "attname");
                        } else if (!CommonUtils.isEmpty(tableName)) {
                            objName += " ON " + tableName;
                        }
                        String objDesc = JDBCUtils.safeGetString(dbResult, "adefval");
                        PostgreDependency dependency = new PostgreDependency(
                            object.getDatabase(),
                            JDBCUtils.safeGetLong(dbResult, queryObjId),
                            JDBCUtils.safeGetString(dbResult, "deptype"),
                            objName,
                            objDesc,
                            JDBCUtils.safeGetString(dbResult, "type"),
                            tableName,
                            schemaName);
                        dependencies.add(dependency);
                    }
                }
            }
        } catch (Exception e) {
            throw new DBCException("Error reading dependencies", e);
        }

        return dependencies;
    }

}
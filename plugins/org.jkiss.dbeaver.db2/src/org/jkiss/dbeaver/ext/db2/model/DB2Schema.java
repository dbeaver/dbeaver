/*
 * Copyright (C) 2013      Denis Forveille titou10.titou10@gmail.com
 * Copyright (C) 2010-2015 Serge Rieder serge@jkiss.org
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
package org.jkiss.dbeaver.ext.db2.model;

import org.jkiss.dbeaver.core.Log;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.db2.DB2Constants;
import org.jkiss.dbeaver.ext.db2.model.cache.DB2AliasCache;
import org.jkiss.dbeaver.ext.db2.model.cache.DB2IndexCache;
import org.jkiss.dbeaver.ext.db2.model.cache.DB2MaterializedQueryTableCache;
import org.jkiss.dbeaver.ext.db2.model.cache.DB2NicknameCache;
import org.jkiss.dbeaver.ext.db2.model.cache.DB2RoutineCache;
import org.jkiss.dbeaver.ext.db2.model.cache.DB2TableCache;
import org.jkiss.dbeaver.ext.db2.model.cache.DB2TableCheckConstraintCache;
import org.jkiss.dbeaver.ext.db2.model.cache.DB2TableForeignKeyCache;
import org.jkiss.dbeaver.ext.db2.model.cache.DB2TableReferenceCache;
import org.jkiss.dbeaver.ext.db2.model.cache.DB2TableUniqueKeyCache;
import org.jkiss.dbeaver.ext.db2.model.cache.DB2TriggerCache;
import org.jkiss.dbeaver.ext.db2.model.cache.DB2ViewCache;
import org.jkiss.dbeaver.ext.db2.model.dict.DB2OwnerType;
import org.jkiss.dbeaver.ext.db2.model.dict.DB2RoutineType;
import org.jkiss.dbeaver.ext.db2.model.dict.DB2YesNo;
import org.jkiss.dbeaver.ext.db2.model.fed.DB2Nickname;
import org.jkiss.dbeaver.ext.db2.model.module.DB2Module;
import org.jkiss.dbeaver.model.DBPRefreshableObject;
import org.jkiss.dbeaver.model.DBPSystemObject;
import org.jkiss.dbeaver.model.impl.DBSObjectCache;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectSimpleCache;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.rdb.DBSSchema;
import org.jkiss.utils.CommonUtils;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

/**
 * DB2Schema
 * 
 * @author Denis Forveille
 */
public class DB2Schema extends DB2GlobalObject implements DBSSchema, DBPRefreshableObject, DBPSystemObject {
    private static final Log LOG = Log.getLog(DB2Schema.class);

    private static final String C_SEQ = "SELECT * FROM SYSCAT.SEQUENCES WHERE SEQSCHEMA = ? AND SEQTYPE <> 'A' ORDER BY SEQNAME WITH UR";
    private static final String C_PKG = "SELECT * FROM SYSCAT.PACKAGES WHERE PKGSCHEMA = ? ORDER BY PKGNAME WITH UR";
    private static final String C_XSR = "SELECT * FROM SYSCAT.XSROBJECTS WHERE OBJECTSCHEMA = ? ORDER BY OBJECTNAME WITH UR";
    private static final String C_MOD = "SELECT * FROM SYSCAT.MODULES WHERE MODULESCHEMA = ? AND MODULETYPE <> 'A'  ORDER BY MODULENAME WITH UR";
    private static final String C_DTT = "SELECT * FROM SYSCAT.DATATYPES WHERE TYPESCHEMA = ? AND METATYPE <> 'S' ORDER BY TYPENAME WITH UR";
    private static final String C_DTT_97 = "SELECT * FROM SYSCAT.DATATYPES WHERE TYPESCHEMA = ? AND METATYPE <> 'S' AND TYPEMODULENAME IS NULL ORDER BY TYPENAME WITH UR";

    // DB2Schema's children
    private final DB2TableCache tableCache = new DB2TableCache();
    private final DB2ViewCache viewCache = new DB2ViewCache();
    private final DB2MaterializedQueryTableCache mqtCache = new DB2MaterializedQueryTableCache();
    private final DB2NicknameCache nicknameCache = new DB2NicknameCache();

    private final DBSObjectCache<DB2Schema, DB2Sequence> sequenceCache;
    private final DB2IndexCache indexCache = new DB2IndexCache();
    private final DB2TriggerCache triggerCache = new DB2TriggerCache();
    private final DB2AliasCache aliasCache = new DB2AliasCache();
    private final DBSObjectCache<DB2Schema, DB2Package> packageCache;
    private DBSObjectCache<DB2Schema, DB2XMLSchema> xmlSchemaCache;

    private final DB2RoutineCache udfCache = new DB2RoutineCache(DB2RoutineType.F);
    private final DB2RoutineCache methodCache = new DB2RoutineCache(DB2RoutineType.M);
    private final DB2RoutineCache procedureCache = new DB2RoutineCache(DB2RoutineType.P);
    private final DBSObjectCache<DB2Schema, DB2DataType> udtCache;
    private DBSObjectCache<DB2Schema, DB2Module> moduleCache;

    // DB2Table's children
    private final DB2TableUniqueKeyCache constraintCache = new DB2TableUniqueKeyCache(tableCache);
    private final DB2TableForeignKeyCache associationCache = new DB2TableForeignKeyCache(tableCache);
    private final DB2TableReferenceCache referenceCache = new DB2TableReferenceCache(tableCache);
    private final DB2TableCheckConstraintCache checkCache = new DB2TableCheckConstraintCache(tableCache);

    // Combined Cache for the content assist to work. Aliases being not DB2TableBase, they are alas not included...
    private Map<String, DB2TableBase> allKindOfTableCache;

    private String name;
    private String owner;
    private DB2OwnerType ownerType;
    private Timestamp createTime;
    private Integer auditPolicyID;
    private String auditPolicyName;
    private Boolean dataCapture;
    private String remarks;

    // ------------
    // Constructors
    // ------------
    public DB2Schema(DB2DataSource db2DataSource, ResultSet dbResult) throws DBException
    {
        this(db2DataSource, JDBCUtils.safeGetStringTrimmed(dbResult, "SCHEMANAME"));

        this.owner = JDBCUtils.safeGetString(dbResult, "OWNER");
        this.createTime = JDBCUtils.safeGetTimestamp(dbResult, "CREATE_TIME");
        this.remarks = JDBCUtils.safeGetString(dbResult, "REMARKS");

        if (db2DataSource.isAtLeastV9_5()) {
            this.ownerType = CommonUtils.valueOf(DB2OwnerType.class, JDBCUtils.safeGetString(dbResult, "OWNERTYPE"));
        }
        if (db2DataSource.isAtLeastV10_1()) {
            this.auditPolicyID = JDBCUtils.safeGetInteger(dbResult, "AUDITPOLICYID");
            this.auditPolicyName = JDBCUtils.safeGetString(dbResult, "AUDITPOLICYNAME");
            this.dataCapture = JDBCUtils.safeGetBoolean(dbResult, "DATACAPTURE", DB2YesNo.Y.name());
        }

    }

    public DB2Schema(DB2DataSource db2DataSource, String name)
    {
        super(db2DataSource, true);
        this.name = name;

        this.sequenceCache = new JDBCObjectSimpleCache<DB2Schema, DB2Sequence>(DB2Sequence.class, C_SEQ, name);
        this.packageCache = new JDBCObjectSimpleCache<DB2Schema, DB2Package>(DB2Package.class, C_PKG, name);
        this.xmlSchemaCache = new JDBCObjectSimpleCache<DB2Schema, DB2XMLSchema>(DB2XMLSchema.class, C_XSR, name);

        if (db2DataSource.isAtLeastV9_7()) {
            this.moduleCache = new JDBCObjectSimpleCache<DB2Schema, DB2Module>(DB2Module.class, C_MOD, name);
        }

        String datatypeSQL;
        if (db2DataSource.isAtLeastV9_7()) {
            datatypeSQL = C_DTT_97;
        } else {
            datatypeSQL = C_DTT;
        }
        this.udtCache = new JDBCObjectSimpleCache<DB2Schema, DB2DataType>(DB2DataType.class, datatypeSQL, name);

    }

    // -----------------
    // Business Contract
    // -----------------
    @Override
    public boolean isSystem()
    {
        if (getName().startsWith("SYS")) {
            return true;
        }
        if (getName().equals("DB2QP")) {
            return true;
        }
        if (getName().equals("SQLJ")) {
            return true;
        }
        if (getName().equals("NULLID")) {
            return true;
        }
        return false;
    }

    @Override
    public String toString()
    {
        return "Schema " + name;
    }

    @Override
    public synchronized void cacheStructure(DBRProgressMonitor monitor, int scope) throws DBException
    {
        if ((scope & STRUCT_ENTITIES) != 0) {
            monitor.subTask("Cache tables");
            tableCache.loadObjects(monitor, this);
            monitor.subTask("Cache Views");
            viewCache.getObjects(monitor, this);
            monitor.subTask("Cache MQTs");
            mqtCache.getObjects(monitor, this);
            monitor.subTask("Cache Nicknames");
            nicknameCache.getObjects(monitor, this);

            monitor.subTask("Cache Check Constraints");
            checkCache.getObjects(monitor, this);
            monitor.subTask("Cache Sequences");
            sequenceCache.getObjects(monitor, this);
            if (xmlSchemaCache != null) {
                monitor.subTask("Cache XML Schemas");
                xmlSchemaCache.getObjects(monitor, this);
            }
            if (moduleCache != null) {
                monitor.subTask("Cache Modules");
                moduleCache.getObjects(monitor, this);
            }
        }
        if ((scope & STRUCT_ATTRIBUTES) != 0) {
            monitor.subTask("Cache table columns");
            tableCache.loadChildren(monitor, this, null);
        }
        if ((scope & STRUCT_ASSOCIATIONS) != 0) {
            monitor.subTask("Cache table unique keys");
            constraintCache.getObjects(monitor, this, null);
            monitor.subTask("Cache table foreign keys");
            associationCache.getObjects(monitor, this, null);
            monitor.subTask("Cache table references");
            referenceCache.getObjects(monitor, this, null);
            monitor.subTask("Cache indexes");
            indexCache.getObjects(monitor, this);
        }
    }

    @Override
    public synchronized boolean refreshObject(DBRProgressMonitor monitor) throws DBException
    {
        tableCache.clearCache();
        viewCache.clearCache();
        mqtCache.clearCache();
        nicknameCache.clearCache();

        packageCache.clearCache();
        procedureCache.clearCache();
        udfCache.clearCache();
        udtCache.clearCache();
        sequenceCache.clearCache();
        aliasCache.clearCache();
        if (xmlSchemaCache != null) {
            xmlSchemaCache.clearCache();
        }
        if (moduleCache != null) {
            moduleCache.clearCache();
        }

        // For those 2, need to refresh dependent cache (cache for tables..?)
        indexCache.clearCache();
        triggerCache.clearCache();

        constraintCache.clearCache();
        associationCache.clearCache();
        referenceCache.clearCache();
        checkCache.clearCache();

        return true;
    }

    // --------------------------
    // Schema "Children" = Tables
    // --------------------------

    @Override
    public Class<DB2TableBase> getChildType(DBRProgressMonitor monitor) throws DBException
    {
        return DB2TableBase.class;
    }

    @Override
    public Collection<DB2TableBase> getChildren(DBRProgressMonitor monitor) throws DBException
    {
        // Build only once, a combined cache of ""Tables"" for content assist to work
        if (allKindOfTableCache == null) {

            allKindOfTableCache = new TreeMap<String, DB2TableBase>(); // TreeMap to keep things ordered

            for (DB2Table db2Table : tableCache.getObjects(monitor, this)) {
                allKindOfTableCache.put(db2Table.getName(), db2Table);
            }
            for (DB2View db2View : viewCache.getObjects(monitor, this)) {
                allKindOfTableCache.put(db2View.getName(), db2View);
            }
            for (DB2MaterializedQueryTable db2Mqt : mqtCache.getObjects(monitor, this)) {
                allKindOfTableCache.put(db2Mqt.getName(), db2Mqt);
            }
            for (DB2Nickname db2Nickname : nicknameCache.getObjects(monitor, this)) {
                allKindOfTableCache.put(db2Nickname.getName(), db2Nickname);
            }
        }
        return allKindOfTableCache.values();
    }

    @Override
    public DB2TableBase getChild(DBRProgressMonitor monitor, String childName) throws DBException
    {
        if (allKindOfTableCache == null) {
            getChildren(monitor);
        }
        return allKindOfTableCache.get(childName);
    }

    // -----------------
    // Associations
    // -----------------

    @Association
    public Collection<DB2Table> getTables(DBRProgressMonitor monitor) throws DBException
    {
        return tableCache.getTypedObjects(monitor, this, DB2Table.class);
    }

    public DB2Table getTable(DBRProgressMonitor monitor, String name) throws DBException
    {
        return tableCache.getObject(monitor, this, name, DB2Table.class);
    }

    @Association
    public Collection<DB2View> getViews(DBRProgressMonitor monitor) throws DBException
    {
        return viewCache.getTypedObjects(monitor, this, DB2View.class);
    }

    public DB2View getView(DBRProgressMonitor monitor, String name) throws DBException
    {
        return viewCache.getObject(monitor, this, name, DB2View.class);
    }

    @Association
    public Collection<DB2Nickname> getNicknames(DBRProgressMonitor monitor) throws DBException
    {
        return nicknameCache.getTypedObjects(monitor, this, DB2Nickname.class);
    }

    public DB2Nickname getNickname(DBRProgressMonitor monitor, String name) throws DBException
    {
        return nicknameCache.getObject(monitor, this, name, DB2Nickname.class);
    }

    @Association
    public Collection<DB2MaterializedQueryTable> getMaterializedQueryTables(DBRProgressMonitor monitor) throws DBException
    {
        return mqtCache.getTypedObjects(monitor, this, DB2MaterializedQueryTable.class);
    }

    public DB2MaterializedQueryTable getMaterializedQueryTable(DBRProgressMonitor monitor, String name) throws DBException
    {
        return mqtCache.getObject(monitor, this, name, DB2MaterializedQueryTable.class);
    }

    @Association
    public Collection<DB2Index> getIndexes(DBRProgressMonitor monitor) throws DBException
    {
        return indexCache.getObjects(monitor, this);
    }

    public DB2Index getIndex(DBRProgressMonitor monitor, String name) throws DBException
    {
        return indexCache.getObject(monitor, this, name, DB2Index.class);
    }

    @Association
    public Collection<DB2Trigger> getTriggers(DBRProgressMonitor monitor) throws DBException
    {
        return triggerCache.getObjects(monitor, this);
    }

    public DB2Trigger getTrigger(DBRProgressMonitor monitor, String name) throws DBException
    {
        return triggerCache.getObject(monitor, this, name, DB2Trigger.class);
    }

    @Association
    public Collection<DB2DataType> getUDTs(DBRProgressMonitor monitor) throws DBException
    {
        return udtCache.getObjects(monitor, this);
    }

    public DB2DataType getUDT(DBRProgressMonitor monitor, String name) throws DBException
    {
        return udtCache.getObject(monitor, this, name);
    }

    @Association
    public Collection<DB2Sequence> getSequences(DBRProgressMonitor monitor) throws DBException
    {
        return sequenceCache.getObjects(monitor, this);
    }

    public DB2Sequence getSequence(DBRProgressMonitor monitor, String name) throws DBException
    {
        return sequenceCache.getObject(monitor, this, name);
    }

    @Association
    public Collection<DB2XMLSchema> getXMLSchemas(DBRProgressMonitor monitor) throws DBException
    {
        return xmlSchemaCache.getObjects(monitor, this);
    }

    public DB2XMLSchema getXMLSchema(DBRProgressMonitor monitor, String name) throws DBException
    {
        return xmlSchemaCache.getObject(monitor, this, name);
    }

    @Association
    public Collection<DB2Alias> getAliases(DBRProgressMonitor monitor) throws DBException
    {
        return aliasCache.getObjects(monitor, this);
    }

    public DB2Alias getAlias(DBRProgressMonitor monitor, String name) throws DBException
    {
        return aliasCache.getObject(monitor, this, name, DB2Alias.class);
    }

    @Association
    public Collection<DB2Package> getPackages(DBRProgressMonitor monitor) throws DBException
    {
        return packageCache.getObjects(monitor, this);
    }

    public DB2Package getPackage(DBRProgressMonitor monitor, String name) throws DBException
    {
        return packageCache.getObject(monitor, this, name);
    }

    @Association
    public Collection<DB2Routine> getProcedures(DBRProgressMonitor monitor) throws DBException
    {
        return procedureCache.getObjects(monitor, this);
    }

    public DB2Routine getProcedure(DBRProgressMonitor monitor, String name) throws DBException
    {
        return procedureCache.getObject(monitor, this, name);
    }

    @Association
    public Collection<DB2Routine> getMethods(DBRProgressMonitor monitor) throws DBException
    {
        return methodCache.getObjects(monitor, this);
    }

    public DB2Routine getMethod(DBRProgressMonitor monitor, String name) throws DBException
    {
        return methodCache.getObject(monitor, this, name);
    }

    @Association
    public Collection<DB2Routine> getUDFs(DBRProgressMonitor monitor) throws DBException
    {
        return udfCache.getObjects(monitor, this);
    }

    public DB2Routine getUDF(DBRProgressMonitor monitor, String name) throws DBException
    {
        return udfCache.getObject(monitor, this, name);
    }

    @Association
    public Collection<DB2Module> getModules(DBRProgressMonitor monitor) throws DBException
    {
        return moduleCache.getObjects(monitor, this);
    }

    public DB2Module getModule(DBRProgressMonitor monitor, String name) throws DBException
    {
        return moduleCache.getObject(monitor, this, name);
    }

    // -----------------
    // Properties
    // -----------------

    @Override
    @Property(viewable = true, editable = false, order = 1)
    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    @Property(viewable = false, editable = false, category = DB2Constants.CAT_OWNER)
    public String getOwner()
    {
        return owner;
    }

    @Property(viewable = false, editable = false, category = DB2Constants.CAT_OWNER)
    public DB2OwnerType getOwnerType()
    {
        return ownerType;
    }

    @Property(viewable = true, editable = false, category = DB2Constants.CAT_DATETIME)
    public Timestamp getCreateTime()
    {
        return createTime;
    }

    @Property(viewable = false, editable = false, order = 7, category = DB2Constants.CAT_AUDIT)
    public Integer getAuditPolicyID()
    {
        return auditPolicyID;
    }

    @Property(viewable = false, editable = false, order = 8, category = DB2Constants.CAT_AUDIT)
    public String getAuditPolicyName()
    {
        return auditPolicyName;
    }

    @Property(viewable = false, editable = false)
    public Boolean getDataCapture()
    {
        return dataCapture;
    }

    @Nullable
    @Override
    @Property(viewable = false, editable = false)
    public String getDescription()
    {
        return remarks;
    }

    // -------------------------
    // Standards Getters
    // -------------------------

    public DB2TableCache getTableCache()
    {
        return tableCache;
    }

    public DB2ViewCache getViewCache()
    {
        return viewCache;
    }

    public DB2NicknameCache getNicknameCache()
    {
        return nicknameCache;
    }

    public DB2MaterializedQueryTableCache getMaterializedQueryTableCache()
    {
        return mqtCache;
    }

    public DBSObjectCache<DB2Schema, DB2DataType> getUdtCache()
    {
        return udtCache;
    }

    public DB2RoutineCache getUdfCache()
    {
        return udfCache;
    }

    public DBSObjectCache<DB2Schema, DB2Sequence> getSequenceCache()
    {
        return sequenceCache;
    }

    public DBSObjectCache<DB2Schema, DB2XMLSchema> getXMLSchemaCache()
    {
        return xmlSchemaCache;
    }

    public DB2AliasCache getAliasCache()
    {
        return aliasCache;
    }

    public DBSObjectCache<DB2Schema, DB2Package> getPackageCache()
    {
        return packageCache;
    }

    public DB2RoutineCache getProcedureCache()
    {
        return procedureCache;
    }

    public DB2IndexCache getIndexCache()
    {
        return indexCache;
    }

    public DB2TableUniqueKeyCache getConstraintCache()
    {
        return constraintCache;
    }

    public DB2TableForeignKeyCache getAssociationCache()
    {
        return associationCache;
    }

    public DB2TableReferenceCache getReferenceCache()
    {
        return referenceCache;
    }

    public DB2TriggerCache getTriggerCache()
    {
        return triggerCache;
    }

    public DB2TableCheckConstraintCache getCheckCache()
    {
        return checkCache;
    }

    public DBSObjectCache<DB2Schema, DB2Module> getModuleCache()
    {
        return moduleCache;
    }

    public DBSObjectCache<DB2Schema, DB2XMLSchema> getXmlSchemaCache()
    {
        return xmlSchemaCache;
    }

    public DBSObjectCache<DB2Schema, DB2Routine> getMethodCache()
    {
        return methodCache;
    }

}

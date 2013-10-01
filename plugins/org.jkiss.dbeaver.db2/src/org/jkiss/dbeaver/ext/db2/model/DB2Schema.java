/*
 * Copyright (C) 2013      Denis Forveille titou10.titou10@gmail.com
 * Copyright (C) 2010-2013 Serge Rieder serge@jkiss.org
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.db2.DB2Constants;
import org.jkiss.dbeaver.ext.db2.model.cache.DB2AliasCache;
import org.jkiss.dbeaver.ext.db2.model.cache.DB2IndexCache;
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
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureType;
import org.jkiss.dbeaver.model.struct.rdb.DBSSchema;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Collection;

/**
 * DB2Schema
 * 
 * @author Denis Forveille
 */
public class DB2Schema extends DB2GlobalObject implements DBSSchema, DBPRefreshableObject, DBPSystemObject {
    private static final Log LOG = LogFactory.getLog(DB2Schema.class);

    private static final String C_SEQ = "SELECT * FROM SYSCAT.SEQUENCES WHERE SEQSCHEMA = ? AND SEQTYPE <> 'A' ORDER BY SEQNAME WITH UR";
    private static final String C_PKG = "SELECT * FROM SYSCAT.PACKAGES WHERE PKGSCHEMA = ? ORDER BY PKGNAME WITH UR";
    private static final String C_DTT = "SELECT * FROM SYSCAT.DATATYPES WHERE TYPESCHEMA = ? AND METATYPE <> 'S' AND  TYPEMODULENAME IS NULL ORDER BY TYPENAME WITH UR";
    private static final String C_XSR = "SELECT * FROM SYSCAT.XSROBJECTS WHERE OBJECTSCHEMA = ? ORDER BY OBJECTNAME WITH UR";
    private static final String C_MOD = "SELECT * FROM SYSCAT.MODULES WHERE MODULESCHEMA = ? AND MODULETYPE <> 'A'  ORDER BY MODULENAME WITH UR";

    // DB2Schema's children
    private final DB2TableCache tableCache = new DB2TableCache();
    private final DB2ViewCache viewCache = new DB2ViewCache();
    private final DBSObjectCache<DB2Schema, DB2Sequence> sequenceCache;
    private final DB2IndexCache indexCache = new DB2IndexCache();
    private final DB2TriggerCache triggerCache = new DB2TriggerCache();
    private final DB2AliasCache aliasCache = new DB2AliasCache();
    private final DBSObjectCache<DB2Schema, DB2Package> packageCache;
    private final DB2NicknameCache nicknameCache = new DB2NicknameCache();
    private final DBSObjectCache<DB2Schema, DB2XMLSchema> xmlSchemaCache;

    private final DB2RoutineCache procedureCache = new DB2RoutineCache(DBSProcedureType.PROCEDURE);
    private final DB2RoutineCache udfCache = new DB2RoutineCache(DBSProcedureType.FUNCTION);
    private final DBSObjectCache<DB2Schema, DB2DataType> udtCache;
    private final DBSObjectCache<DB2Schema, DB2Module> moduleCache;

    // DB2Table's children
    private final DB2TableUniqueKeyCache constraintCache = new DB2TableUniqueKeyCache(tableCache);
    private final DB2TableForeignKeyCache associationCache = new DB2TableForeignKeyCache(tableCache);
    private final DB2TableReferenceCache referenceCache = new DB2TableReferenceCache(tableCache);
    private final DB2TableCheckConstraintCache checkCache = new DB2TableCheckConstraintCache(tableCache);

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
    public DB2Schema(DB2DataSource dataSource, String name)
    {
        super(dataSource, true);
        this.name = name;

        this.sequenceCache = new JDBCObjectSimpleCache<DB2Schema, DB2Sequence>(DB2Sequence.class, C_SEQ, name);
        this.packageCache = new JDBCObjectSimpleCache<DB2Schema, DB2Package>(DB2Package.class, C_PKG, name);
        this.udtCache = new JDBCObjectSimpleCache<DB2Schema, DB2DataType>(DB2DataType.class, C_DTT, name);
        this.xmlSchemaCache = new JDBCObjectSimpleCache<DB2Schema, DB2XMLSchema>(DB2XMLSchema.class, C_XSR, name);
        this.moduleCache = new JDBCObjectSimpleCache<DB2Schema, DB2Module>(DB2Module.class, C_MOD, name);
    }

    public DB2Schema(DB2DataSource dataSource, ResultSet dbResult)
    {
        this(dataSource, JDBCUtils.safeGetStringTrimmed(dbResult, "SCHEMANAME"));

        this.owner = JDBCUtils.safeGetString(dbResult, "OWNER");
        this.ownerType = DB2OwnerType.valueOf(JDBCUtils.safeGetString(dbResult, "OWNERTYPE"));
        this.createTime = JDBCUtils.safeGetTimestamp(dbResult, "CREATE_TIME");
        this.remarks = JDBCUtils.safeGetString(dbResult, "REMARKS");

        // DB2 v10.1+ columns
        this.auditPolicyID = JDBCUtils.safeGetInteger(dbResult, "AUDITPOLICYID");
        this.auditPolicyName = JDBCUtils.safeGetString(dbResult, "AUDITPOLICYNAME");
        this.dataCapture = JDBCUtils.safeGetBoolean(dbResult, "DATACAPTURE", DB2YesNo.Y.name());
    }

    @Override
    public boolean isSystem()
    {
        if (getName().startsWith("SYS")) {
            return true;
        }
        if (getName().equals("DB2QP")) {
            return true;
        }
        if (getName().equals("SESSION")) {
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
            monitor.subTask("Cache Check Constraints");
            checkCache.getObjects(monitor, this);
            monitor.subTask("Cache Sequences");
            sequenceCache.getObjects(monitor, this);
            monitor.subTask("Cache XML Schemas");
            xmlSchemaCache.getObjects(monitor, this);
            monitor.subTask("Cache Modules");
            moduleCache.getObjects(monitor, this);
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
        packageCache.clearCache();
        procedureCache.clearCache();
        udfCache.clearCache();
        udtCache.clearCache();
        sequenceCache.clearCache();
        aliasCache.clearCache();
        xmlSchemaCache.clearCache();
        moduleCache.clearCache();

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
    public Class<DB2Table> getChildType(DBRProgressMonitor monitor) throws DBException
    {
        return DB2Table.class;
    }

    @Override
    public Collection<DB2Table> getChildren(DBRProgressMonitor monitor) throws DBException
    {
        return tableCache.getObjects(monitor, this);
    }

    @Override
    public DB2Table getChild(DBRProgressMonitor monitor, String childName) throws DBException
    {
        return tableCache.getObject(monitor, this, childName);
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

    @Association
    public Collection<DB2Routine> getUDFs(DBRProgressMonitor monitor) throws DBException
    {
        return udfCache.getObjects(monitor, this);
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

}

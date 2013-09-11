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

import java.sql.SQLException;
import java.util.Collection;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IAdaptable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.ext.db2.DB2Constants;
import org.jkiss.dbeaver.ext.db2.DB2DataSourceProvider;
import org.jkiss.dbeaver.ext.db2.DB2Utils;
import org.jkiss.dbeaver.ext.db2.editors.DB2StructureAssistant;
import org.jkiss.dbeaver.ext.db2.model.cache.DB2BufferpoolCache;
import org.jkiss.dbeaver.ext.db2.model.cache.DB2DataTypeCache;
import org.jkiss.dbeaver.ext.db2.model.cache.DB2GroupCache;
import org.jkiss.dbeaver.ext.db2.model.cache.DB2RoleCache;
import org.jkiss.dbeaver.ext.db2.model.cache.DB2SchemaCache;
import org.jkiss.dbeaver.ext.db2.model.cache.DB2TablespaceCache;
import org.jkiss.dbeaver.ext.db2.model.cache.DB2UserCache;
import org.jkiss.dbeaver.ext.db2.model.plan.DB2PlanAnalyser;
import org.jkiss.dbeaver.model.DBPConnectionInfo;
import org.jkiss.dbeaver.model.DBPDataSourceInfo;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCDatabaseMetaData;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.exec.plan.DBCPlan;
import org.jkiss.dbeaver.model.exec.plan.DBCQueryPlanner;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSource;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSourceInfo;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataSourceContainer;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectSelector;
import org.jkiss.dbeaver.model.struct.DBSStructureAssistant;
import org.jkiss.dbeaver.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.ui.UIUtils;

/**
 * DB2 DataSource
 * 
 * @author Denis Forveille
 * 
 */
public class DB2DataSource extends JDBCDataSource implements DBSObjectSelector, DBCQueryPlanner, IAdaptable {

   private static final Log         LOG             = LogFactory.getLog(DB2DataSource.class);

   private static final String      PLAN_TABLE_TIT  = "PLAN_TABLE missing";
   private static final String      PLAN_TABLE_MSG  = "PLAN_TABLE not found in current schema nor in SYSTOOLS. Do you want DBeaver to create new PLAN_TABLE?";

   private final DB2SchemaCache     schemaCache     = new DB2SchemaCache();
   private final DB2DataTypeCache   dataTypeCache   = new DB2DataTypeCache();
   private final DB2BufferpoolCache bufferpoolCache = new DB2BufferpoolCache();
   private final DB2TablespaceCache tablespaceCache = new DB2TablespaceCache();
   private final DB2RoleCache       roleCache       = new DB2RoleCache();
   private final DB2UserCache       userCache       = new DB2UserCache();
   private final DB2GroupCache      groupCache      = new DB2GroupCache();

   private String                   activeSchemaName;
   private String                   planTableSchemaName;

   // -----------------------
   // Constructors
   // -----------------------

   public DB2DataSource(DBRProgressMonitor monitor, DBSDataSourceContainer container) throws DBException {
      super(monitor, container);
   }

   // -----------------------
   // Connection related Info
   // -----------------------

   @Override
   protected String getConnectionUserName(DBPConnectionInfo connectionInfo) {
      return connectionInfo.getUserName();
   }

   @Override
   // TODO DF strange...What is the role of this non static mathod?
   public DB2DataSource getDataSource() {
      return this;
   }

   @Override
   protected DBPDataSourceInfo makeInfo(JDBCDatabaseMetaData metaData) {
      final JDBCDataSourceInfo info = new JDBCDataSourceInfo(metaData);
      // TODO DF: Need to be reviewed
      for (String kw : DB2Constants.ADVANCED_KEYWORDS) {
         info.addSQLKeyword(kw);
      }
      return info;
   }

   @Override
   protected Map<String, String> getInternalConnectionProperties() {
      return DB2DataSourceProvider.getConnectionsProps();
   }

   @Override
   public void initialize(DBRProgressMonitor monitor) throws DBException {
      super.initialize(monitor);

      {
         final JDBCExecutionContext context = openContext(monitor, DBCExecutionPurpose.META, "Load data source meta info");
         try {
            // Get active schema
            this.activeSchemaName = JDBCUtils.queryString(context, "SELECT CURRENT_SCHEMA FROM SYSIBM.SYSDUMMY1");
            if (this.activeSchemaName != null) {
               this.activeSchemaName = this.activeSchemaName.trim();
            }
         } catch (SQLException e) {
            LOG.warn(e);
         } finally {
            context.close();
         }
      }
      this.dataTypeCache.getObjects(monitor, this);
   }

   @Override
   public boolean refreshObject(DBRProgressMonitor monitor) throws DBException {
      super.refreshObject(monitor);

      this.userCache.clearCache();
      this.groupCache.clearCache();
      this.roleCache.clearCache();
      this.tablespaceCache.clearCache();
      this.bufferpoolCache.clearCache();
      this.schemaCache.clearCache();
      this.dataTypeCache.clearCache();

      this.initialize(monitor);

      return true;
   }

   @Override
   public Collection<DB2DataType> getDataTypes() {
      // TODO DF: not sure it is the beqst way to do it
      try {
         return getDataTypes(VoidProgressMonitor.INSTANCE);
      } catch (DBException e) {
         LOG.error("DBException occurred when reading system dataTypes: ", e);
         return null;
      }
   }

   @Override
   public DB2DataType getDataType(String typeName) {
      // TODO DF: not sure it is the beqst way to do it
      try {
         return getDataType(VoidProgressMonitor.INSTANCE, typeName);
      } catch (DBException e) {
         LOG.error("DBException occurred when reading system dataTYpe : " + typeName, e);
         return null;
      }
   }

   // --------------
   // TODO DF: No idea what to do with those methods, what they are used for...
   // --------------

   @Override
   public void cacheStructure(DBRProgressMonitor monitor, int scope) throws DBException {
   }

   @Override
   public Object getAdapter(Class adapter) {
      if (adapter == DBSStructureAssistant.class) {
         return new DB2StructureAssistant(this);
      }
      return null;
   }

   // --------------------------
   // Manage Children: DB2Schema
   // --------------------------

   @Override
   public boolean supportsObjectSelect() {
      return true;
   }

   @Override
   public Class<? extends DB2Schema> getChildType(DBRProgressMonitor monitor) throws DBException {
      return DB2Schema.class;
   }

   @Override
   public Collection<DB2Schema> getChildren(DBRProgressMonitor monitor) throws DBException {
      return getSchemas(monitor);
   }

   @Override
   public DB2Schema getChild(DBRProgressMonitor monitor, String childName) throws DBException {
      return getSchema(monitor, childName);
   }

   @Override
   public DB2Schema getSelectedObject() {
      return activeSchemaName == null ? null : schemaCache.getCachedObject(activeSchemaName);
   }

   @Override
   public void selectObject(DBRProgressMonitor monitor, DBSObject object) throws DBException {
      final DB2Schema oldSelectedEntity = getSelectedObject();

      if (!(object instanceof DB2Schema)) {
         throw new IllegalArgumentException("Invalid object type: " + object);
      }

      activeSchemaName = object.getName();

      JDBCExecutionContext context = openContext(monitor, DBCExecutionPurpose.UTIL, "Set active schema");
      try {
         JDBCUtils.executeSQL(context, "SET CURRENT SCHEMA = " + activeSchemaName);
      } catch (SQLException e) {
         throw new DBException(e);
      } finally {
         context.close();
      }

      // Send notifications
      if (oldSelectedEntity != null) {
         DBUtils.fireObjectSelect(oldSelectedEntity, false);
      }
      if (this.activeSchemaName != null) {
         DBUtils.fireObjectSelect(object, true);
      }
   }

   // -------
   // Helpers
   // -------
   /**
    * 
    * 
    * @param monitor
    * @param parentSchema
    * @param objectSchemaName
    * @return
    * @throws DBException
    */
   public DB2Schema schemaLookup(DBRProgressMonitor monitor, DB2Schema parentSchema, String objectSchemaName) throws DBException {
      LOG.trace("schemaLookup");

      // Quick bypass: If it's the same name (99% of the time), return the
      // parentSchema
      if (parentSchema.getName().equals(objectSchemaName)) {
         return parentSchema;
      }

      // Lookup fo the schema that correspond to the name
      DB2Schema objectSchema = getSchema(monitor, objectSchemaName);
      if (objectSchema == null) {
         String msg = "Schema  '" + objectSchemaName + "' not found in database ??? Impossible";
         LOG.error(msg);
         throw new DBException(msg);

      }
      LOG.debug("objectSchemaName : " + objectSchemaName + " was different from parentSchema : " + parentSchema.getName());

      return objectSchema;
   }

   // --------------
   // Plan Tables
   // --------------

   // TODO DF: yet to be done

   @Override
   public DBCPlan planQueryExecution(DBCExecutionContext context, String query) throws DBCException {
      DB2PlanAnalyser plan = new DB2PlanAnalyser(this, query);
      plan.explain((JDBCExecutionContext) context);
      return plan;
   }

   public String getPlanTableSchemaName(JDBCExecutionContext context) throws DBCException {
      if (planTableSchemaName == null) {

         // TODO DF: not sure of activeSchema Explain tables could be created in any schema or at default, in SYSTOOLS
         // Should be current user in fact..
         planTableSchemaName = DB2Utils.checkExplainTables(context.getProgressMonitor(), this, activeSchemaName);

         if (planTableSchemaName == null) {
            // Plan table not found - try to create new one
            // TODO DF: ask the user in what schema to create the tables
            if (!UIUtils.confirmAction(DBeaverUI.getActiveWorkbenchShell(), PLAN_TABLE_TIT, PLAN_TABLE_MSG)) {
               return null;
            }
            planTableSchemaName = "SYSTOOLS";
            DB2Utils.createExplainTables(context.getProgressMonitor(), this, planTableSchemaName);
         }
      }
      return planTableSchemaName;
   }

   // --------------
   // Associations
   // --------------

   @Association
   public Collection<DB2Schema> getSchemas(DBRProgressMonitor monitor) throws DBException {
      return schemaCache.getObjects(monitor, this);
   }

   public DB2Schema getSchema(DBRProgressMonitor monitor, String name) throws DBException {
      return schemaCache.getObject(monitor, this, name);
   }

   @Association
   public Collection<DB2DataType> getDataTypes(DBRProgressMonitor monitor) throws DBException {
      return dataTypeCache.getObjects(monitor, this);
   }

   public DB2DataType getDataType(DBRProgressMonitor monitor, String name) throws DBException {
      return dataTypeCache.getObject(monitor, this, name);
   }

   @Association
   public Collection<DB2Tablespace> getTablespaces(DBRProgressMonitor monitor) throws DBException {
      return tablespaceCache.getObjects(monitor, this);
   }

   public DB2Tablespace getTablespace(DBRProgressMonitor monitor, String name) throws DBException {
      return tablespaceCache.getObject(monitor, this, name);
   }

   @Association
   public Collection<DB2Bufferpool> getBufferpools(DBRProgressMonitor monitor) throws DBException {
      return bufferpoolCache.getObjects(monitor, this);
   }

   public DB2Bufferpool getBufferpool(DBRProgressMonitor monitor, String name) throws DBException {
      return bufferpoolCache.getObject(monitor, this, name);
   }

   @Association
   public Collection<DB2User> getUsers(DBRProgressMonitor monitor) throws DBException {
      return userCache.getObjects(monitor, this);
   }

   public DB2User getUser(DBRProgressMonitor monitor, String name) throws DBException {
      return userCache.getObject(monitor, this, name);
   }

   @Association
   public Collection<DB2Role> getRoles(DBRProgressMonitor monitor) throws DBException {
      return roleCache.getObjects(monitor, this);
   }

   public DB2Role getRole(DBRProgressMonitor monitor, String name) throws DBException {
      return roleCache.getObject(monitor, this, name);
   }

   // -------------------------
   // Standards Getters
   // -------------------------

   public DB2SchemaCache getSchemaCache() {
      return schemaCache;
   }

   public DB2BufferpoolCache getBufferpoolCache() {
      return bufferpoolCache;
   }

   public DB2DataTypeCache getDataTypeCache() {
      return dataTypeCache;
   }

   public DB2TablespaceCache getTablespaceCache() {
      return tablespaceCache;
   }

   public DB2UserCache getUserCache() {
      return userCache;
   }

   public DB2GroupCache getGroupCache() {
      return groupCache;
   }

   public DB2RoleCache getRoleCache() {
      return roleCache;
   }

}

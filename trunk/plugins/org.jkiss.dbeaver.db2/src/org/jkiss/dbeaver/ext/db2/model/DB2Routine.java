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

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Collection;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.IDatabasePersistAction;
import org.jkiss.dbeaver.ext.db2.DB2Constants;
import org.jkiss.dbeaver.ext.db2.model.cache.DB2RoutineParmsCache;
import org.jkiss.dbeaver.ext.db2.model.dict.DB2OwnerType;
import org.jkiss.dbeaver.ext.db2.model.dict.DB2RoutineLanguage;
import org.jkiss.dbeaver.ext.db2.model.dict.DB2RoutineOrigin;
import org.jkiss.dbeaver.ext.db2.model.source.DB2SourceObject;
import org.jkiss.dbeaver.ext.db2.model.source.DB2SourceType;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.dbeaver.model.struct.DBSObjectState;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedure;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureType;
import org.jkiss.utils.CommonUtils;

/**
 * DB2 Routine Base Object (Procedures, Function)
 * 
 * @author Denis Forveille
 * 
 */
public class DB2Routine extends DB2SchemaObject implements DBSProcedure, DB2SourceObject {

   private final DB2RoutineParmsCache parmsCache = new DB2RoutineParmsCache();

   private String                     moduleName;
   private String                     routineName;
   private Integer                    routineId;
   private Integer                    routineModuleId;
   private DB2RoutineOrigin           origin;
   private DB2RoutineLanguage         language;
   private String                     dialect;
   private String                     owner;
   private DB2OwnerType               ownerType;
   private Timestamp                  createTime;
   private Timestamp                  alterTime;
   private String                     text;
   private String                     remarks;

   // TODO DF: add other attributes

   // -----------------------
   // Constructors
   // -----------------------

   public DB2Routine(DB2Schema schema, ResultSet dbResult) {
      super(schema, JDBCUtils.safeGetString(dbResult, "SPECIFICNAME"), true);

      this.moduleName = JDBCUtils.safeGetString(dbResult, "ROUTINEMODULENAME");
      this.routineName = JDBCUtils.safeGetString(dbResult, "ROUTINENAME");
      this.routineId = JDBCUtils.safeGetInteger(dbResult, "ROUTINEID");
      this.routineModuleId = JDBCUtils.safeGetInteger(dbResult, "ROUTINEMODULEID");
      this.origin = CommonUtils.valueOf(DB2RoutineOrigin.class, JDBCUtils.safeGetString(dbResult, "ORIGIN"));
      this.language = CommonUtils.valueOf(DB2RoutineLanguage.class, JDBCUtils.safeGetStringTrimmed(dbResult, "LANGUAGE"));
      this.dialect = JDBCUtils.safeGetString(dbResult, "DIALECT");
      this.owner = JDBCUtils.safeGetString(dbResult, "OWNER");
      this.ownerType = CommonUtils.valueOf(DB2OwnerType.class, JDBCUtils.safeGetString(dbResult, "OWNERTYPE"));
      this.createTime = JDBCUtils.safeGetTimestamp(dbResult, "CREATE_TIME");
      this.alterTime = JDBCUtils.safeGetTimestamp(dbResult, "ALTER_TIME");
      this.text = JDBCUtils.safeGetString(dbResult, "TEXT");
      this.remarks = JDBCUtils.safeGetString(dbResult, "REMARKS");

   }

   @Override
   public DBSObjectContainer getContainer() {
      return getParentObject();
   }

   @Override
   public DBSObjectState getObjectState() {
      return DBSObjectState.UNKNOWN;
   }

   @Override
   public void refreshObjectState(DBRProgressMonitor monitor) throws DBCException {
      parmsCache.clearCache();
   }

   @Override
   public IDatabasePersistAction[] getCompileActions() {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public DBSProcedureType getProcedureType() {
      return DBSProcedureType.PROCEDURE;
   }

   // -----------------
   // Children
   // -----------------
   @Override
   public Collection<DB2RoutineParm> getParameters(DBRProgressMonitor monitor) throws DBException {
      return parmsCache.getObjects(monitor, this);
   }

   // -----------------
   // Source
   // -----------------

   @Override
   public DB2SourceType getSourceType() {
      return DB2SourceType.PROCEDURE;
   }

   @Override
   public String getSourceDeclaration(DBRProgressMonitor monitor) throws DBException {
      if ((language != null) && (language.equals(DB2RoutineLanguage.SQL))) {
         return text;
      } else {
         return "";
      }
   }

   @Override
   public void setSourceDeclaration(String source) {
      // TODO Auto-generated method stub

   }

   // -----------------------
   // Properties
   // -----------------------

   @Override
   @Property(viewable = true, editable = false, order = 1)
   public String getName() {
      return super.getName();
   }

   @Property(viewable = true, editable = false, order = 2)
   public DB2Schema getSchema() {
      return parent;
   }

   @Property(viewable = true, editable = false, order = 3)
   public String getRoutineName() {
      return routineName;
   }

   @Property(viewable = true, editable = false, order = 4)
   public String getModuleName() {
      return moduleName;
   }

   @Property(viewable = true, editable = false, order = 5)
   public DB2RoutineLanguage getLanguage() {
      return language;
   }

   @Property(viewable = true, editable = false, order = 6)
   public Integer getRoutineId() {
      return routineId;
   }

   @Property(viewable = false, editable = false)
   public String getDialect() {
      return dialect;
   }

   @Property(viewable = false, editable = false, category = DB2Constants.CAT_DATETIME)
   public Timestamp getCreateTime() {
      return createTime;
   }

   @Property(viewable = false, editable = false, category = DB2Constants.CAT_DATETIME)
   public Timestamp getAlterTime() {
      return alterTime;
   }

   @Property(viewable = false, editable = false, category = DB2Constants.CAT_OWNER)
   public String getOwner() {
      return owner;
   }

   @Property(viewable = false, editable = false, category = DB2Constants.CAT_OWNER)
   public String getOwnerTypeDescription() {
      return ownerType.getDescription();
   }

   @Property(viewable = false, editable = false)
   public Integer getRoutineModuleId() {
      return routineModuleId;
   }

   @Property(viewable = false, editable = false)
   public String getOriginDesciption() {
      return origin.getDescription();
   }

   @Override
   @Property(viewable = false, editable = false)
   public String getDescription() {
      return remarks;
   }

}

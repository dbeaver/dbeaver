/*
 * Copyright (C) 2010-2013 Serge Rieder
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
package org.jkiss.dbeaver.ext.db2.model;

import java.sql.ResultSet;

import org.jkiss.dbeaver.ext.db2.model.dict.DB2TriggerDepType;
import org.jkiss.dbeaver.model.access.DBAPrivilege;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.utils.CommonUtils;

/**
 * DB2 Trigger Dependency
 * 
 * @author Denis Forveille
 * 
 */
public class DB2TriggerDep extends DB2Object<DB2Trigger> implements DBAPrivilege {

   private DB2TriggerDepType triggerDepType;
   private String            depSchema;
   private String            depModuleId;
   private String            tabAuth;

   // -----------------------
   // Constructors
   // -----------------------
   public DB2TriggerDep(DB2Trigger db2Trigger, ResultSet resultSet) {
      // TODO DF: Bad should be BSCHEMA+BNAME
      super(db2Trigger, JDBCUtils.safeGetString(resultSet, "BNAME"), true);

      // TODO DF: translaste BTYPE+BSCHEMA+BNAME into a real navigable DBSObject

      this.triggerDepType = CommonUtils.valueOf(DB2TriggerDepType.class, JDBCUtils.safeGetString(resultSet, "BTYPE"));
      this.depSchema = JDBCUtils.safeGetStringTrimmed(resultSet, "BSCHEMA");
      this.depModuleId = JDBCUtils.safeGetString(resultSet, "BMODULEID");
      this.tabAuth = JDBCUtils.safeGetString(resultSet, "TABAUTH");
   }

   // -----------------
   // Properties
   // -----------------

   @Override
   @Property(viewable = true, editable = false, id = "Name", order = 1)
   public String getName() {
      return super.getName();
   }

   @Property(viewable = true, editable = false, order = 2)
   public String getDepSchema() {
      return depSchema;
   }

   public DB2TriggerDepType getTriggerDepType() {
      return triggerDepType;
   }

   @Property(viewable = true, editable = false, order = 3)
   public String getTriggerDepTypeDescription() {
      return triggerDepType.getDescription();
   }

   @Property(viewable = true, editable = false)
   public String getDepModuleId() {
      return depModuleId;
   }

   @Property(viewable = true, editable = false)
   public String getTabAuth() {
      return tabAuth;
   }

}

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
import java.util.Collection;
import java.util.Collections;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.db2.DB2Constants;
import org.jkiss.dbeaver.ext.db2.model.dict.DB2OwnerType;
import org.jkiss.dbeaver.ext.db2.model.source.DB2StatefulObject;
import org.jkiss.dbeaver.model.DBPNamedObject2;
import org.jkiss.dbeaver.model.DBPRefreshableObject;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCTable;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.CommonUtils;

/**
 * Super class for DB2 Tables and Views
 * 
 * @author Denis Forveille
 * 
 */
public abstract class DB2TableBase extends JDBCTable<DB2DataSource, DB2Schema> implements
                                                                              DBPNamedObject2,
                                                                              DBPRefreshableObject,
                                                                              DB2StatefulObject {

   private static final Log log = LogFactory.getLog(DB2TableBase.class);

   private String           owner;
   private DB2OwnerType     ownerType;
   private String           remarks;

   // -----------------
   // Constructors
   // -----------------
   public DB2TableBase(DBRProgressMonitor monitor, DB2Schema schema, ResultSet dbResult) {
      super(schema, true);

      this.owner = JDBCUtils.safeGetString(dbResult, "OWNER");
      this.ownerType = CommonUtils.valueOf(DB2OwnerType.class, JDBCUtils.safeGetString(dbResult, "OWNERTYPE"));
      this.remarks = JDBCUtils.safeGetString(dbResult, "REMARKS");
   }

   @Override
   public void refreshObjectState(DBRProgressMonitor monitor) throws DBCException {
      // TODO Auto-generated method stub

   }

   @Override
   public DB2Schema getSchema() {
      return super.getContainer();
   }

   @Override
   @Property(viewable = false, editable = false)
   public String getDescription() {
      return remarks;
   }

   @Override
   public String getFullQualifiedName() {
      return getContainer().getName() + "." + this.getName();
   }

   // -----------------
   // Associations (Imposed from DBSTable). In DB2, Most of objects "derived"
   // from Tables don't have those..
   // -----------------

   @Override
   public Collection<DB2Index> getIndexes(DBRProgressMonitor monitor) throws DBException {
      return Collections.emptyList();
   }

   @Override
   public Collection<DB2TableUniqueKey> getConstraints(DBRProgressMonitor monitor) throws DBException {
      return Collections.emptyList();
   }

   @Override
   public Collection<DB2TableForeignKey> getAssociations(DBRProgressMonitor monitor) throws DBException {
      return Collections.emptyList();
   }

   @Override
   public Collection<DB2TableReference> getReferences(DBRProgressMonitor monitor) throws DBException {
      return Collections.emptyList();
   }

   // -----------------
   // Properties
   // -----------------

   @Override
   @Property(viewable = true, editable = false, valueTransformer = DBObjectNameCaseTransformer.class, order = 1)
   public String getName() {
      return super.getName();
   }

   @Property(viewable = false, editable = false, category = DB2Constants.CAT_OWNER)
   public String getOwner() {
      return owner;
   }

   @Property(viewable = false, editable = false, category = DB2Constants.CAT_OWNER)
   public String getOwnerTypeDescription() {
      return ownerType.getDescription();
   }

   public DB2OwnerType getOwnerType() {
      return ownerType;
   }

}

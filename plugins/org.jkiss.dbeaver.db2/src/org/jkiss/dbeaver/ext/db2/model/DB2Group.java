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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.dbeaver.model.access.DBAUser;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.struct.DBSObjectLazy;

/**
 * DB2 Group
 * 
 * @author Denis Forveille
 * 
 * 
 */
public class DB2Group extends DB2GlobalObject implements DBAUser, DBSObjectLazy<DB2DataSource> {
   static final Log log = LogFactory.getLog(DB2Group.class);

   // TODO DF: yet to be done

   private long     id;
   private String   name;

   // -----------------------
   // Constructors
   // -----------------------

   public DB2Group(DB2DataSource dataSource) {
      super(dataSource, true);
      // TODO DF: Used for what?
   }

   public DB2Group(DB2DataSource dataSource, ResultSet resultSet) {
      super(dataSource, true);
      this.id = JDBCUtils.safeGetLong(resultSet, "USER_ID");
      this.name = JDBCUtils.safeGetString(resultSet, "USERNAME");
   }

   @Override
   public Object getLazyReference(Object propertyId) {
      // TODO Auto-generated method stub
      return null;
   }

   // -----------------
   // Properties
   // -----------------
   @Property(order = 1)
   public long getId() {
      return id;
   }

   @Override
   @Property(viewable = true, order = 2)
   public String getName() {
      return name;
   }

}

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
package org.jkiss.dbeaver.ext.db2.info;

import java.sql.ResultSet;

import org.jkiss.dbeaver.ext.db2.model.DB2DataSource;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.struct.DBSObject;

/**
 * DB2 Application
 * 
 * @author Denis Forveille
 * 
 */
public class DB2Application implements DBSObject {

   private DB2DataSource dataSource;

   private String        databaseName;
   private Long          agentId;
   private String        authorisationId;

   private String        applicationName;
   private String        applicationId;
   private String        applicationStatus;
   private String        statusChangeTime;
   private String        sequenceNo;

   private String        clientDatabaseAlias;
   private String        clientProductId;
   private Long          clientPId;
   private String        clientPlatform;
   private String        clientProtocol;
   private String        clientNName;

   private Integer       coordNodeNum;
   private Long          coordAgentPid;
   private Long          numAssociatedAgents;
   private String        tpmonClientUserid;
   private String        tpmonClientWorkstationNane;
   private String        tpmonClientApplicationName;
   private String        tpmonAccountingString;

   // -----------------------
   // Constructors
   // -----------------------
   public DB2Application(DB2DataSource dataSource, ResultSet dbResult) {
      this.dataSource = dataSource;

      this.clientDatabaseAlias = JDBCUtils.safeGetString(dbResult, "CLIENT_DB_ALIAS");
      this.databaseName = JDBCUtils.safeGetString(dbResult, "DB_NAME");
      this.agentId = JDBCUtils.safeGetLong(dbResult, "AGENT_ID");
      this.authorisationId = JDBCUtils.safeGetString(dbResult, "AUTHID");
      this.applicationName = JDBCUtils.safeGetString(dbResult, "APPL_NAME");
      this.applicationId = JDBCUtils.safeGetString(dbResult, "APPL_ID");
      this.applicationStatus = JDBCUtils.safeGetString(dbResult, "APPL_STATUS");
      this.statusChangeTime = JDBCUtils.safeGetString(dbResult, "STATUS_CHANGE_TIME");
      this.sequenceNo = JDBCUtils.safeGetString(dbResult, "SEQUENCE_NO");
      this.clientProductId = JDBCUtils.safeGetString(dbResult, "CLIENT_PRDID");
      this.clientPId = JDBCUtils.safeGetLong(dbResult, "CLIENT_PID");
      this.clientPlatform = JDBCUtils.safeGetString(dbResult, "CLIENT_PLATFORM");
      this.clientProtocol = JDBCUtils.safeGetString(dbResult, "CLIENT_PROTOCOL");
      this.clientNName = JDBCUtils.safeGetString(dbResult, "CLIENT_NNAME");
      this.coordNodeNum = JDBCUtils.safeGetInteger(dbResult, "COORD_NODE_NUM");
      this.numAssociatedAgents = JDBCUtils.safeGetLong(dbResult, "COORD_AGENT_PID");
      this.numAssociatedAgents = JDBCUtils.safeGetLong(dbResult, "NUM_ASSOC_AGENTS");
      this.tpmonClientUserid = JDBCUtils.safeGetString(dbResult, "TPMON_CLIENT_USERID");
      this.tpmonClientWorkstationNane = JDBCUtils.safeGetString(dbResult, "TPMON_CLIENT_WKSTN");
      this.tpmonClientApplicationName = JDBCUtils.safeGetString(dbResult, "TPMON_CLIENT_APP");
      this.tpmonAccountingString = JDBCUtils.safeGetString(dbResult, "TPMON_ACC_STR");
   }

   @Override
   public DBPDataSource getDataSource() {
      return dataSource;
   }

   @Override
   public DBSObject getParentObject() {
      return dataSource.getContainer();
   }

   @Override
   public boolean isPersisted() {
      return false;
   }

   @Override
   public String getDescription() {
      return null;
   }

   // -----------------
   // Properties
   // -----------------
   @Override
   @Property(viewable = true, editable = false, order = 1)
   public String getName() {
      return applicationId;
   }

   @Property(viewable = true, editable = false, order = 2)
   public String getDatabaseName() {
      return databaseName;
   }

   @Property(viewable = true, editable = false, order = 3)
   public String getApplicationName() {
      return applicationName;
   }

   @Property(viewable = true, editable = false, order = 4)
   public String getApplicationStatus() {
      return applicationStatus;
   }

   @Property(viewable = true, editable = false, order = 5)
   public Long getAgentId() {
      return agentId;
   }

   @Property(viewable = true, editable = false, order = 6)
   public String getAuthorisationId() {
      return authorisationId;
   }

   @Property(viewable = true, editable = false, order = 7)
   public String getClientNName() {
      return clientNName;
   }

   @Property(viewable = true, editable = false, order = 8)
   public String getClientDatabaseAlias() {
      return clientDatabaseAlias;
   }

   @Property(viewable = true, editable = false, order = 9)
   public Long getClientPId() {
      return clientPId;
   }

   @Property(viewable = true, editable = false, order = 10)
   public String getClientProductId() {
      return clientProductId;
   }

   @Property(viewable = true, editable = false, order = 11)
   public String getClientPlatform() {
      return clientPlatform;
   }

   @Property(viewable = true, editable = false, order = 12)
   public String getTpmonAccountingString() {
      return tpmonAccountingString;
   }

   @Property(viewable = false)
   public String getClientProtocol() {
      return clientProtocol;
   }

   @Property(viewable = false)
   public String getStatusChangeTime() {
      return statusChangeTime;
   }

   @Property(viewable = false)
   public String getSequenceNo() {
      return sequenceNo;
   }

   @Property(viewable = false)
   public Integer getCoordNodeNum() {
      return coordNodeNum;
   }

   @Property(viewable = false)
   public Long getCoordAgentPid() {
      return coordAgentPid;
   }

   @Property(viewable = false)
   public Long getNumAssociatedAgents() {
      return numAssociatedAgents;
   }

   @Property(viewable = false)
   public String getTpmonClientUserid() {
      return tpmonClientUserid;
   }

   @Property(viewable = false)
   public String getTpmonClientWorkstationNane() {
      return tpmonClientWorkstationNane;
   }

   @Property(viewable = false)
   public String getTpmonClientApplicationName() {
      return tpmonClientApplicationName;
   }

}

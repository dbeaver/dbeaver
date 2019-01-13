/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2013-2015 Denis Forveille (titou10.titou10@gmail.com)
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ext.db2.model.app;

import org.jkiss.dbeaver.ext.db2.DB2Constants;
import org.jkiss.dbeaver.model.admin.sessions.DBAServerSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;

import java.sql.ResultSet;

/**
 * DB2 Application
 * 
 * @author Denis Forveille
 */
public class DB2ServerApplication implements DBAServerSession {

    private String databaseName;
    private Long agentId;
    private String authorisationId;

    private String applicationName;
    private String applicationId;
    private String applicationStatus;
    private String statusChangeTime;
    private String sequenceNo;

    private String clientDatabaseAlias;
    private String clientProductId;
    private Long clientPId;
    private String clientPlatform;
    private String clientProtocol;
    private String clientNName;

    private Integer coordNodeNum;
    private Long coordAgentPid;
    private Long numAssociatedAgents;
    private String tpmonClientUserid;
    private String tpmonClientWorkstationNane;
    private String tpmonClientApplicationName;
    private String tpmonAccountingString;

    // -----------------------
    // Constructors
    // -----------------------
    public DB2ServerApplication(ResultSet dbResult)
    {

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
    public String getActiveQuery()
    {
        // DF: no "Active Query" easily available in DB2
        // ..and most applications are not currently executing an SQL...
        // It needs to activate some monitoring flags that are usually off..
        return null;
    }

    @Override
    public String toString()
    {
        return agentId.toString();
    }

    // -----------------
    // Properties
    // -----------------
    @Property(viewable = true, editable = false, order = 1)
    public String getApplicationId()
    {
        return applicationId;
    }

    @Property(viewable = true, editable = false, order = 2)
    public Long getAgentId()
    {
        return agentId;
    }

    @Property(viewable = true, editable = false, order = 3)
    public String getDatabaseName()
    {
        return databaseName;
    }

    @Property(viewable = true, editable = false, order = 4)
    public String getApplicationName()
    {
        return applicationName;
    }

    @Property(viewable = true, editable = false, order = 5)
    public String getApplicationStatus()
    {
        return applicationStatus;
    }

    @Property(viewable = true, editable = false, order = 6)
    public String getAuthorisationId()
    {
        return authorisationId;
    }

    @Property(viewable = true, editable = false, order = 7, category = DB2Constants.CAT_CLIENT)
    public String getClientNName()
    {
        return clientNName;
    }

    @Property(viewable = true, editable = false, order = 8, category = DB2Constants.CAT_CLIENT)
    public String getClientDatabaseAlias()
    {
        return clientDatabaseAlias;
    }

    @Property(viewable = true, editable = false, order = 9, category = DB2Constants.CAT_CLIENT)
    public Long getClientPId()
    {
        return clientPId;
    }

    @Property(viewable = true, editable = false, order = 10, category = DB2Constants.CAT_CLIENT)
    public String getClientProductId()
    {
        return clientProductId;
    }

    @Property(viewable = true, editable = false, order = 11, category = DB2Constants.CAT_CLIENT)
    public String getClientPlatform()
    {
        return clientPlatform;
    }

    @Property(viewable = false)
    public String getTpmonAccountingString()
    {
        return tpmonAccountingString;
    }

    @Property(viewable = false, category = DB2Constants.CAT_CLIENT)
    public String getClientProtocol()
    {
        return clientProtocol;
    }

    @Property(viewable = false)
    public String getStatusChangeTime()
    {
        return statusChangeTime;
    }

    @Property(viewable = false)
    public String getSequenceNo()
    {
        return sequenceNo;
    }

    @Property(viewable = false)
    public Integer getCoordNodeNum()
    {
        return coordNodeNum;
    }

    @Property(viewable = false)
    public Long getCoordAgentPid()
    {
        return coordAgentPid;
    }

    @Property(viewable = false)
    public Long getNumAssociatedAgents()
    {
        return numAssociatedAgents;
    }

    @Property(viewable = false, category = DB2Constants.CAT_CLIENT)
    public String getTpmonClientUserid()
    {
        return tpmonClientUserid;
    }

    @Property(viewable = false, category = DB2Constants.CAT_CLIENT)
    public String getTpmonClientWorkstationNane()
    {
        return tpmonClientWorkstationNane;
    }

    @Property(viewable = true, category = DB2Constants.CAT_CLIENT)
    public String getTpmonClientApplicationName()
    {
        return tpmonClientApplicationName;
    }

}

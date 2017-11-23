/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ext.vertica.model;

import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.util.Date;

/**
 * VerticaNode
 */
public class VerticaNode implements DBSObject
{

    final private VerticaDataSource dataSource;
    private String name;
    private String nodeState;
    private String nodeAddress;
    private String nodeAddressFamily;
    private String exportAddress;
    private String exportAddressFamily;
    private String catalogPath;
    private String nodeType;
    private boolean isEphemeral;
    private String standingInFor;
    private Date lastMsgFromNodeAt;
    private Date nodeDownSince;

    protected VerticaNode(VerticaDataSource dataSource, JDBCResultSet dbResult) {
        this.dataSource = dataSource;
        this.name = JDBCUtils.safeGetString(dbResult, "node_name");
        this.nodeState = JDBCUtils.safeGetString(dbResult, "node_state");
        this.nodeAddress = JDBCUtils.safeGetString(dbResult, "node_address");
        this.nodeAddressFamily = JDBCUtils.safeGetString(dbResult, "node_address_family");
        this.exportAddress = JDBCUtils.safeGetString(dbResult, "export_address");
        this.exportAddressFamily = JDBCUtils.safeGetString(dbResult, "export_address_family");
        this.catalogPath = JDBCUtils.safeGetString(dbResult, "catalog_path");
        this.nodeType = JDBCUtils.safeGetString(dbResult, "node_type");
        this.isEphemeral = JDBCUtils.safeGetBoolean(dbResult, "is_ephemeral");
        this.standingInFor = JDBCUtils.safeGetString(dbResult, "standing_in_for");
        this.lastMsgFromNodeAt = JDBCUtils.safeGetTimestamp(dbResult, "last_msg_from_node_at");
        this.nodeDownSince = JDBCUtils.safeGetTimestamp(dbResult, "node_down_since");
    }

    @Override
    public String getDescription() {
        return null;
    }

    @Override
    public DBSObject getParentObject() {
        return dataSource;
    }

    @Override
    public boolean isPersisted() {
        return true;
    }

    @Override
    public DBPDataSource getDataSource() {
        return dataSource;
    }

    @Property(viewable = true, order = 1)
    @Override
    public String getName() {
        return name;
    }

    @Property(viewable = true, order = 4)
    public String getNodeState() {
        return nodeState;
    }

    @Property(viewable = true, order = 2)
    public String getNodeAddress() {
        return nodeAddress;
    }

    @Property(order = 5)
    public String getNodeAddressFamily() {
        return nodeAddressFamily;
    }

    @Property(viewable = true, order = 3)
    public String getExportAddress() {
        return exportAddress;
    }

    @Property(order = 6)
    public String getExportAddressFamily() {
        return exportAddressFamily;
    }

    @Property(order = 7)
    public String getCatalogPath() {
        return catalogPath;
    }

    @Property(order = 8)
    public String getNodeType() {
        return nodeType;
    }

    @Property(order = 9)
    public boolean isEphemeral() {
        return isEphemeral;
    }

    @Property(order = 20)
    public String getStandingInFor() {
        return standingInFor;
    }

    @Property(order = 21)
    public Date getLastMsgFromNodeAt() {
        return lastMsgFromNodeAt;
    }

    @Property(order = 22)
    public Date getNodeDownSince() {
        return nodeDownSince;
    }
}

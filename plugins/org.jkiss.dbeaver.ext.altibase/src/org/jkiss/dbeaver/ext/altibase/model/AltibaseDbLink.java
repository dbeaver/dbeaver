/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.altibase.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.generic.model.GenericStructContainer;
import org.jkiss.dbeaver.model.DBPRefreshableObject;
import org.jkiss.dbeaver.model.DBPScriptObject;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.utils.CommonUtils;

import java.sql.Timestamp;
import java.util.Map;

public class AltibaseDbLink extends AltibaseObject<GenericStructContainer> implements DBPScriptObject, DBPRefreshableObject {

    private String ddl;

    private String userName; // null for public, user_id for private
    private int linkId;
    private int userMode; // 0:public, 1: private
    private String remoteUserId;
    private String targetName;
    private int linkType; // Heterogeneous Link or Homogeneous Link: Only Heterogeneous since 6.5.1
    private Timestamp created;
    private Timestamp lastDdlTime;

    protected AltibaseDbLink(GenericStructContainer parent, JDBCResultSet resultSet) {
        super(parent, 
                JDBCUtils.safeGetString(resultSet, "LINK_NAME"), 
                JDBCUtils.safeGetLong(resultSet, "LINK_OID"),
                true);

        userName = JDBCUtils.safeGetString(resultSet, "USER_NAME");
        linkId = JDBCUtils.safeGetInt(resultSet, "LINK_ID");
        userMode = JDBCUtils.safeGetInt(resultSet, "USER_MODE");
        remoteUserId = JDBCUtils.safeGetString(resultSet, "REMOTE_USER_ID");
        linkType = JDBCUtils.safeGetInt(resultSet, "LINK_TYPE");
        targetName = JDBCUtils.safeGetString(resultSet, "TARGET_NAME");
        created = JDBCUtils.safeGetTimestamp(resultSet, "CREATED");
        lastDdlTime = JDBCUtils.safeGetTimestamp(resultSet, "LAST_DDL_TIME");
    }

    @Property(viewable = true, order = 2)
    public int getLinkId() {
        return linkId;
    }

    @Property(viewable = true, order = 3)
    public long getLinkOid() {
        return getObjectId();
    }

    /* null in case of public DbLink */
    @Nullable
    @Property(viewable = true, order = 4)
    public String getUserName() {
        return userName;
    }

    @NotNull
    @Property(viewable = true, order = 5)
    public String getRemoteUserId() {
        return remoteUserId;
    }

    @NotNull
    @Property(viewable = true, order = 6)
    public String getTargetName() {
        return targetName;
    }

    @NotNull
    @Property(viewable = true, order = 7)
    public String getUserMode() {
        return isPublic() ? "Public" : "Private";
    }

    @NotNull
    @Property(viewable = true, order = 8)
    public String getLinkType() {
        return (linkType == 0) ? "Heterogeneous" : "Homogeneous";
    }

    @NotNull
    @Property(viewable = true, order = 10)
    public Timestamp getCreated() {
        return created;
    }

    @NotNull
    @Property(viewable = true, order = 11)
    public Timestamp getLastDdlTime() {
        return lastDdlTime;
    }

    private boolean isPublic() {
        return (userMode == 0);
    }

    @Override
    public String getObjectDefinitionText(DBRProgressMonitor monitor, Map<String, Object> options) throws DBException {
        if (CommonUtils.isEmpty(ddl)) {
            options.put("SCHEMA", isPublic() ? "PUBLIC" : this.getParentObject().getName());
            ddl = ((AltibaseMetaModel) getDataSource().getMetaModel()).getDbLinkDDL(monitor, this, options) + ";";
        }

        return ddl;
    }

    @Override
    public DBSObject refreshObject(DBRProgressMonitor monitor) throws DBException {
        DBSObject dbsObject = null;
        
        // Non-schema DbLink
        if (isPublic()) {
            AltibaseDataSource dataSouce = getDataSource();
            dbsObject = dataSouce.getDbLinkCache().refreshObject(monitor, dataSouce, this);
        // Schema DbLink
        } else {
            AltibaseSchema schema = (AltibaseSchema) getParentObject();
            dbsObject = schema.getDbLinkCache().refreshObject(monitor, schema, this);
        }
        
        return dbsObject;
    }
}
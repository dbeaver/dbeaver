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
package org.jkiss.dbeaver.ext.altibase.model.lock;

import org.jkiss.dbeaver.ext.altibase.model.AltibaseDataSource;
import org.jkiss.dbeaver.model.admin.locks.DBAServerLock;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class AltibaseLock implements DBAServerLock {

    private int    waitSid;
    private int    waitPid;
    private int    waitTxid;
    private String waitUser;
    private String waitQuery;
    private String objSchema;
    private String objName;
    private int    holdSid;
    private int    holdPid;
    private int    holdTxid;
    private String holdUser;
    private String holdQuery;

    private DBAServerLock hold = null;
    private List<DBAServerLock> waiters = new ArrayList<>(0);

    private AltibaseDataSource dataSource;

    public AltibaseLock(ResultSet dbResult, AltibaseDataSource dataSource) {
        this.waitSid = JDBCUtils.safeGetInt(dbResult, "w_sid");
        this.waitPid = JDBCUtils.safeGetInt(dbResult, "w_pid");
        this.waitTxid = JDBCUtils.safeGetInt(dbResult, "w_txid");
        this.waitUser = JDBCUtils.safeGetString(dbResult, "w_user_name");
        this.waitQuery = JDBCUtils.safeGetString(dbResult, "w_query");

        this.objName = JDBCUtils.safeGetString(dbResult, "w_obj_schema");
        this.objSchema = JDBCUtils.safeGetString(dbResult, "w_obj_name");

        this.holdSid  = JDBCUtils.safeGetInt(dbResult, "h_sid");
        this.holdPid = JDBCUtils.safeGetInt(dbResult, "h_pid");
        this.holdTxid = JDBCUtils.safeGetInt(dbResult, "h_txid");
        this.holdUser = JDBCUtils.safeGetString(dbResult, "h_user_name");
        this.holdQuery = JDBCUtils.safeGetString(dbResult, "h_query");

        this.dataSource = dataSource;
    }

    @Override
    public String getTitle() {
        return String.valueOf(waitSid);
    }

    @Override
    public DBAServerLock getHoldBy() {
        return hold;
    }

    public DBAServerLock getHold() {
        return hold;
    }

    @Override
    public Integer getId() {
        return waitSid;
    }

    @Override
    public List<DBAServerLock> waitThis() {
        return this.waiters;
    }

    @Override
    public Integer getHoldID() {
        return holdSid;
    }

    @Override
    public void setHoldBy(DBAServerLock lock) {
        this.hold = lock;
    }

    @Property(viewable = true, order = 1)
    public int getWait_sid() {
        return waitSid;
    }

    @Property(viewable = true, order = 2)
    public int getWait_pid() {
        return waitPid;
    }

    @Property(viewable = true, order = 3)
    public int getWait_txid() {
        return waitTxid;
    }

    @Property(viewable = true, order = 4)
    public String getWait_user() {
        return waitUser;
    }

    @Property(viewable = true, order = 5)
    public String getWait_query() {
        return waitQuery;
    }

    @Property(viewable = true, order = 6)
    public String getOwner() {
        return objSchema;
    }

    @Property(viewable = true, order = 7)
    public String getOname() {
        return objName;
    }


    @Property(viewable = true, order = 10)
    public int getHold_sid() {
        return holdSid;
    }

    @Property(viewable = true, order = 11)
    public int getHold_pid() {
        return holdPid;
    }

    @Property(viewable = true, order = 12)
    public int getHold_txid() {
        return holdTxid;
    }

    @Property(viewable = true, order = 13)
    public String getHold_user() {
        return holdUser;
    }

    @Property(viewable = true, order = 14)
    public String getHold_query() {
        return holdQuery;
    }

    public AltibaseDataSource getDataSource() {
        return dataSource;
    }
}
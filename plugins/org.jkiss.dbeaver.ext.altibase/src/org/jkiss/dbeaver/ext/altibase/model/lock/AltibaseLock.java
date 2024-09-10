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

    private int    wait_sid;
    private int    wait_pid;
    private int    wait_txid;
    private String wait_user;
    private String wait_query;
    private String obj_schema;
    private String obj_name;
    private int    hold_sid;
    private int    hold_pid;
    private int    hold_txid;
    private String hold_user;
    private String hold_query;

    private DBAServerLock hold = null;
    private List<DBAServerLock> waiters = new ArrayList<>(0);

    private AltibaseDataSource dataSource;

    public AltibaseLock(ResultSet dbResult, AltibaseDataSource dataSource) {
        this.wait_sid = JDBCUtils.safeGetInt(dbResult, "w_sid");
        this.wait_pid = JDBCUtils.safeGetInt(dbResult, "w_pid");
        this.wait_txid = JDBCUtils.safeGetInt(dbResult, "w_txid");
        this.wait_user = JDBCUtils.safeGetString(dbResult, "w_user_name");
        this.wait_query = JDBCUtils.safeGetString(dbResult, "w_query");

        this.obj_name = JDBCUtils.safeGetString(dbResult, "w_obj_schema");
        this.obj_schema = JDBCUtils.safeGetString(dbResult, "w_obj_name");

        this.hold_sid  = JDBCUtils.safeGetInt(dbResult, "h_sid");
        this.hold_pid = JDBCUtils.safeGetInt(dbResult, "h_pid");
        this.hold_txid = JDBCUtils.safeGetInt(dbResult, "h_txid");
        this.hold_user = JDBCUtils.safeGetString(dbResult, "h_user_name");
        this.hold_query = JDBCUtils.safeGetString(dbResult, "h_query");

        this.dataSource = dataSource;
    }

    @Override
    public String getTitle() {
        return String.valueOf(wait_sid);
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
        return wait_sid;
    }

    @Override
    public List<DBAServerLock> waitThis() {
        return this.waiters;
    }

    @Override
    public Integer getHoldID() {
        return hold_sid;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void setHoldBy(DBAServerLock lock) {
        this.hold = lock;
    }

    @Property(viewable = true, order = 1)
    public int getWait_sid() {
        return wait_sid;
    }

    @Property(viewable = true, order = 2)
    public int getWait_pid() {
        return wait_pid;
    }

    @Property(viewable = true, order = 3)
    public int getWait_txid() {
        return wait_txid;
    }

    @Property(viewable = true, order = 4)
    public String getWait_user() {
        return wait_user;
    }

    @Property(viewable = true, order = 5)
    public String getWait_query() {
        return wait_query;
    }

    @Property(viewable = true, order = 6)
    public String getOwner() {
        return obj_schema;
    }

    @Property(viewable = true, order = 7)
    public String getOname() {
        return obj_name;
    }


    @Property(viewable = true, order = 10)
    public int getHold_sid() {
        return hold_sid;
    }

    @Property(viewable = true, order = 11)
    public int getHold_pid() {
        return hold_pid;
    }

    @Property(viewable = true, order = 12)
    public int getHold_txid() {
        return hold_txid;
    }

    @Property(viewable = true, order = 13)
    public String getHold_user() {
        return hold_user;
    }

    @Property(viewable = true, order = 14)
    public String getHold_query() {
        return hold_query;
    }

    public AltibaseDataSource getDataSource() {
        return dataSource;
    }
}
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

import org.jkiss.dbeaver.model.admin.locks.DBAServerLockItem;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;

import java.sql.ResultSet;

public class AltibaseLockItem implements DBAServerLockItem {

    private String lockItemType;
    private int tbsId;
    private String tbsName;
    private long tableOid;
    private long dbfId;

    private String lockDesc;
    private int lockCnt;

    public AltibaseLockItem(ResultSet dbResult) {

        this.lockDesc = JDBCUtils.safeGetString(dbResult, "lock_desc");
        this.lockItemType = JDBCUtils.safeGetString(dbResult, "locked_obj_type");
        this.lockCnt = JDBCUtils.safeGetInt(dbResult, "lock_cnt");

        this.tbsId = JDBCUtils.safeGetInt(dbResult, "tbs_id");
        this.tbsName = JDBCUtils.safeGetString(dbResult, "tbs_name");
        this.tableOid = JDBCUtils.safeGetLong(dbResult, "table_oid");
        this.dbfId = JDBCUtils.safeGetLong(dbResult, "dbf_id");
    }

    @Property(viewable = true, order = 1)
    public String getLockType() {
        return lockDesc;
    }

    @Property(viewable = true, order = 2)
    public String getLockTargetObjType() {
        return lockItemType;
    }

    @Property(viewable = true, order = 3)
    public int getLockCnt() {
        return lockCnt;
    }

    @Property(viewable = true, order = 4)
    public int getTbsId() {
        return tbsId;
    }

    @Property(viewable = true, order = 5)
    public String getTbsName() {
        return tbsName;
    }

    @Property(viewable = true, order = 6)
    public long getTableOid() {
        return tableOid;
    }

    @Property(viewable = true, order = 7)
    public long getDbfOid() {
        return dbfId;
    }
}
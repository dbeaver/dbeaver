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
package org.jkiss.dbeaver.ext.db2.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.db2.DB2Constants;
import org.jkiss.dbeaver.ext.db2.model.dict.DB2YesNo;
import org.jkiss.dbeaver.model.DBPStatefulObject;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.meta.PropertyLength;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObjectState;

import java.util.Date;

public class DB2ColumnMask extends DB2Object<DB2Table> implements DBPStatefulObject {

    private DB2TableColumn tableColumn;
    private Date creationTime;
    private Date alteringTime;
    private boolean enable;
    private boolean valid;
    private String ruleText;
    private String description;

    DB2ColumnMask(@NotNull DB2Table db2Table, @NotNull DB2TableColumn column, @NotNull String name, @NotNull JDBCResultSet resultSet) {
        super(db2Table, name, true);
        this.tableColumn = column;
        this.creationTime = JDBCUtils.safeGetTimestamp(resultSet, DB2Constants.SYSCOLUMN_CREATE_TIME);
        this.alteringTime = JDBCUtils.safeGetTimestamp(resultSet, DB2Constants.SYSCOLUMN_ALTER_TIME);
        this.enable = JDBCUtils.safeGetBoolean(resultSet, "ENABLE", DB2YesNo.Y.name());
        this.valid = JDBCUtils.safeGetBoolean(resultSet, DB2Constants.SYSCOLUMN_VALID, DB2YesNo.Y.name());
        this.ruleText = JDBCUtils.safeGetString(resultSet, "RULETEXT");
        this.description = JDBCUtils.safeGetString(resultSet,DB2Constants.SYSCOLUMN_REMARKS);
    }

    @Property(viewable = true, order = 2)
    public DB2TableColumn getTableColumn() {
        return tableColumn;
    }

    @Property(viewable = true, order = 3)
    public Date getCreationTime() {
        return creationTime;
    }

    @Property(viewable = true, order = 4)
    public Date getAlteringTime() {
        return alteringTime;
    }

    @Property(viewable = true, order = 5)
    public boolean isEnable() {
        return enable;
    }

    @Property(viewable = true, order = 6)
    public boolean isValid() {
        return valid;
    }

    @Property(viewable = true, length = PropertyLength.MULTILINE, order = 7)
    public String getRuleText() {
        return ruleText;
    }

    @Nullable
    @Override
    @Property(viewable = true, length = PropertyLength.MULTILINE, order = 100)
    public String getDescription() {
        return description;
    }

    @NotNull
    @Override
    public DBSObjectState getObjectState() {
        return valid ? DBSObjectState.NORMAL : DBSObjectState.INVALID;
    }

    @Override
    public void refreshObjectState(@NotNull DBRProgressMonitor monitor) {
        // do nothing
    }
}

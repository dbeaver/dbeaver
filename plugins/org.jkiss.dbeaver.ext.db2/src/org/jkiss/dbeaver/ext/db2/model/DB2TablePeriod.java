/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2013-2016 Denis Forveille (titou10.titou10@gmail.com)
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.db2.model.dict.DB2PeriodType;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.utils.CommonUtils;

import java.sql.ResultSet;

/**
 * DB2 Table Period
 * 
 * @author Denis Forveille
 */
public class DB2TablePeriod extends DB2Object<DB2Table> {

    private DB2TableColumn beginColumn;
    private DB2TableColumn endColumn;
    private DB2PeriodType type;
    private DB2Schema historyTableSchema;
    private DB2Table historyTable;

    // -----------------------
    // Constructors
    // -----------------------

    public DB2TablePeriod(DB2Table db2Table, ResultSet dbResult) throws DBException
    {
        super(db2Table, JDBCUtils.safeGetString(dbResult, "PERIODNAME"), true);

        DB2DataSource db2DataSource = db2Table.getDataSource();

        String beginColumnName = JDBCUtils.safeGetString(dbResult, "BEGINCOLNAME");
        String endColumnName = JDBCUtils.safeGetString(dbResult, "ENDCOLNAME");
        String historyTabSchemaName = JDBCUtils.safeGetString(dbResult, "HISTORYTABSCHEMA");
        String historyTabName = JDBCUtils.safeGetString(dbResult, "HISTORYTABNAME");

        this.type = CommonUtils.valueOf(DB2PeriodType.class, JDBCUtils.safeGetString(dbResult, "PERIODTYPE"));

        // Lookup related objects
        VoidProgressMonitor vpm = new VoidProgressMonitor();
        beginColumn = db2Table.getAttribute(vpm, beginColumnName);
        endColumn = db2Table.getAttribute(vpm, endColumnName);
        historyTableSchema = db2DataSource.getSchema(vpm, historyTabSchemaName.trim());
        historyTable = historyTableSchema.getTable(vpm, historyTabName);
    }

    // -----------------
    // Properties
    // -----------------

    @NotNull
    @Override
    @Property(viewable = true, editable = false, order = 1)
    public String getName()
    {
        return super.getName();
    }

    @Property(viewable = true, order = 2)
    public DB2Table getTable()
    {
        return parent;
    }

    @Property(viewable = true, order = 3)
    public DB2PeriodType getType()
    {
        return type;
    }

    @Property(viewable = true, order = 10)
    public DB2TableColumn getBeginColumn()
    {
        return beginColumn;
    }

    @Property(viewable = true, order = 11)
    public DB2TableColumn getEndColumn()
    {
        return endColumn;
    }

    @Property(viewable = true, order = 30)
    public DB2Schema getHistoryTableSchema()
    {
        return historyTableSchema;
    }

    @Property(viewable = true, order = 31)
    public DB2Table getHistoryTable()
    {
        return historyTable;
    }

}

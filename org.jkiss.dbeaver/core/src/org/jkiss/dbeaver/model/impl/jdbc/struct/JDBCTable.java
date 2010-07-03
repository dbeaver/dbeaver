/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.jdbc.struct;

import org.jkiss.dbeaver.model.impl.meta.AbstractTable;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.dbc.DBCExecutionContext;
import org.jkiss.dbeaver.model.dbc.DBCStatement;
import org.jkiss.dbeaver.model.dbc.DBCResultSet;
import org.jkiss.dbeaver.model.data.DBDDataReciever;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSStructureContainer;
import org.jkiss.dbeaver.model.struct.DBSDataContainer;
import org.jkiss.dbeaver.DBException;

/**
 * JDBC abstract table mplementation
 */
public abstract class JDBCTable<DATASOURCE extends DBPDataSource, CONTAINER extends DBSStructureContainer>
    extends AbstractTable<DATASOURCE, CONTAINER>
    implements DBSDataContainer
{

    protected JDBCTable(CONTAINER container)
    {
        super(container);
    }

    protected JDBCTable(CONTAINER container, String tableName, String tableType, String description)
    {
        super(container, tableName, tableType, description);
    }

    public int pumpAllData(DBRProgressMonitor monitor, DBDDataReciever dataReciever, int firstRow, int maxRows)
        throws DBException
    {
        if (firstRow > 0) {
            throw new DBException("First row number must be 0");
        }
        DBCExecutionContext context = getDataSource().openContext(monitor, "Query table " + getFullQualifiedName() + " data");
        try {
            StringBuilder query = new StringBuilder();
            query.append("SELECT * FROM ").append(getFullQualifiedName());
            DBCStatement dbStat = context.prepareStatement(query.toString(), false, false);
            try {
                dbStat.setDataContainer(this);
                if (maxRows > 0) {
                    dbStat.setLimit(0, maxRows);
                }
                if (!dbStat.executeStatement()) {
                    return 0;
                }
                DBCResultSet dbResult = dbStat.openResultSet();
                if (dbResult == null) {
                    return 0;
                }
                try {
                    dataReciever.fetchStart(monitor, dbResult);
                    try {
                        int rowCount = 0;
                        while (dbResult.nextRow()) {
                            dataReciever.fetchRow(monitor, dbResult);
                            rowCount++;
                        }
                        return rowCount;
                    }
                    finally {
                        dataReciever.fetchEnd(monitor);
                    }
                }
                finally {
                    dbResult.close();
                }
            }
            finally {
                dbStat.close();
            }

        }
        finally {
            context.close();
        }

    }


}

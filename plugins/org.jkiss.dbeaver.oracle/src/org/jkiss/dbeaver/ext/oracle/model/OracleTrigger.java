/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.model;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.dbeaver.ext.oracle.OracleConstants;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.struct.AbstractTrigger;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.struct.DBSActionTiming;
import org.jkiss.dbeaver.model.struct.DBSManipulationType;
import org.jkiss.dbeaver.model.struct.DBSSchema;

import java.sql.ResultSet;

/**
 * GenericProcedure
 */
public class OracleTrigger extends AbstractTrigger
{
    static final Log log = LogFactory.getLog(OracleTrigger.class);

    private OracleSchema schema;
    private OracleTable table;
    private String body;
    private String charsetClient;
    private String sqlMode;

    public OracleTrigger(
        OracleSchema schema,
        OracleTable table,
        ResultSet dbResult)
    {
        this.schema = schema;
        this.table = table;
        loadInfo(dbResult);
    }

    private void loadInfo(ResultSet dbResult)
    {
        setName(JDBCUtils.safeGetString(dbResult, "TRIGGER_NAME"));
        setManipulationType(DBSManipulationType.getByName(JDBCUtils.safeGetString(dbResult, OracleConstants.COL_TRIGGER_EVENT_MANIPULATION)));
        setActionTiming(DBSActionTiming.getByName(JDBCUtils.safeGetString(dbResult, OracleConstants.COL_TRIGGER_ACTION_TIMING)));
        setOrdinalPosition(JDBCUtils.safeGetInt(dbResult, OracleConstants.COL_TRIGGER_ACTION_ORDER));
        this.body = JDBCUtils.safeGetString(dbResult, OracleConstants.COL_TRIGGER_ACTION_STATEMENT);
        this.charsetClient = JDBCUtils.safeGetString(dbResult, OracleConstants.COL_TRIGGER_CHARACTER_SET_CLIENT);
        this.sqlMode = JDBCUtils.safeGetString(dbResult, OracleConstants.COL_TRIGGER_SQL_MODE);
    }

    public String getBody()
    {
        return body;
    }

    @Property(name = "Table", viewable = true, order = 4)
    public OracleTable getTable()
    {
        return table;
    }

    @Property(name = "Client Charset", order = 5)
    public String getCharsetClient()
    {
        return charsetClient;
    }

    @Property(name = "SQL Mode", order = 6)
    public String getSqlMode()
    {
        return sqlMode;
    }

    public DBSSchema getParentObject()
    {
        return schema;
    }

    public OracleDataSource getDataSource()
    {
        return schema.getDataSource();
    }

}
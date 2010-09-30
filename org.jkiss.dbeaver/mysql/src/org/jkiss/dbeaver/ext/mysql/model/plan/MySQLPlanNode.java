/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.mysql.model.plan;

import org.jkiss.dbeaver.ext.mysql.model.MySQLDataSource;
import org.jkiss.dbeaver.model.SQLUtils;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.plan.DBCPlan;
import org.jkiss.dbeaver.model.exec.plan.DBCPlanNode;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;

/**
 * MySQL execution plan node
 */
public class MySQLPlanNode implements DBCPlanNode {

    private long id;
    private String selectType;
    private String table;
    private String type;
    private String possibleKeys;
    private String key;
    private String keyLength;
    private String ref;
    private long rowCount;
    private double filtered;
    private String extra;

    private MySQLPlanNode parent;
    private List<MySQLPlanNode> nested;

    public MySQLPlanNode(MySQLPlanNode parent, ResultSet dbResult) throws SQLException
    {
        this.parent = parent;
        this.id = dbResult.getLong("id");
        this.selectType = dbResult.getString("select_type");
        this.table = dbResult.getString("table");
        this.type = dbResult.getString("type");
        this.possibleKeys = dbResult.getString("possible_keys");
        this.key = dbResult.getString("key");
        this.keyLength = dbResult.getString("key_len");
        this.ref = dbResult.getString("ref");
        this.rowCount = dbResult.getLong("rows");
        this.filtered = dbResult.getDouble("filtered");
        this.extra = dbResult.getString("extra");
    }

    public long getId()
    {
        return id;
    }

    public String getSelectType()
    {
        return selectType;
    }

    public String getTable()
    {
        return table;
    }

    public String getObjectName()
    {
        return table;
    }

    public String getType()
    {
        return type;
    }

    public DBCPlanNode getParent()
    {
        return parent;
    }

    public List<MySQLPlanNode> getNested()
    {
        return nested;
    }

    public String getPossibleKeys()
    {
        return possibleKeys;
    }

    public String getKey()
    {
        return key;
    }

    public String getKeyLength()
    {
        return keyLength;
    }

    public String getRef()
    {
        return ref;
    }

    public long getRowCount()
    {
        return rowCount;
    }

    public double getFiltered()
    {
        return filtered;
    }

    public String getExtra()
    {
        return extra;
    }
}
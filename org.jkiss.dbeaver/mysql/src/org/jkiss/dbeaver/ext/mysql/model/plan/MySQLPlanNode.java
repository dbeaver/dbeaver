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
import org.jkiss.dbeaver.model.meta.Property;

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

    public String getObjectName()
    {
        return String.valueOf(id);
    }

    public DBCPlanNode getParent()
    {
        return parent;
    }

    public List<MySQLPlanNode> getNested()
    {
        return nested;
    }

    public long getId()
    {
        return id;
    }

    @Property(name = "Select Type", order = 1, viewable = true)
    public String getSelectType()
    {
        return selectType;
    }

    @Property(name = "Table", order = 2, viewable = true)
    public String getTable()
    {
        return table;
    }

    @Property(name = "Type", order = 3, viewable = true)
    public String getType()
    {
        return type;
    }

    @Property(name = "Possible Keys", order = 4, viewable = true)
    public String getPossibleKeys()
    {
        return possibleKeys;
    }

    @Property(name = "Key", order = 5, viewable = true)
    public String getKey()
    {
        return key;
    }

    @Property(name = "Key Length", order = 6, viewable = true)
    public String getKeyLength()
    {
        return keyLength;
    }

    @Property(name = "Ref", order = 7, viewable = true)
    public String getRef()
    {
        return ref;
    }

    @Property(name = "Rows", order = 8, viewable = true)
    public long getRowCount()
    {
        return rowCount;
    }

    @Property(name = "Filtered", order = 9, viewable = true)
    public double getFiltered()
    {
        return filtered;
    }

    @Property(name = "Extra", order = 10, viewable = true)
    public String getExtra()
    {
        return extra;
    }
}
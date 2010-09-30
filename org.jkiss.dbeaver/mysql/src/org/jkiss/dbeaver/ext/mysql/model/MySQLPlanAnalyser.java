package org.jkiss.dbeaver.ext.mysql.model;

import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.plan.DBCPlan;

/**
 * MySQL execution plan analyser
 */
public class MySQLPlanAnalyser implements DBCPlan {

    private MySQLDataSource dataSource;
    private DBCExecutionContext context;
    private String query;

    public MySQLPlanAnalyser(MySQLDataSource dataSource, DBCExecutionContext context, String query)
    {
        this.dataSource = dataSource;
        this.context = context;
        this.query = query;
    }

}

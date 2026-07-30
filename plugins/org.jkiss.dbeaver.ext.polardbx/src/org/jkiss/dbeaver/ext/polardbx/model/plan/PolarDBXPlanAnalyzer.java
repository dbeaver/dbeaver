/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0
 */
package org.jkiss.dbeaver.ext.polardbx.model.plan;

import org.jkiss.dbeaver.ext.mysql.model.plan.MySQLPlanAbstract;
import org.jkiss.dbeaver.ext.mysql.model.plan.MySQLPlanAnalyser;
import org.jkiss.dbeaver.ext.polardbx.mysql.model.PolarDBXMySQLDataSource;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.sql.SQLUtils;

public class PolarDBXPlanAnalyzer extends MySQLPlanAnalyser {
    private static final String[] FIRST_KEYWORD_BLOCK_LIST = new String[]{
        "DESC", "SET", "EXPLAIN"
    };
    private final PolarDBXMySQLDataSource dataSource;

    public PolarDBXPlanAnalyzer(PolarDBXMySQLDataSource dataSource) {
        super(dataSource);
        this.dataSource = dataSource;
    }

    private static boolean block(String firstKeyword) {
        for (String blockWord : FIRST_KEYWORD_BLOCK_LIST) {
            if (!blockWord.equalsIgnoreCase(firstKeyword)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public MySQLPlanAbstract explain(JDBCSession session, String query) throws DBCException {
        final SQLDialect dialect = SQLUtils.getDialectFromObject(this.dataSource);
        final String plainQuery = SQLUtils.stripComments(dialect, query).toUpperCase();
        final String firstKeyword = SQLUtils.getFirstKeyword(dialect, plainQuery);
        if (PolarDBXPlanAnalyzer.block(firstKeyword)) {
            throw new DBCException("This statement could not produce execution plan");
        }
        return new PolarDBXPlainClassic(session, query);
    }
}
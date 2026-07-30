/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0
 */
package org.jkiss.dbeaver.ext.polardbx.mysql.model;

import org.jkiss.dbeaver.ext.mysql.model.MySQLDialect;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCDatabaseMetaData;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSource;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.dbeaver.model.sql.SQLDialectDDLExtension;

import java.util.Arrays;

public class PolarDBXDialect extends MySQLDialect implements SQLDialectDDLExtension {

    public PolarDBXDialect() {
        super("PolarDB-X", "polardbx");
    }

    public static final String[] POLARDBX_NON_TRANSACTIONAL_KEYWORDS = {
        // Removed "GLOBAL" to avoid it being incorrectly classified as a non-transactional keyword.
    };
    private static final String[] POLARDBX_ADVANCED_KEYWORDS = {
        "GLOBAL", "COVERING"
    };
    private static final String[] POLARDBX_EXTRA_FUNCTIONS = {

    };

    @Override
    public void initDriverSettings(JDBCSession session, JDBCDataSource dataSource, JDBCDatabaseMetaData metaData) {
        super.initBaseDriverSettings(session, dataSource, metaData);

        for (String kw : POLARDBX_ADVANCED_KEYWORDS) {
            addSQLKeyword(kw);
        }
        for (String kw : POLARDBX_NON_TRANSACTIONAL_KEYWORDS) {
            addSQLKeyword(kw);
        }
        addFunctions(Arrays.asList(POLARDBX_EXTRA_FUNCTIONS));
    }

    @NotNull
    public String[] getNonTransactionKeywords() {
        return ArrayUtils.concatArrays(
            MySQLDialect.MYSQL_NON_TRANSACTIONAL_KEYWORDS,
            POLARDBX_NON_TRANSACTIONAL_KEYWORDS
        );
    }

    @NotNull
    @Override
    public String[] getExecuteKeywords() {
        return new String[] {};
    }

    @Override
    public boolean supportsAlterHasColumn() {
        // In PolarDB-X ALTER syntax, in scenarios like ADD GLOBAL INDEX the GLOBAL that follows should not be parsed as a column name.
        return false;
    }

    @Override
    public boolean supportsAlterColumnSet() {
        // Conservatively return false to avoid the semantics of the SET branch affecting PolarDB-X ALTER syntax parsing.
        return false;
    }
}
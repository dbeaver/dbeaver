/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.runtime.qm;

import java.util.Map;

/**
 * DBCStatement meta info
 */
public class QMStatementMetaInfo {

    public class QMExecuteMetaInfo {
        private QMTransactionMetaInfo transaction;
        private String queryString;
        private Map<Object, Object> parameters;

        private long fetchRowCount;
        private long updateRowCount;

        private Throwable executionError;
        private long executeTime;
        private long fetchTime;
    }
    
    private long openTime;
    private long closeTime;
    private Object executions;

}

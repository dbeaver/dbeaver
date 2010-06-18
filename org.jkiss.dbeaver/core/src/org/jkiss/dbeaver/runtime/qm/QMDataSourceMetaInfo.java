/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.runtime.qm;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Data source information
 */
class QMDataSourceMetaInfo {

    private Set<QMStatementMetaInfo> activeStatements = new TreeSet<QMStatementMetaInfo>();
    private List<QMStatementMetaInfo> statements;
    private List<QMTransactionMetaInfo> transactions;

}

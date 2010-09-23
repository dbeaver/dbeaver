/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.runtime.qm;

import java.util.List;

/**
 * QM Transaction info
 */
public class QMTransactionMetaInfo {

    private long startTime;
    private long endTime;
    private boolean commited;
    private List<?> savepoints;

}

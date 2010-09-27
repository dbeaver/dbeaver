/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.runtime.qm.meta;

import java.util.List;

/**
 * QM Transaction info
 */
public class QMMTransactionInfo {

    private long startTime;
    private long endTime;
    private boolean commited;
    private List<?> savepoints;

}

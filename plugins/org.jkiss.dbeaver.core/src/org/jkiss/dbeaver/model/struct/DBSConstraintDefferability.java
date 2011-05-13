/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.struct;

/**
 * DBSConstraintModifyRule
 */
public enum DBSConstraintDefferability
{
    UNKNOWN,
    INITIALLY_DEFERRED,
    INITIALLY_IMMEDIATE,
    NOT_DEFERRABLE,
}

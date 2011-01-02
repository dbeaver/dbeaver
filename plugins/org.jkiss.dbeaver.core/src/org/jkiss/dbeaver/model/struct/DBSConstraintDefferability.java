/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.struct;

/**
 * DBSConstraintCascade
 */
public enum DBSConstraintDefferability
{
    UNKNOWN,
    INITIALLY_DEFERRED,
    INITIALLY_IMMEDIATE,
    NOT_DEFERRABLE,
}

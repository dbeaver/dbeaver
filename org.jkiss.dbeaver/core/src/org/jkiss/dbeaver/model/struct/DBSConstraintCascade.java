/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.struct;

/**
 * DBSConstraintCascade
 */
public enum DBSConstraintCascade
{
    UNKNOWN,
    NO_ACTION,
    CASCADE,
    SET_NULL,
    SET_DEFAULT,
    RESTRICT,
}

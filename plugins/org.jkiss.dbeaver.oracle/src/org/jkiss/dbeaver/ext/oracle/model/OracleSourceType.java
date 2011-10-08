/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.model;

/**
 * Stored code interface
 */
public enum OracleSourceType {

    TYPE(false),
    PROCEDURE(false),
    FUNCTION(false),
    PACKAGE(false),
    TRIGGER(false),
    VIEW(true),
    MATERIALIZED_VIEW(true);

    private final boolean isCustom;

    OracleSourceType(boolean custom)
    {
        isCustom = custom;
    }

    public boolean isCustom()
    {
        return isCustom;
    }
}

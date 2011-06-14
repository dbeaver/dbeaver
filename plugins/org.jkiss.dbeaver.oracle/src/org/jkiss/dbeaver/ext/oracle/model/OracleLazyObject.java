/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.model;

/**
 * Lazy object
 */
public interface OracleLazyObject<OBJECT_TYPE> {

    OBJECT_TYPE getObject();

}

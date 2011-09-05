/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.model;

/**
 * Stored code interface
 */
public interface OracleSourceEditable extends OracleSourceObject {

    boolean isSourceValid();

    void setSourceValid(boolean valid);

    String getCompileQuery();

}

/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.edit;

/**
 * Command visitor.
 */
public interface DBECommandVisitor {

    boolean visit(DBECommand command);

}
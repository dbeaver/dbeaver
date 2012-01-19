/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.struct;

/**
 * DBSEntityAssociation
 */
public interface DBSEntityAssociation extends DBSObject {

    DBSEntityConstraintType getConstraintType();

    DBSEntity getParentObject();

    DBSEntity getAssociatedEntity();

    // target entity
    // identifying
    // multiplicity
    // bidirectional
}
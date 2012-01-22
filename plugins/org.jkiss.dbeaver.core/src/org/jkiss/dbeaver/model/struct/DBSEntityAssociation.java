/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.struct;

/**
 * DBSEntityAssociation
 */
public interface DBSEntityAssociation extends DBSEntityConstraint {

    DBSEntityConstraint getReferencedConstraint();

    DBSEntity getAssociatedEntity();

    // target entity
    // identifying
    // multiplicity
    // bidirectional
}
/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.struct;

import org.jkiss.dbeaver.model.DBPDataSource;

/**
 * DBSForeignKey
 */
public interface DBSForeignKey extends DBSConstraint
{
    DBSConstraint getReferencedKey();

    DBSConstraintCascade getDeleteRule();

    DBSConstraintCascade getUpdateRule();

    DBSConstraintDefferability getDefferability();

}

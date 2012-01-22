/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.struct;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.util.Collection;

/**
 * DBSEntityReferrer
 */
public interface DBSEntityReferrer extends DBSEntityConstraint {

    Collection<? extends DBSEntityAttributeRef> getAttributeReferences(DBRProgressMonitor monitor);

}
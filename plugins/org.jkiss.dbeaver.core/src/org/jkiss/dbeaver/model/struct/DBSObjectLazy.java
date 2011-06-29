/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.struct;

import org.jkiss.dbeaver.model.DBPDataSource;

/**
 * Lazy object
 */
public interface DBSObjectLazy<DATA_SOURCE extends DBPDataSource> {

    DATA_SOURCE getDataSource();

    Object getLazyReference(Object propertyId);
}

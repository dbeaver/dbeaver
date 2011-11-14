/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model;

import java.util.Collection;

/**
 * DBPClientManager
 */
public interface DBPClientManager {

    Collection<DBPClientHome> findHomes();

    DBPClientHome getDefaultHome();

}

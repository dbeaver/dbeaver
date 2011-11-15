/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model;

import java.util.Collection;

/**
 * Native client manager.
 * This interface can be implemented by data source provider to support native client functions.
 */
public interface DBPClientManager {

    Collection<String> findClientHomeIds();

    String getDefaultClientHomeId();

    DBPClientHome getClientHome(String homeId);

}

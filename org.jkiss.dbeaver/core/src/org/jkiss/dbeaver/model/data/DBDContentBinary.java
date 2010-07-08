/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.data;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.runtime.sql.ISQLQueryListener;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.io.InputStream;

/**
 * DBC BLOB
 *
 * @author Serge Rider
 */
public interface DBDContentBinary extends DBDContent {

    InputStream getContents() throws DBException;

}

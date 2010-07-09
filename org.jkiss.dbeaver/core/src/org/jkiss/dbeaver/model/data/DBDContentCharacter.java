/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.data;

import org.jkiss.dbeaver.DBException;

import java.io.Reader;

/**
 * DBC CLOB
 *
 * @author Serge Rider
 */
public interface DBDContentCharacter extends DBDContent {

    String getCharset();

    Reader getContents() throws DBException;

}

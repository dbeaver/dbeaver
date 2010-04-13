/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.dbc;

/**
 * DBC CLOB
 *
 * @author Serge Rider
 */
public interface DBCCLOB extends DBCLOB {

    String getString(long pos, int length) throws DBCException;

    int setString(long pos, String str, int offset, int len) throws DBCException;

    java.io.Reader getCharacterStream() throws DBCException;

    java.io.Writer setCharacterStream(long pos) throws DBCException;

}

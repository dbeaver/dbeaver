package org.jkiss.dbeaver.model.dbc;

/**
 * DBC BLOB
 *
 * @author Serge Rider
 */
public interface DBCBLOB extends DBCLOB {

    byte[] getBytes(long pos, int length) throws DBCException;

    int setBytes(long pos, byte[] bytes, int offset, int len) throws DBCException;

    java.io.InputStream getBinaryStream () throws DBCException;

    java.io.OutputStream setBinaryStream(long pos) throws DBCException;

}

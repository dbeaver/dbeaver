/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.data;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.sql.Clob;
import java.sql.SQLException;
import java.sql.SQLXML;

/**
 * SQL XML implementation
 */
public class OracleXMLWrapper implements SQLXML {

    private final Object xmlType;

    public OracleXMLWrapper(Object xmlType)
    {
        this.xmlType = xmlType;
    }

    @Override
    public void free() throws SQLException
    {
        try {
            xmlType.getClass().getMethod("close").invoke(xmlType);
        } catch (Throwable e) {
            throw new SQLException("Can't free XMLType", e);
        }
    }

    @Override
    public InputStream getBinaryStream() throws SQLException
    {
        try {
            return (InputStream) xmlType.getClass().getMethod("getInputStream").invoke(xmlType);
        } catch (Throwable e) {
            throw new SQLException("Can't obtain binary stream from XMLType", e);
        }
    }

    @Override
    public OutputStream setBinaryStream() throws SQLException
    {
        throw new SQLException("Function not supported");
    }

    @Override
    public Reader getCharacterStream() throws SQLException
    {
        try {
            final Clob clob = (Clob)xmlType.getClass().getMethod("getClobVal").invoke(xmlType);
            return clob.getCharacterStream();
        } catch (Throwable e) {
            throw new SQLException("Can't obtain binary stream from XMLType", e);
        }
    }

    @Override
    public Writer setCharacterStream() throws SQLException
    {
        throw new SQLException("Function not supported");
    }

    @Override
    public String getString() throws SQLException
    {
        try {
            return (String) xmlType.getClass().getMethod("getStringVal").invoke(xmlType);
        } catch (Throwable e) {
            throw new SQLException("Can't obtain binary stream from XMLType", e);
        }
    }

    @Override
    public void setString(String value) throws SQLException
    {
        throw new SQLException("Function not supported");
    }

    @Override
    public <T extends Source> T getSource(Class<T> sourceClass) throws SQLException
    {
        return null;
    }

    @Override
    public <T extends Result> T setResult(Class<T> resultClass) throws SQLException
    {
        throw new SQLException("Function not supported");
    }

}

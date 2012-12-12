/*
 * Copyright (C) 2010-2012 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
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

/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.ext.oracle.data;

import org.jkiss.utils.BeanUtils;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.sql.SQLXML;

/**
 * Oracle XML wrapper.
 * Actual type of xmlType object is oracle.xdb.XMLType
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
            BeanUtils.invokeObjectMethod(xmlType, "close", null, null);
        } catch (Throwable e) {
            throw new SQLException("Can't close XMLType", e);
        }
    }

    @Override
    public InputStream getBinaryStream() throws SQLException
    {
        try {
            return (InputStream) BeanUtils.invokeObjectMethod(xmlType, "getInputStream", null, null);
        } catch (InvocationTargetException e) {
            if (e.getTargetException() instanceof SQLException) {
                throw (SQLException)e.getTargetException();
            }
            throw new SQLException(e);
        } catch (Throwable e) {
            throw new SQLException(e);
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
            Object clobVal = BeanUtils.invokeObjectMethod(xmlType, "getClobVal", null, null);
            return (Reader) BeanUtils.invokeObjectMethod(clobVal, "getCharacterStream", null, null);
        } catch (InvocationTargetException e) {
            if (e.getTargetException() instanceof SQLException) {
                throw (SQLException)e.getTargetException();
            }
            throw new SQLException(e);
        } catch (Throwable e) {
            throw new SQLException(e);
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
            return (String) BeanUtils.invokeObjectMethod(xmlType, "getStringVal", null, null);
        } catch (InvocationTargetException e) {
            if (e.getTargetException() instanceof SQLException) {
                throw (SQLException)e.getTargetException();
            }
            throw new SQLException(e);
        } catch (Throwable e) {
            throw new SQLException(e);
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

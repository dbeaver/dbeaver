/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.model.impl.jdbc.data;

import org.jkiss.dbeaver.model.data.DBDContentStorage;
import org.jkiss.utils.IOUtils;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import java.io.*;
import java.sql.SQLException;
import java.sql.SQLXML;

/**
 * Base XML wrapper.
 */
public class JDBCSQLXMLImpl implements SQLXML {

    private final DBDContentStorage storage;

    public JDBCSQLXMLImpl(DBDContentStorage storage) {
        this.storage = storage;
    }

    @Override
    public void free() throws SQLException
    {
        storage.release();
    }

    @Override
    public InputStream getBinaryStream() throws SQLException
    {
        try {
            return storage.getContentStream();
        } catch (IOException e) {
            throw new SQLException("IO error", e);
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
            return storage.getContentReader();
        } catch (IOException e) {
            throw new SQLException("IO error", e);
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
        StringWriter out = new StringWriter();
        try {
            IOUtils.copyText(getCharacterStream(), out);
        } catch (IOException e) {
            throw new SQLException("IO error reading string value");
        }
        return out.toString();
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

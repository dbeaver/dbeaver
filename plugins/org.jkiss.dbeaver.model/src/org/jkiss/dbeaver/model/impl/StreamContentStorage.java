/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.model.impl;

import org.jkiss.dbeaver.model.data.DBDContentStorage;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

/**
 * Stream content storage
 */
public class StreamContentStorage implements DBDContentStorage {

    private final InputStream stream;

    public StreamContentStorage(InputStream stream)
    {
        this.stream = stream;
    }

    @Override
    public InputStream getContentStream()
        throws IOException
    {
        return stream;
    }

    @Override
    public Reader getContentReader()
        throws IOException
    {
        return new InputStreamReader(stream, getCharset());
    }

    @Override
    public long getContentLength()
    {
        return -1;
    }

    @Override
    public String getCharset()
    {
        return GeneralUtils.DEFAULT_ENCODING;
    }

    @Override
    public DBDContentStorage cloneStorage(DBRProgressMonitor monitor)
        throws IOException
    {
        return new StreamContentStorage(stream);
    }

    @Override
    public void release()
    {
        IOUtils.close(stream);
    }

}

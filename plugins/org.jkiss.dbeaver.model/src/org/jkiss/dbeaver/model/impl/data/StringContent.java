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
package org.jkiss.dbeaver.model.impl.data;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.data.DBDContent;
import org.jkiss.dbeaver.model.data.DBDContentStorage;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.impl.StringContentStorage;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.utils.ContentUtils;
import org.jkiss.dbeaver.utils.MimeTypes;

import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;

/**
 * StringContent
 *
 * @author Serge Rider
 */
public class StringContent extends AbstractContent {

    private StringContentStorage storage;

    public StringContent(DBPDataSource dataSource, String data) {
        super(dataSource);
        this.storage = new StringContentStorage(data);
    }

    @Override
    public long getContentLength() throws DBCException {
        return storage.getContentLength();
    }

    @Override
    public String getContentType() {
        return MimeTypes.TEXT_PLAIN;
    }

    @Override
    public String getDisplayString(DBDDisplayFormat format) {
        return storage.getCachedValue();
    }

    @Override
    public DBDContentStorage getContents(DBRProgressMonitor monitor) throws DBCException {
        return storage;
    }

    @Override
    public boolean updateContents(DBRProgressMonitor monitor, DBDContentStorage storage) throws DBException {
        try {
            try (Reader reader = storage.getContentReader()) {
                StringWriter sw = new StringWriter((int)storage.getContentLength());
                ContentUtils.copyStreams(reader, storage.getContentLength(), sw, monitor);
                this.storage = new StringContentStorage(sw.toString());
            }
        }
        catch (IOException e) {
            throw new DBCException("IO error while reading content", e);
        }
        return true;
    }

    @Override
    public Object getRawValue() {
        return storage.getCachedValue();
    }

    @Override
    public boolean isNull() {
        return false;
    }

    @Override
    public void release() {

    }
}

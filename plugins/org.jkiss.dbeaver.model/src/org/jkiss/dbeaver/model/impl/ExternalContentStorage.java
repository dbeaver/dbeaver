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
package org.jkiss.dbeaver.model.impl;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.app.DBPPlatform;
import org.jkiss.dbeaver.model.data.DBDContentStorage;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.utils.ContentUtils;
import org.jkiss.dbeaver.utils.GeneralUtils;

import java.io.*;

/**
 * File content storage
 */
public class ExternalContentStorage implements DBDContentStorage {

    @NotNull
    private final DBPPlatform platform;
    @NotNull
    private File file;
    private String charset;

    public ExternalContentStorage(@NotNull DBPPlatform platform, @NotNull File file)
    {
        this(platform, file, null);
    }

    public ExternalContentStorage(@NotNull DBPPlatform platform, @NotNull File file, String charset)
    {
        this.platform = platform;
        this.file = file;
        this.charset = charset;
    }

    @NotNull
    public File getFile() {
        return file;
    }

    @NotNull
    @Override
    public InputStream getContentStream()
        throws IOException
    {
        return new FileInputStream(file);
    }

    @NotNull
    @Override
    public Reader getContentReader()
        throws IOException
    {
        if (charset == null) {
            return new InputStreamReader(new FileInputStream(file), GeneralUtils.DEFAULT_ENCODING);
        } else {
            return new InputStreamReader(new FileInputStream(file), charset);
        }
    }

    @Override
    public long getContentLength()
    {
        return file.length();
    }

    @Override
    public String getCharset()
    {
        return charset;
    }

    @Override
    public DBDContentStorage cloneStorage(DBRProgressMonitor monitor)
        throws IOException
    {
        // Create new local storage
        File tempFile = ContentUtils.createTempContentFile(monitor, platform, "copy" + this.hashCode());
        try {
            try (InputStream is = new FileInputStream(file)) {
                try (OutputStream os = new FileOutputStream(tempFile)) {
                    ContentUtils.copyStreams(is, file.length(), os, monitor);
                }
            }
        } catch (IOException e) {
            ContentUtils.deleteTempFile(tempFile);
            throw new IOException(e);
        }
        return new TemporaryContentStorage(platform, tempFile, charset);
    }

    @Override
    public void release()
    {
        // Do nothing
/*
        if (!file.delete()) {
            log.warn("Could not delete temporary file");
        }
*/
    }
}
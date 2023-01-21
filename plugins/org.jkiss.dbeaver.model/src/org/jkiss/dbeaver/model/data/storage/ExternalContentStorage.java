/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.data.storage;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.app.DBPPlatform;
import org.jkiss.dbeaver.model.data.DBDContentStorage;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.utils.ContentUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * File content storage
 */
public class ExternalContentStorage implements DBDContentStorage {

    @NotNull
    private final DBPPlatform platform;
    @NotNull
    private Path file;
    private String charset;

    public ExternalContentStorage(@NotNull DBPPlatform platform, @NotNull Path file)
    {
        this(platform, file, null);
    }

    public ExternalContentStorage(@NotNull DBPPlatform platform, @NotNull Path file, String charset)
    {
        this.platform = platform;
        this.file = file;
        this.charset = charset;
    }

    @NotNull
    public Path getFile() {
        return file;
    }

    @NotNull
    @Override
    public InputStream getContentStream()
        throws IOException
    {
        return Files.newInputStream(file);
    }

    @NotNull
    @Override
    public Reader getContentReader()
        throws IOException
    {
        if (charset == null) {
            return Files.newBufferedReader(file, StandardCharsets.UTF_8);
        } else {
            return Files.newBufferedReader(file, Charset.forName(charset));
        }
    }

    @Override
    public long getContentLength() throws IOException {
        return Files.size(file);
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
        Path tempFile = ContentUtils.createTempContentFile(monitor, platform, "copy" + this.hashCode());
        try {
            try (InputStream is = Files.newInputStream(file)) {
                try (OutputStream os = Files.newOutputStream(tempFile)) {
                    ContentUtils.copyStreams(is, Files.size(file), os, monitor);
                }
            }
        } catch (IOException e) {
            ContentUtils.deleteTempFile(tempFile);
            throw new IOException(e);
        }
        return new TemporaryContentStorage(platform, tempFile, charset, true);
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
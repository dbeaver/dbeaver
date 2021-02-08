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
package org.jkiss.dbeaver.model.impl.data;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.data.DBDContent;
import org.jkiss.dbeaver.model.data.DBDContentStorage;
import org.jkiss.dbeaver.model.data.DBDDocument;
import org.jkiss.dbeaver.model.data.storage.BytesContentStorage;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.utils.ContentUtils;

import java.io.*;
import java.nio.charset.Charset;

/**
 * Content proxy document
 */
public abstract class DBDDocumentContentProxy implements DBDDocument {

    @NotNull
    protected final DBDContent content;
    protected final DBDDocument document;

    protected DBDDocumentContentProxy(@NotNull DBDContent content) throws DBException {
        this.content = content;
        this.document = createDocumentFromContent(content);
    }

    @NotNull
    protected abstract DBDDocument createDocumentFromContent(@NotNull DBDContent content) throws DBException;

    @Nullable
    @Override
    public Object getDocumentId() {
        return document.getDocumentId();
    }

    @Nullable
    @Override
    public Object getDocumentProperty(String name) {
        return document.getDocumentProperty(name);
    }

    @NotNull
    @Override
    public String getDocumentContentType() {
        return document.getDocumentContentType();
    }

    @NotNull
    @Override
    public Object getRootNode() {
        return document.getRootNode();
    }

    @Override
    public void serializeDocument(@NotNull DBRProgressMonitor monitor, @NotNull OutputStream stream, Charset charset) throws DBException {
        DBDContentStorage contents = content.getContents(monitor);
        if (contents != null) {
            try {
                try (InputStream contentStream = contents.getContentStream()) {
                    ContentUtils.copyStreams(contentStream, content.getContentLength(), stream, monitor);
                }
            } catch (IOException e) {
                throw new DBException("Error copying content stream", e);
            }
        }
    }

    @Override
    public void updateDocument(@NotNull DBRProgressMonitor monitor, @NotNull InputStream stream, Charset charset) throws DBException {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ContentUtils.copyStreams(stream, -1, baos, monitor);
            content.updateContents(monitor, new BytesContentStorage(baos.toByteArray(), charset));

            document.updateDocument(monitor, new ByteArrayInputStream(baos.toByteArray()), charset);
        } catch (IOException e) {
            throw new DBException("Error transforming XML document", e);
        }
    }

    @Override
    public Object getRawValue() {
        return document.getRawValue();
    }

    @Override
    public boolean isNull() {
        return document.isNull();
    }

    @Override
    public boolean isModified() {
        return document.isModified();
    }

    @Override
    public void release() {
        document.release();
    }

}

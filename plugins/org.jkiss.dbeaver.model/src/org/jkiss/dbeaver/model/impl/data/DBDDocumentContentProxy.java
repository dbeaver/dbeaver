/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
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
package org.jkiss.dbeaver.model.impl.data;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.data.DBDContent;
import org.jkiss.dbeaver.model.data.DBDContentStorage;
import org.jkiss.dbeaver.model.data.DBDDocument;
import org.jkiss.dbeaver.model.impl.BytesContentStorage;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.utils.ContentUtils;

import java.io.*;

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
    public void serializeDocument(@NotNull DBRProgressMonitor monitor, @NotNull OutputStream stream, String encoding) throws DBException {
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
    public void updateDocument(@NotNull DBRProgressMonitor monitor, @NotNull InputStream stream, String encoding) throws DBException {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ContentUtils.copyStreams(stream, -1, baos, monitor);
            content.updateContents(monitor, new BytesContentStorage(baos.toByteArray(), encoding));

            document.updateDocument(monitor, new ByteArrayInputStream(baos.toByteArray()), encoding);
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
    public void release() {
        document.release();
    }

}

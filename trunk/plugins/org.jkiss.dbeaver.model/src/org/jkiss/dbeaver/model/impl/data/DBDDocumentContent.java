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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Content proxy document
 */
public class DBDDocumentContent implements DBDDocument {

    private DBDContent content;

    public DBDDocumentContent(DBDContent content) {
        this.content = content;
    }

    @Nullable
    @Override
    public Object getDocumentProperty(String name) {
        if (PROP_ID.equals(name)) {
            return null;
        }
        return null;
    }

    @NotNull
    @Override
    public String getDocumentContentType() {
        return content.getContentType();
    }

    @NotNull
    @Override
    public Object getRootNode() {
        return content;
    }

    @Override
    public void serializeDocument(@NotNull DBRProgressMonitor monitor, @NotNull OutputStream stream, String encoding) throws DBException {
        DBDContentStorage contents = content.getContents(monitor);
        if (contents != null) {
            try {
                InputStream contentStream = contents.getContentStream();
                try {
                    ContentUtils.copyStreams(contentStream, content.getContentLength(), stream, monitor);
                } finally {
                    ContentUtils.close(contentStream);
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
        } catch (IOException e) {
            throw new DBException("Error transforming XML document", e);
        }
    }

    @Override
    public Object getRawValue() {
        return content;
    }

    @Override
    public boolean isNull() {
        return content.isNull();
    }

    @Override
    public void release() {
        content.release();
    }

}

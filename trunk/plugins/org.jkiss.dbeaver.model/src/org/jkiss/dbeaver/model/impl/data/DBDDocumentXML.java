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
import org.jkiss.dbeaver.model.data.DBDDocument;
import org.jkiss.dbeaver.utils.MimeTypes;
import org.w3c.dom.Document;

import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * XML document
 */
public class DBDDocumentXML implements DBDDocument {

    private Document document;

    @Nullable
    @Override
    public Object getDocumentProperty(String name) {
        if (PROP_ID.equals(name)) {
            return document.getDocumentURI();
        }
        return null;
    }

    @NotNull
    @Override
    public String getDocumentContentType() {
        return MimeTypes.TEXT_XML;
    }

    @NotNull
    @Override
    public Object getRootNode() {
        return document;
    }

    @Override
    public void serializeDocument(@NotNull OutputStream stream) throws DBException {
        try {
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            Result output = new StreamResult(stream);

            transformer.transform(
                new DOMSource(document),
                output);
        } catch (TransformerException e) {
            throw new DBException("Error serializing XML document", e);
        }
    }

    @Override
    public void updateDocument(@NotNull InputStream stream) throws DBException {
        try {
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            DOMResult output = new DOMResult();

            transformer.transform(
                new StreamSource(stream),
                output);
            document = (Document) output.getNode();
        } catch (TransformerException e) {
            throw new DBException("Error transforming XML document", e);
        }
    }

    @Override
    public Object getRawValue() {
        return document;
    }

    @Override
    public boolean isNull() {
        return document == null;
    }

    @Override
    public void release() {
        document = null;
    }

}

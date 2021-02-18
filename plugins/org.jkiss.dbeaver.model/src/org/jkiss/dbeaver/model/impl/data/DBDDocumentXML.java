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
import org.jkiss.dbeaver.model.data.DBDDocument;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.utils.MimeTypes;
import org.w3c.dom.Document;

import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;

/**
 * XML document
 */
public class DBDDocumentXML implements DBDDocument {

    private Document document;
    private boolean modified;

    public DBDDocumentXML(Document document) {
        this.document = document;
    }

    @Nullable
    @Override
    public Object getDocumentId() {
        return document.getDocumentURI();
    }

    @Nullable
    @Override
    public Object getDocumentProperty(String name) {
        if (PROP_ID.equals(name)) {
            return getDocumentId();
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
        return document.getDocumentElement();
    }

    @Override
    public void serializeDocument(@NotNull DBRProgressMonitor monitor, @NotNull OutputStream stream, Charset charset) throws DBException {
        try {
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            Result output = new StreamResult(new OutputStreamWriter(stream, charset));

            transformer.transform(
                new DOMSource(document),
                output);
        } catch (Exception e) {
            throw new DBException("Error serializing XML document", e);
        }
    }

    @Override
    public void updateDocument(@NotNull DBRProgressMonitor monitor, @NotNull InputStream stream, Charset charset) throws DBException {
        try {
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            DOMResult output = new DOMResult();

            transformer.transform(
                new StreamSource(new InputStreamReader(stream, charset)),
                output);
            document = (Document) output.getNode();
            modified = true;
        } catch (Exception e) {
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
    public boolean isModified() {
        return modified;
    }

    @Override
    public void release() {
        document = null;
    }

}

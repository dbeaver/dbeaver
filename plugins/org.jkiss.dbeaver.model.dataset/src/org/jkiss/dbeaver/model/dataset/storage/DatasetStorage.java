/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2022 DBeaver Corp and others
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

package org.jkiss.dbeaver.model.dataset.storage;

import org.eclipse.core.runtime.CoreException;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.dataset.DBDDataSet;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.IOUtils;
import org.jkiss.utils.xml.XMLBuilder;
import org.jkiss.utils.xml.XMLUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Dataset storage
 */
public class DatasetStorage {

    public static final String ATTR_NAME = "name"; //NON-NLS-1
    public static final String ATTR_DESCRIPTION = "description"; //NON-NLS-1
    public static final String ATTR_DRAFT = "draft"; //NON-NLS-1
    public static final String ATTR_DATA_SOURCE = "data-source"; //NON-NLS-1

    private DBDDataSet dataSet;

    public DatasetStorage(DBDDataSet dataSet) {
        this.dataSet = dataSet;
    }

    public DatasetStorage(Path file) throws DBException, CoreException {
        dataSet = new DBDDataSet();
        dataSet.setId(IOUtils.getFileNameWithoutExtension(file));

        try (InputStream contents = Files.newInputStream(file)) {
            final Document document = XMLUtils.parseDocument(contents);
            final Element root = document.getDocumentElement();
            dataSet.setDisplayName(root.getAttribute(ATTR_NAME));
            dataSet.setDescription(root.getAttribute(ATTR_DESCRIPTION));
            dataSet.setDraft(CommonUtils.toBoolean(root.getAttribute(ATTR_DRAFT)));
        } catch (Exception e) {
            throw new DBException("IO Error reading dataset from '" + file.toAbsolutePath().toString() + "'", e);
        }
    }

    public DBDDataSet getDataSet() {
        return dataSet;
    }

    public ByteArrayInputStream serialize() throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream(5000);
        XMLBuilder xml = new XMLBuilder(buffer, GeneralUtils.getDefaultFileEncoding());
        xml.startElement("dataset");
        xml.addAttribute(ATTR_NAME, dataSet.getDisplayName());
        if (dataSet.getDescription() != null) {
            xml.addAttribute(ATTR_DESCRIPTION, dataSet.getDescription());
        }
        xml.endElement();
        xml.flush();
        return new ByteArrayInputStream(buffer.toByteArray());
    }

}

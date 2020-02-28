/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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

package org.jkiss.dbeaver.tools.project;

import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.wizard.WizardPage;
import org.jkiss.utils.xml.XMLException;
import org.jkiss.utils.xml.XMLUtils;
import org.w3c.dom.Document;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Import data
 */
class ProjectImportData {

    private File importFile = null;
    private boolean importDriverLibraries = true;
    private Document metaTree;
    private Map<String, String> projectNameMap = new HashMap<>();

    public File getImportFile()
    {
        return importFile;
    }

    public void setImportFile(File importFile)
    {
        this.importFile = importFile;
        this.metaTree = null;
    }

    public boolean isImportDriverLibraries()
    {
        return importDriverLibraries;
    }

    public void setImportDriverLibraries(boolean importDriverLibraries)
    {
        this.importDriverLibraries = importDriverLibraries;
    }

    public Document getMetaTree()
    {
        return metaTree;
    }

    boolean isFileSpecified(WizardPage page)
    {
        if (importFile == null) {
            page.setMessage("Import file not specified", IMessageProvider.ERROR);
            return false;
        }
        if (!importFile.exists()) {
            page.setMessage("File '" + importFile.getAbsolutePath() + "' doesn't exist", IMessageProvider.ERROR);
            return false;
        }
        if (!importFile.isFile()) {
            page.setMessage("File '" + importFile.getAbsolutePath() + "' is a directory", IMessageProvider.ERROR);
            return false;
        }
        page.setMessage("Configure project import settings", IMessageProvider.NONE);
        return true;
    }

    public boolean isProjectsSelected(WizardPage page)
    {
        return !projectNameMap.isEmpty();
    }

    boolean loadArchiveMeta(WizardPage page)
    {
        try {
            try (ZipFile zipFile = new ZipFile(importFile, ZipFile.OPEN_READ)) {
                ZipEntry metaEntry = zipFile.getEntry(ExportConstants.META_FILENAME);
                if (metaEntry == null) {
                    page.setMessage("Cannot find meta file", IMessageProvider.ERROR);
                    return false;
                }
                try (InputStream metaStream = zipFile.getInputStream(metaEntry)) {
                    metaTree = XMLUtils.parseDocument(metaStream);
                } catch (XMLException e) {
                    page.setMessage("Cannot parse meta file: " + e.getMessage(), IMessageProvider.ERROR);
                    return false;
                }
                return true;
            }
        } catch (IOException e) {
            page.setMessage("Cannot open archive '" + importFile.getAbsolutePath() + "': " + e.getMessage(), IMessageProvider.ERROR);
            return false;
        }
    }

    public String getTargetProjectName(String projectName)
    {
        return projectNameMap.get(projectName);
    }

    public void clearProjectNameMap()
    {
        projectNameMap.clear();
    }

    public void addProjectName(String sourceName, String targetName)
    {
        projectNameMap.put(sourceName, targetName);
    }
}

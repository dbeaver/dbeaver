/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.export.project;

import net.sf.jkiss.utils.xml.XMLException;
import net.sf.jkiss.utils.xml.XMLUtils;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.wizard.WizardPage;
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
public class ProjectImportData {

    private File importFile = null;
    private boolean importDriverLibraries = true;
    private Document metaTree;
    private Map<String, String> projectNameMap = new HashMap<String, String>();

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
            ZipFile zipFile = new ZipFile(importFile, ZipFile.OPEN_READ);
            try {
                ZipEntry metaEntry = zipFile.getEntry(ExportConstants.META_FILENAME);
                if (metaEntry == null) {
                    page.setMessage("Cannot find meta file", IMessageProvider.ERROR);
                    return false;
                }
                InputStream metaStream = zipFile.getInputStream(metaEntry);
                try {
                    metaTree = XMLUtils.parseDocument(metaStream);
                } catch (XMLException e) {
                    page.setMessage("Cannot parse meta file: " + e.getMessage(), IMessageProvider.ERROR);
                    return false;
                } finally {
                    metaStream.close();
                }
                return true;
            }
            finally {
                zipFile.close();
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

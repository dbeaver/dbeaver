/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.export.scripts;

import org.eclipse.core.resources.IResource;

import java.io.File;
import java.util.Collection;


class ScriptsExportData {

    private final Collection<IResource> scripts;
    private final boolean overwriteFiles;
    private final File outputFolder;

    ScriptsExportData(Collection<IResource> scripts, boolean overwriteFiles, File outputFolder)
    {
        this.scripts = scripts;
        this.overwriteFiles = overwriteFiles;
        this.outputFolder = outputFolder;
    }

    public Collection<IResource> getScripts()
    {
        return scripts;
    }

    public boolean isOverwriteFiles()
    {
        return overwriteFiles;
    }

    public File getOutputFolder()
    {
        return outputFolder;
    }

}

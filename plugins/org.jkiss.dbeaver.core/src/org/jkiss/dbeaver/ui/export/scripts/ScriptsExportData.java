/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.export.scripts;

import org.eclipse.core.resources.IResource;

import java.io.File;
import java.util.Collection;


class ScriptsExportData {

    private final Collection<IResource> scripts;
    private final File outputFolder;

    public ScriptsExportData(Collection<IResource> scripts, File outputFolder)
    {
        this.scripts = scripts;
        this.outputFolder = outputFolder;
    }

    public Collection<IResource> getScripts()
    {
        return scripts;
    }

    public File getOutputFolder()
    {
        return outputFolder;
    }

}

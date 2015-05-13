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

package org.jkiss.dbeaver.tools.scripts;

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

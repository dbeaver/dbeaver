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
package org.jkiss.dbeaver.registry;

import org.eclipse.core.resources.IFile;

import java.io.File;

/**
 * DataSourceOrigin
 */
class DataSourceOrigin
{
    private final IFile sourceFile;
    private final boolean isDefault;

    public DataSourceOrigin(IFile sourceFile, boolean isDefault) {
        this.sourceFile = sourceFile;
        this.isDefault = isDefault;
    }

    public String getName() {
        return sourceFile.getName();
    }

    public boolean isDefault() {
        return isDefault;
    }

    public IFile getSourceFile() {
        return sourceFile;
    }

    @Override
    public String toString() {
        return sourceFile.getFullPath().toString();
    }
}

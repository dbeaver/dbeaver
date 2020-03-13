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
package org.jkiss.dbeaver.ui.editors;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IPathEditorInput;
import org.eclipse.ui.part.FileEditorInput;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.ui.DBeaverIcons;

/**
 * ProjectFileEditorInput
 */
public class ProjectFileEditorInput extends FileEditorInput implements IFileEditorInput, IPathEditorInput {

    /**
     * Creates an editor input based of the given file resource.
     *
     * @param file the file resource
     */
    public ProjectFileEditorInput(IFile file) {
        super(file);

    }

    public IProject getProject() {
        if (getFile() == null || !getFile().exists()) {
            return null;
        }
        return getFile().getProject();
    }

    @Override
    public ImageDescriptor getImageDescriptor() {
        return DBeaverIcons.getImageDescriptor(DBIcon.TYPE_UNKNOWN);
    }

    @Override
    public <T> T getAdapter(Class<T> adapter) {
        return super.getAdapter(adapter);
    }

}

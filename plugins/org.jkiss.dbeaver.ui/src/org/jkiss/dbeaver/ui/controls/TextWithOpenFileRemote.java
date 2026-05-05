/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.controls;

import org.eclipse.swt.widgets.Composite;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.fs.DBFUtils;

/**
 * TextWithOpenFileRemote.
 *
 * Styles: SWT.SAVE, SWT.OPEN, SWT.SINGLE
 */
public class TextWithOpenFileRemote extends TextWithOpenFile {

    private final DBPProject project;

    public TextWithOpenFileRemote(
        Composite parent,
        String title,
        String[] filterExt,
        int style,
        boolean binary,
        DBPProject project
    ) {
        super(parent, title, filterExt, style, binary, project != null && DBFUtils.supportsMultiFileSystems(project), false);
        this.project = project;
    }

    @Override
    public DBPProject getProject() {
        return project;
    }
}
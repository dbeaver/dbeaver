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
package org.jkiss.dbeaver.ui.editors.sql.dialogs;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbenchPartSite;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;

public abstract class GenerateSQLParametrizedDialog extends GenerateSQLDialog {

    protected GenerateSQLParametrizedDialog(
        IWorkbenchPartSite parentSite,
        DBCExecutionContext executionContext,
        String title,
        @Nullable DBPImage image)
    {
        super(parentSite, executionContext, title, image);
    }

    protected void createControls(Composite parent) {

    }

    protected abstract String[] generateSQLScript();

}
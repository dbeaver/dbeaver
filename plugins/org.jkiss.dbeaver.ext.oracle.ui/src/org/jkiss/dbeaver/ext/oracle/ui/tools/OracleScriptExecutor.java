/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.oracle.ui.tools;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.oracle.model.OracleDataSource;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.tasks.ui.nativetool.NativeToolWizardDialog;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditor;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditorExecutor;

import java.io.File;

public class OracleScriptExecutor implements SQLEditorExecutor {

    @Override
    public void execute(
        @NotNull DBPDataSource dataSource,
        @NotNull SQLEditor editor
    ) throws DBException {
        File sourceFile = editor.getGlobalScriptContext().getSourceFile();
        NativeToolWizardDialog dialog = new NativeToolWizardDialog(
            UIUtils.getActiveWorkbenchWindow(), new OracleScriptExecuteWizard(
            (OracleDataSource) dataSource,
            sourceFile != null ? sourceFile.getAbsolutePath() : null)
        );
        dialog.open();
    }
}

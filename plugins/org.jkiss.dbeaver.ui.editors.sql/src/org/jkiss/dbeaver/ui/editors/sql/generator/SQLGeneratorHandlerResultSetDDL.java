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
package org.jkiss.dbeaver.ui.editors.sql.generator;

import java.util.List;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.sql.generator.SQLGenerator;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.resultset.IResultSetController;
import org.jkiss.dbeaver.ui.controls.resultset.IResultSetProvider;
import org.jkiss.dbeaver.ui.editors.MultiPageAbstractEditor;
import org.jkiss.dbeaver.ui.editors.sql.dialogs.ViewSQLDialog;
import org.jkiss.dbeaver.ui.editors.sql.internal.SQLEditorMessages;
import org.jkiss.utils.CommonUtils;

public class SQLGeneratorHandlerResultSetDDL extends AbstractHandler {

    @Nullable
    @Override
    public Object execute(@NotNull ExecutionEvent event) throws ExecutionException {
        IEditorPart editorPart = HandlerUtil.getActiveEditor(event);
        while (!(editorPart instanceof IResultSetProvider)) {
            if (editorPart instanceof MultiPageAbstractEditor) {
                editorPart = ((MultiPageAbstractEditor) editorPart).getActiveEditor();
            } else {
                return null;
            }
        }
        
        IResultSetController resultSetController = ((IResultSetProvider) editorPart).getResultSetController();
        if (resultSetController != null) {
            SQLGenerator<IResultSetController> generator = new SQLGeneratorDDLFromResultSet();
            generator.initGenerator(List.of(resultSetController));
            
            if (generator != null) {
                IWorkbenchPage activePage = UIUtils.getActiveWorkbenchWindow().getActivePage();
                DBCExecutionContext context = resultSetController.getExecutionContext();
                ViewSQLDialog dialog = new ViewSQLDialog(
                    activePage.getActivePart().getSite(),
                    () -> context,
                    NLS.bind(SQLEditorMessages.sql_generator_title_text, context.getDataSource().getContainer().getName()),
                    null, ""
                ) {
                    @NotNull
                    @Override
                    protected Composite createDialogArea(Composite parent) {
                        UIUtils.runInUI(generator);
                        Object sql = generator.getResult();
                        if (sql != null) {
                            setSQLText(CommonUtils.toString(sql));
                        }
                        return super.createDialogArea(parent);
                    } 
                };
                dialog.open();
            }
        }
        
        return null;
    }
}

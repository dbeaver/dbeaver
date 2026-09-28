/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.data.editors;

import org.eclipse.core.expressions.EvaluationResult;
import org.eclipse.core.expressions.Expression;
import org.eclipse.core.expressions.ExpressionInfo;
import org.eclipse.core.expressions.IEvaluationContext;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.commands.ActionHandler;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.ITextOperationTarget;
import org.eclipse.jface.text.TextViewer;
import org.eclipse.jface.text.TextViewerUndoManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.ISources;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.eclipse.ui.handlers.IHandlerActivation;
import org.eclipse.ui.handlers.IHandlerService;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.ui.ActionUtils;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.StyledTextUtils;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetPreferences;
import org.jkiss.dbeaver.ui.data.IValueController;

/**
* StringInlineEditor.
 * Relies on StyledText. After all it is better.
*/
public class StringInlineEditor extends BaseValueEditor<Control> {

    private static final int MAX_STRING_LENGTH = 0xffff;

    private TextViewer textViewer;

    public StringInlineEditor(IValueController controller) {
        super(controller);
    }

    @Override
    protected Control createControl(Composite editPlaceholder) {
        final boolean inline = valueController.getEditType() == IValueController.EditType.INLINE;
        if (inline) {
            // Native Text only has platform-dependent, single-level undo on Windows.
            textViewer = new TextViewer(editPlaceholder, SWT.SINGLE | SWT.BORDER);
            textViewer.setDocument(new Document());
            TextViewerUndoManager undoManager = new TextViewerUndoManager(ResultSetPreferences.DEFAULT_EDIT_UNDO_LEVEL);
            undoManager.connect(textViewer);
            textViewer.setUndoManager(undoManager);
            final StyledText editor = textViewer.getTextWidget();
            //editor.setTextLimit(MAX_STRING_LENGTH);
            //editor.setFont(UIUtils.getMonospaceFont());
            editor.setEditable(!valueController.isReadOnly());
            editor.addTraverseListener(e -> {
                // TextViewer suppresses Shift+Tab, but a single-line cell editor must allow navigation.
                if (e.detail == SWT.TRAVERSE_TAB_PREVIOUS) {
                    e.doit = true;
                }
            });
            UIUtils.addDefaultEditActionsSupport(valueController.getValueSite(), editor);
            IAction undo = registerTextOperation(IWorkbenchCommandConstants.EDIT_UNDO, ITextOperationTarget.UNDO);
            IAction redo = registerTextOperation(IWorkbenchCommandConstants.EDIT_REDO, ITextOperationTarget.REDO);
            MenuManager menu = new MenuManager();
            menu.setRemoveAllWhenShown(true);
            menu.addMenuListener(manager -> {
                manager.add(undo);
                manager.add(redo);
                manager.add(new Separator());
                StyledTextUtils.fillDefaultStyledTextContextMenu(manager, editor);
            });
            editor.setMenu(menu.createContextMenu(editor));
            editor.addDisposeListener(e -> menu.dispose());
            return editor;
        } else {
            final StyledText editor = new StyledText(editPlaceholder, SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
            //editor.setTextLimit(MAX_STRING_LENGTH);
            editor.setEditable(!valueController.isReadOnly());
            editor.setFont(UIUtils.getMonospaceFont());
            StyledTextUtils.fillDefaultStyledTextContextMenu(editor);
            return editor;
        }
    }

    @NotNull
    private IAction registerTextOperation(@NotNull String commandId, int operation) {
        TextViewer viewer = textViewer;
        StyledText editor = viewer.getTextWidget();
        IAction action = new Action(ActionUtils.findCommandName(commandId)) {
            @Override
            public boolean isEnabled() {
                return !editor.isDisposed() && editor.getEditable() && viewer.canDoOperation(operation);
            }

            @Override
            public void run() {
                if (isEnabled()) {
                    viewer.doOperation(operation);
                }
            }
        };
        action.setActionDefinitionId(commandId);
        IHandlerService handlerService = valueController.getValueSite().getService(IHandlerService.class);
        ActionHandler handler = new ActionHandler(action);
        // Own these commands only while this editor has focus, including when its history is empty.
        // This respects configured shortcuts without falling back to the host SQL editor's history.
        IHandlerActivation activation = handlerService.activateHandler(commandId, handler, new Expression() {
            @NotNull
            @Override
            public EvaluationResult evaluate(@NotNull IEvaluationContext context) {
                return EvaluationResult.valueOf(context.getVariable(ISources.ACTIVE_FOCUS_CONTROL_NAME) == editor);
            }

            @Override
            public void collectExpressionInfo(@NotNull ExpressionInfo info) {
                info.addVariableNameAccess(ISources.ACTIVE_FOCUS_CONTROL_NAME);
            }
        });
        editor.addDisposeListener(e -> {
            handlerService.deactivateHandler(activation);
            handler.dispose();
        });
        return action;
    }

    @Override
    public void primeEditorValue(@Nullable Object value) throws DBException
    {
        final String strValue = valueController.getValueHandler().getValueDisplayString(
            valueController.getValueType(),
            value,
            DBDDisplayFormat.EDIT
        );
        if (textViewer != null) {
            // Loading a cell starts a new editing session; never undo into the previous cell.
            textViewer.setDocument(new Document(strValue));
            textViewer.getTextWidget().selectAll();
        } else if (control instanceof Text text) {
            text.setText(strValue);
            if (valueController.getEditType() == IValueController.EditType.INLINE) {
                text.selectAll();
            }
        } else if (control instanceof StyledText styledText) {
            styledText.setText(strValue);
            if (valueController.getEditType() == IValueController.EditType.INLINE) {
                styledText.selectAll();
            }
        }
    }

    @Override
    public Object extractEditorValue() throws DBCException {
        DBCExecutionContext executionContext = valueController.getExecutionContext();
        if (executionContext == null) {
            return null;
        }
        try (DBCSession session = executionContext.openSession(
            new VoidProgressMonitor(),
            DBCExecutionPurpose.UTIL,
            "Make string value from editor"
        )) {
            String strValue;
            if (control instanceof Text text) {
                strValue = text.getText();
            } else if (control instanceof StyledText styledText){
                strValue = styledText.getText();
            } else {
                return null;
            }
            return valueController.getValueHandler().getValueFromObject(
                session,
                valueController.getValueType(),
                strValue,
                false,
                false);
        }
    }
}

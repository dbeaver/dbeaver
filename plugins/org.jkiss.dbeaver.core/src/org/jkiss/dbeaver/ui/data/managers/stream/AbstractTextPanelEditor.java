/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ui.data.managers.stream;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IContributionManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.text.IUndoManager;
import org.eclipse.jface.text.TextViewer;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.DBPMessageType;
import org.jkiss.dbeaver.model.data.DBDContent;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.impl.StringContentStorage;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.resultset.panel.valueviewer.ValueViewerPanel;
import org.jkiss.dbeaver.ui.data.IStreamValueEditor;
import org.jkiss.dbeaver.ui.data.IValueController;
import org.jkiss.dbeaver.ui.editors.StringEditorInput;
import org.jkiss.dbeaver.ui.editors.SubEditorSite;
import org.jkiss.dbeaver.ui.editors.content.ContentEditorInput;
import org.jkiss.dbeaver.ui.editors.text.BaseTextEditor;
import org.jkiss.dbeaver.utils.RuntimeUtils;

/**
* AbstractTextPanelEditor
*/
public abstract class AbstractTextPanelEditor<EDITOR extends BaseTextEditor> implements IStreamValueEditor<StyledText>, IAdaptable {

    public static final String PREF_TEXT_EDITOR_WORD_WRAP = "content.text.editor.word-wrap";
    public static final String PREF_TEXT_EDITOR_AUTO_FORMAT = "content.text.editor.auto-format";

    private static final Log log = Log.getLog(AbstractTextPanelEditor.class);

    private IValueController valueController;
    private IEditorSite subSite;
    private EDITOR editor;

    @Override
    public StyledText createControl(IValueController valueController) {
        this.valueController = valueController;
        this.subSite = new SubEditorSite(valueController.getValueSite());
        editor = createEditorParty(valueController);
        try {
            editor.init(subSite, StringEditorInput.EMPTY_INPUT);
        } catch (PartInitException e) {
            valueController.showMessage(e.getMessage(), DBPMessageType.ERROR);
            return new StyledText(valueController.getEditPlaceholder(), SWT.NONE);
        }
        editor.createPartControl(valueController.getEditPlaceholder());
        StyledText editorControl = editor.getEditorControl();
        assert editorControl != null;
        initEditorSettings(editorControl);
        editorControl.addDisposeListener(e -> editor.releaseEditorInput());
        return editor.getEditorControl();
    }

    protected abstract EDITOR createEditorParty(IValueController valueController);

    @Override
    public void contributeActions(@NotNull IContributionManager manager, @NotNull final StyledText control) throws DBCException {

    }

    @Override
    public void contributeSettings(@NotNull IContributionManager manager, @NotNull final StyledText editorControl) throws DBCException {
        manager.add(new Separator());
        {
            Action wwAction = new Action("Word Wrap", Action.AS_CHECK_BOX) {
                @Override
                public void run() {
                    boolean newWW = !editorControl.getWordWrap();
                    setChecked(newWW);
                    editorControl.setWordWrap(newWW);
                    ValueViewerPanel.getPanelSettings().put(PREF_TEXT_EDITOR_WORD_WRAP, newWW);
                }
            };
            wwAction.setChecked(editorControl.getWordWrap());
            manager.add(wwAction);
        }

        BaseTextEditor textEditor = getTextEditor();
        if (textEditor != null) {
            final Action afAction = new Action("Auto Format", Action.AS_CHECK_BOX) {
                @Override
                public void run() {
                    boolean newAF = !ValueViewerPanel.getPanelSettings().getBoolean(PREF_TEXT_EDITOR_AUTO_FORMAT);
                    setChecked(newAF);
                    ValueViewerPanel.getPanelSettings().put(PREF_TEXT_EDITOR_AUTO_FORMAT, newAF);
                    applyEditorStyle();
                }
            };
            afAction.setChecked(ValueViewerPanel.getPanelSettings().getBoolean(PREF_TEXT_EDITOR_AUTO_FORMAT));
            manager.add(afAction);
        }
    }

    protected EDITOR getTextEditor() {
        return editor;
    }

    private void initEditorSettings(StyledText control) {
        boolean wwEnabled = ValueViewerPanel.getPanelSettings().getBoolean(PREF_TEXT_EDITOR_WORD_WRAP);
        if (wwEnabled != control.getWordWrap()) {
            control.setWordWrap(wwEnabled);
        }
    }

    private void applyEditorStyle() {
        BaseTextEditor textEditor = getTextEditor();
        if (textEditor != null && ValueViewerPanel.getPanelSettings().getBoolean(PREF_TEXT_EDITOR_AUTO_FORMAT)) {
            try {
                if (textEditor.getViewer().canDoOperation(ISourceViewer.FORMAT)) {
                    textEditor.getViewer().doOperation(ISourceViewer.FORMAT);
                }
            } catch (Exception e) {
                log.debug("Error formatting text", e);
            }
        }
    }

    @Override
    public <T> T getAdapter(Class<T> adapter) {
        BaseTextEditor textEditor = getTextEditor();
        if (textEditor != null) {
            if (adapter.isAssignableFrom(textEditor.getClass())) {
                return adapter.cast(textEditor);
            }
            if (adapter == IUndoManager.class) {
                TextViewer textViewer = textEditor.getTextViewer();
                if (textViewer != null && textViewer.getUndoManager() != null) {
                    return adapter.cast(textViewer.getUndoManager());
                }
            }
            return textEditor.getAdapter(adapter);
        }
        return null;
    }

    @Override
    public void primeEditorValue(@NotNull DBRProgressMonitor monitor, @NotNull StyledText control, @NotNull DBDContent value) throws DBException
    {
        monitor.beginTask("Load text", 1);
        try {
            monitor.subTask("Loading text value");
            final IEditorInput sqlInput = new ContentEditorInput(valueController, null, null, monitor);
            UIUtils.syncExec(() -> {
                editor.setInput(sqlInput);
                applyEditorStyle();
            });
        } catch (Exception e) {
            throw new DBException("Error loading text value", e);
        } finally {
            monitor.done();
        }
    }

    @Override
    public void extractEditorValue(@NotNull DBRProgressMonitor monitor, @NotNull StyledText control, @NotNull DBDContent value) throws DBException
    {
        if (valueController.getValueType().getDataKind() == DBPDataKind.STRING) {
            value.updateContents(
                    monitor,
                    new StringContentStorage(control.getText()));
        } else {
            monitor.beginTask("Extract text", 1);
            try {
                monitor.subTask("Extracting text from editor");
                editor.doSave(RuntimeUtils.getNestedMonitor(monitor));
                final IEditorInput editorInput = editor.getEditorInput();
                if (editorInput instanceof ContentEditorInput) {
                    final ContentEditorInput contentEditorInput = (ContentEditorInput) editorInput;
                    contentEditorInput.updateContentFromFile(monitor, value);
                }
            } catch (Exception e) {
                throw new DBException("Error extracting text from editor", e);
            } finally {
                monitor.done();
            }
        }
    }

}

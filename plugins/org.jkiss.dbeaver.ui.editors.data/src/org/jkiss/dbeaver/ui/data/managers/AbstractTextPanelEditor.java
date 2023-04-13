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
package org.jkiss.dbeaver.ui.data.managers;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IContributionManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.text.IUndoManager;
import org.eclipse.jface.text.TextViewer;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ST;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.texteditor.ITextEditorActionConstants;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPMessageType;
import org.jkiss.dbeaver.model.data.DBDContent;
import org.jkiss.dbeaver.model.data.storage.StringContentStorage;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.StyledTextUtils;
import org.jkiss.dbeaver.ui.controls.resultset.internal.ResultSetMessages;
import org.jkiss.dbeaver.ui.data.IStreamValueEditorPersistent;
import org.jkiss.dbeaver.ui.data.IValueController;
import org.jkiss.dbeaver.ui.dialogs.BaseDialog;
import org.jkiss.dbeaver.ui.editors.StringEditorInput;
import org.jkiss.dbeaver.ui.editors.SubEditorSite;
import org.jkiss.dbeaver.ui.editors.TextEditorUtils;
import org.jkiss.dbeaver.ui.editors.content.ContentEditorInput;
import org.jkiss.dbeaver.ui.editors.data.internal.DataEditorsActivator;
import org.jkiss.dbeaver.ui.editors.text.BaseTextEditor;
import org.jkiss.dbeaver.utils.RuntimeUtils;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
* AbstractTextPanelEditor
*/
public abstract class AbstractTextPanelEditor<EDITOR extends BaseTextEditor>
    implements IStreamValueEditorPersistent<StyledText>, IAdaptable {

    private static final String PREF_TEXT_EDITOR_WORD_WRAP = "content.text.editor.word-wrap";
    private static final String PREF_TEXT_EDITOR_AUTO_FORMAT = "content.text.editor.auto-format";
    private static final String PREF_TEXT_EDITOR_ENCODING = "content.text.editor.encoding";

    private static final Log log = Log.getLog(AbstractTextPanelEditor.class);
    public static final int LONG_CONTENT_LENGTH = 10000;

    private IValueController valueController;
    private IEditorSite subSite;
    private EDITOR editor;
    private Path tempFile;

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

        editor.addContextMenuContributor(manager -> contributeTextEditorActions(manager, editorControl));

        return editorControl;
    }

    protected abstract EDITOR createEditorParty(IValueController valueController);

    protected void contributeTextEditorActions(@NotNull IContributionManager manager, @NotNull final StyledText control) {
        manager.removeAll();
        //StyledTextUtils.fillDefaultStyledTextContextMenu(manager, control);
        final Point selectionRange = control.getSelectionRange();

        manager.add(new StyledTextUtils.StyledTextAction(IWorkbenchCommandConstants.EDIT_COPY, selectionRange.y > 0, control, ST.COPY));
        manager.add(new StyledTextUtils.StyledTextAction(IWorkbenchCommandConstants.EDIT_PASTE, control.getEditable(), control, ST.PASTE));
        manager.add(new StyledTextUtils.StyledTextAction(IWorkbenchCommandConstants.EDIT_CUT, selectionRange.y > 0, control, ST.CUT));
        manager.add(new StyledTextUtils.StyledTextAction(IWorkbenchCommandConstants.EDIT_SELECT_ALL, true, control, ST.SELECT_ALL));

        manager.add(new AutoFormatAction());
        manager.add(new WordWrapAction(control));

        manager.add(new Separator());
        manager.add(TextEditorUtils.createFindReplaceAction(editor.getSite().getShell(), editor.getViewer().getFindReplaceTarget()));

        IAction preferencesAction = editor.getAction(ITextEditorActionConstants.CONTEXT_PREFERENCES);
        if (preferencesAction != null) {
            manager.add(new Separator());
            manager.add(preferencesAction);
        }
    }

    @Override
    public void contributeActions(@NotNull IContributionManager manager, @NotNull final StyledText control) {

    }

    @Override
    public void contributeSettings(@NotNull IContributionManager manager, @NotNull final StyledText editorControl) {
        manager.add(new Separator());
        {
            Action wwAction = new Action(ResultSetMessages.panel_editor_text_word_wrap_name, Action.AS_CHECK_BOX) {
                @Override
                public void run() {
                    boolean newWW = !editorControl.getWordWrap();
                    setChecked(newWW);
                    editorControl.setWordWrap(newWW);
                    getPanelSettings().put(PREF_TEXT_EDITOR_WORD_WRAP, newWW);
                }
            };
            wwAction.setChecked(editorControl.getWordWrap());
            manager.add(wwAction);
        }

        BaseTextEditor textEditor = getTextEditor();
        if (textEditor != null) {
            final Action afAction = new AutoFormatAction();
            afAction.setChecked(getPanelSettings().getBoolean(PREF_TEXT_EDITOR_AUTO_FORMAT));
            manager.add(afAction);
        }

        if (textEditor != null) {
            manager.add(new Action(ResultSetMessages.panel_editor_text_encoding_name) {
                @Override
                public void run() {
                    final ChangeEncodingDialog dialog = new ChangeEncodingDialog(getPanelSettings().get(PREF_TEXT_EDITOR_ENCODING));
                    if (dialog.open() != IDialogConstants.OK_ID) {
                        return;
                    }
                    getPanelSettings().put(PREF_TEXT_EDITOR_ENCODING, dialog.getEncoding());
                    final EDITOR editor = getTextEditor();
                    if (editor != null) {
                        final TextViewer viewer = editor.getTextViewer();
                        if (viewer != null) {
                            final StyledText control = viewer.getTextWidget();
                            if (control != null && !control.isDisposed()) {
                                try {
                                    primeEditorValue(new VoidProgressMonitor(), control, null);
                                } catch (DBException e) {
                                    log.error("Can't refresh editor", e);
                                }
                            }
                        }
                    }
                }
            });
        }
    }

    @Override
    public void disposeEditor() {
        if (editor != null) {
            editor.dispose();
            editor = null;
        }
        if (tempFile != null) {
            try {
                Files.deleteIfExists(tempFile);
            } catch (IOException e) {
                log.warn(e);
            }
        }
    }

    protected EDITOR getTextEditor() {
        return editor;
    }

    private void initEditorSettings(StyledText control) {
        boolean wwEnabled = getPanelSettings().getBoolean(PREF_TEXT_EDITOR_WORD_WRAP);
        if (wwEnabled != control.getWordWrap()) {
            control.setWordWrap(wwEnabled);
        }
    }

    private void applyEditorStyle() {
        BaseTextEditor textEditor = getTextEditor();
        if (textEditor != null && getPanelSettings().getBoolean(PREF_TEXT_EDITOR_AUTO_FORMAT)) {
            TextViewer textViewer = textEditor.getTextViewer();
            if (textViewer != null) {
                StyledText textWidget = textViewer.getTextWidget();
                if (textWidget == null || textWidget.isDisposed()) {
                    return;
                }
                textWidget.setRedraw(false);

                boolean oldEditable = textViewer.isEditable();
                if (!oldEditable) {
                    textViewer.setEditable(true);
                }
                try {
                    if (textViewer.canDoOperation(ISourceViewer.FORMAT)) {
                        textViewer.doOperation(ISourceViewer.FORMAT);
                    }
                } catch (Exception e) {
                    log.debug("Error formatting text", e);
                } finally {
                    if (!oldEditable) {
                        textViewer.setEditable(false);
                    }
                    textWidget.setRedraw(true);
                }
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
    public void primeEditorValue(@NotNull DBRProgressMonitor monitor, @NotNull StyledText control, @Nullable DBDContent value) throws DBException
    {
        try {
            // Load contents in two steps (empty + real in async mode). Workaround for some strange bug in StyledText in E4.13 (#6701)
            final TextViewer textViewer = editor.getTextViewer();
            final String encoding = getPanelSettings().get(PREF_TEXT_EDITOR_ENCODING);
            final ContentEditorInput textInput = new ContentEditorInput(valueController, null, null, encoding, monitor);
            boolean longContent = textInput.getContentLength() > LONG_CONTENT_LENGTH;
            if (longContent) {
                UIUtils.asyncExec(() -> {
                    editor.setInput(new StringEditorInput("Empty", "", true, StandardCharsets.UTF_8.name()));
                });
            }
            UIUtils.asyncExec(() -> {

                if (textViewer != null && editor != null) {
                    StyledText textWidget = textViewer.getTextWidget();
                    if (textWidget != null && longContent) {
                        GC gc = new GC(textWidget);
                        try {
                            UIUtils.drawMessageOverControl(textWidget, gc, NLS.bind(ResultSetMessages.panel_editor_text_loading_placeholder_label, textInput.getContentLength()), 0);
                            editor.setInput(textInput);
                        } finally {
                            gc.dispose();
                        }
                    } else {
                        editor.setInput(textInput);
                    }
                    applyEditorStyle();
                }
            });
        } catch (Exception e) {
            throw new DBException("Error loading text value", e);
        } finally {
            monitor.done();
        }
    }

    @Nullable
    @Override
    public Path getExternalFilePath(@NotNull StyledText control) {
        try {
            if (tempFile != null) {
                Files.deleteIfExists(tempFile);
            }
            this.tempFile = Files.createTempFile(DBWorkbench.getPlatform().getTempFolder(new VoidProgressMonitor(), getFileFolderName()), "file", getFileExtension());
            tempFile.toFile().deleteOnExit();
            Files.writeString(tempFile, control.getText());
            return tempFile;
        } catch (IOException e) {
            log.error(e);
            return null;
        }
    }

    protected abstract String getFileFolderName();

    protected abstract String getFileExtension();

    @Override
    public void extractEditorValue(@NotNull DBRProgressMonitor monitor, @NotNull StyledText control, @NotNull DBDContent value) throws DBException
    {
        if (valueController.getValue() instanceof DBDContent) {
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
        } else {
            value.updateContents(
                monitor,
                new StringContentStorage(control.getText()));
        }
    }

    private static IDialogSettings viewerSettings;

    public static IDialogSettings getPanelSettings() {
        if (viewerSettings == null) {
            viewerSettings = UIUtils.getSettingsSection(
                DataEditorsActivator.getDefault().getDialogSettings(),
                AbstractTextPanelEditor.class.getSimpleName());
        }
        return viewerSettings;
    }

    private static class WordWrapAction extends StyledTextUtils.StyledTextActionEx {

        private final StyledText text;
        WordWrapAction(StyledText text) {
            super(ITextEditorActionDefinitionIds.WORD_WRAP, Action.AS_CHECK_BOX);
            this.text = text;
        }

        @Override
        public boolean isChecked() {
            return text.getWordWrap();
        }

        @Override
        public void run() {
            text.setWordWrap(!text.getWordWrap());
        }
    }

    private class AutoFormatAction extends Action {
        AutoFormatAction() {
            super(ResultSetMessages.panel_editor_text_auto_format_name, Action.AS_CHECK_BOX);
        }

        @Override
        public boolean isChecked() {
            return getPanelSettings().getBoolean(PREF_TEXT_EDITOR_AUTO_FORMAT);
        }

        @Override
        public void run() {
            boolean newAF = !getPanelSettings().getBoolean(PREF_TEXT_EDITOR_AUTO_FORMAT);
            //setChecked(newAF);
            getPanelSettings().put(PREF_TEXT_EDITOR_AUTO_FORMAT, newAF);
            applyEditorStyle();
        }
    }

    private static class ChangeEncodingDialog extends BaseDialog {
        private String encoding;

        public ChangeEncodingDialog(@NotNull String defaultEncoding) {
            super(UIUtils.getActiveShell(), ResultSetMessages.panel_editor_text_encoding_title, null);
            this.encoding = defaultEncoding;
            this.setShellStyle(SWT.DIALOG_TRIM);
        }

        @Override
        protected Composite createDialogArea(Composite parent) {
            final Composite composite = super.createDialogArea(parent);

            {
                final Composite innerComposite = UIUtils.createComposite(composite, 1);
                innerComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

                final Combo encodingCombo = UIUtils.createEncodingCombo(innerComposite, encoding);
                encodingCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
                encodingCombo.addModifyListener(event -> {
                    encoding = encodingCombo.getText();
                    updateCompletion();
                });
            }

            return composite;
        }

        @Override
        protected void createButtonsForButtonBar(Composite parent) {
            super.createButtonsForButtonBar(parent);
            updateCompletion();
        }

        private void updateCompletion() {
            final Button button = getButton(IDialogConstants.OK_ID);
            try {
                Charset.forName(encoding);
                button.setEnabled(true);
            } catch (Exception ignored) {
                button.setEnabled(false);
            }
        }

        @NotNull
        public String getEncoding() {
            return encoding;
        }
    }
}

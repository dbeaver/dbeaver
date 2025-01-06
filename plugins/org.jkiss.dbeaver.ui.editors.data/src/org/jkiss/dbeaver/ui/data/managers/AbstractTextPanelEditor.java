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
package org.jkiss.dbeaver.ui.data.managers;

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
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
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
import org.jkiss.dbeaver.model.DBPAdaptable;
import org.jkiss.dbeaver.model.DBPMessageType;
import org.jkiss.dbeaver.model.data.DBDContent;
import org.jkiss.dbeaver.model.data.DBDContentStorage;
import org.jkiss.dbeaver.model.data.storage.StringContentStorage;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.ActionUtils;
import org.jkiss.dbeaver.ui.UIStyles;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.StyledTextUtils;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetPreferences;
import org.jkiss.dbeaver.ui.controls.resultset.internal.ResultSetMessages;
import org.jkiss.dbeaver.ui.data.IStreamValueEditorPersistent;
import org.jkiss.dbeaver.ui.data.IValueController;
import org.jkiss.dbeaver.ui.dialogs.BaseDialog;
import org.jkiss.dbeaver.ui.editors.StringEditorInput;
import org.jkiss.dbeaver.ui.editors.SubEditorSite;
import org.jkiss.dbeaver.ui.editors.content.ContentEditorInput;
import org.jkiss.dbeaver.ui.editors.data.internal.DataEditorsActivator;
import org.jkiss.dbeaver.ui.editors.data.preferences.PrefPageResultSetEditors;
import org.jkiss.dbeaver.ui.editors.text.BaseTextEditor;
import org.jkiss.dbeaver.utils.RuntimeUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

/**
* AbstractTextPanelEditor
*/
public abstract class AbstractTextPanelEditor<EDITOR extends BaseTextEditor>
    implements IStreamValueEditorPersistent<StyledText>, DBPAdaptable {

    private static final String PREF_TEXT_EDITOR_WORD_WRAP = "content.text.editor.word-wrap";
    private static final String PREF_TEXT_EDITOR_AUTO_FORMAT = "content.text.editor.auto-format";
    private static final String PREF_TEXT_EDITOR_ENCODING = "content.text.editor.encoding";
    private static final String PREF_TEXT_EDITOR_MINIFY = "content.text.editor.minify";

    private static final Log log = Log.getLog(AbstractTextPanelEditor.class);
    

    private IValueController valueController;
    private IEditorSite subSite;
    private EDITOR editor;
    private Path tempFile;
    private MessageBar messageBar;

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
        GridLayout lt = new GridLayout(1, false);
        lt.marginWidth = 0;
        lt.marginHeight = 0;
        valueController.getEditPlaceholder().setLayout(lt);

        Composite cmpsBase = new Composite(valueController.getEditPlaceholder(), SWT.NONE);
        cmpsBase.setLayout(lt);
        cmpsBase.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));

        Composite cmpsInternalBase = new Composite(cmpsBase, SWT.NONE);
        cmpsInternalBase.setLayout(lt);
        cmpsInternalBase.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));

        editor.createPartControl(cmpsInternalBase);
        reasignLayout(cmpsInternalBase);

        StyledText editorControl = editor.getEditorControl();
        assert editorControl != null;
        initEditorSettings(editorControl);
        editor.addContextMenuContributor(manager -> contributeTextEditorActions(manager, editorControl));
        messageBar = new MessageBar(cmpsBase);
        messageBar.hideMessage();
        return editorControl;
    }

    private void reasignLayout(Composite cmpsInternalBase) {
        Control[] children = cmpsInternalBase.getChildren();
        Stream.of(children).forEach(c -> c.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1)));
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
        manager.add(ActionUtils.makeCommandContribution(editor.getSite(), IWorkbenchCommandConstants.EDIT_FIND_AND_REPLACE));

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
            if (supportMinify()) {
                final Action msAction = new SaveMinifyValue();
                msAction.setChecked(getPanelSettings().getBoolean(PREF_TEXT_EDITOR_MINIFY));
                manager.add(msAction);
            }
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
            //control.setWordWrap(wwEnabled);
        }
    }

    private void applyEditorStyle() {
        if (editor == null) {
            return;
        }
        StyledText textWidget = editor.getEditorControl();
        if (textWidget == null || textWidget.isDisposed()) {
            return;
        }
        if (getPanelSettings().getBoolean(PREF_TEXT_EDITOR_AUTO_FORMAT)) {
            TextViewer textViewer = editor.getTextViewer();
            if (textViewer != null) {
                boolean oldEditable = textViewer.isEditable();
                try {
                    if (!oldEditable) {
                        textViewer.setEditable(true);
                    }
                    try {
                        if (textViewer.canDoOperation(ISourceViewer.FORMAT)) {
                            textViewer.doOperation(ISourceViewer.FORMAT);
                        }
                    } catch (Exception e) {
                        log.debug("Error formatting text: " + e.getMessage());
                    }
                } finally {
                    if (!oldEditable) {
                        textViewer.setEditable(false);
                    }
                }
            }
        }
        if (getPanelSettings().getBoolean(PREF_TEXT_EDITOR_WORD_WRAP)) {
            // It must be execute in async mode. Otherwise StyledText goes mad and freezes UI
            UIUtils.asyncExec(() -> {
                if (!textWidget.isDisposed()) {
                    textWidget.setWordWrap(true);
                }
            });
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
    public void primeEditorValue(
        @NotNull DBRProgressMonitor monitor, 
        @NotNull StyledText control, 
        @Nullable DBDContent value)
            throws DBException {
        if (value == null) {
            log.error("Content value (LOB) is null");
            return;
        }
        if (editor == null) {
            log.error("Editor is null or undefined");
            return;
        }
        StyledText editorControl = editor.getEditorControl();
        if (editorControl == null) {
            return;
        }
        if (valueController.isReadOnly()) {
            editorControl.setBackground(UIStyles.getDefaultWidgetBackground());
        }
        try {

            editorControl.setRedraw(false);

            resetEditorInput();
            final DBPPreferenceStore store = valueController.getExecutionContext() != null
                ? valueController.getExecutionContext().getDataSource().getContainer().getPreferenceStore()
                : DBWorkbench.getPlatform().getPreferenceStore();
            final int maxContentLength = store.getInt(ResultSetPreferences.RS_EDIT_MAX_TEXT_SIZE) * 1000;
            if (value.getContentLength() > maxContentLength) {
                showLimitedContent(editorControl, value, maxContentLength);
            } else {
                showRegularContent(editorControl, monitor);
            }
        } catch (Exception e) {
            throw new DBException("Error loading text value", e);
        } finally {
            monitor.done();

            editorControl.setRedraw(true);
        }
    }

    private void resetEditorInput() {
        // Load contents in two steps (empty + real in async mode). Workaround for some
        // strange bug in StyledText in E4.13 (#6701)
        UIUtils.syncExec(() -> {
            if (editor != null) {
                messageBar.hideMessage();
                editor.setInput(StringEditorInput.EMPTY_INPUT);
            }
        });
    }

    private void showRegularContent(StyledText editorControl, @NotNull DBRProgressMonitor monitor) throws DBException {
        String encoding = getPanelSettings().get(PREF_TEXT_EDITOR_ENCODING);
        if (encoding == null) {
            encoding = StandardCharsets.UTF_8.name();
        }
        final ContentEditorInput textInput = new ContentEditorInput(valueController, null, null, encoding, monitor);
        final TextViewer textViewer = editor.getTextViewer();
        if (textViewer != null) {
            long contentLength = textInput.getContentLength();

            StyledText textWidget = textViewer.getTextWidget();
            if (textWidget != null) {
                if (contentLength > 100000) {
                    GC gc = new GC(textWidget);
                    try {
                        UIUtils.drawMessageOverControl(textWidget, gc,
                            NLS.bind(ResultSetMessages.panel_editor_text_loading_placeholder_label, contentLength), 0);
                    } finally {
                        gc.dispose();
                    }
                }
                editorControl.setWordWrap(false);
                editor.setInput(textInput);

                messageBar.hideMessage();
            } else {
                editor.setInput(textInput);
            }
            applyEditorStyle();
        }
    }

    private void showLimitedContent(StyledText editorControl, @NotNull DBDContent value, int lengthInBytes) throws DBCException, IOException {
        DBDContentStorage contents = value.getContents(new VoidProgressMonitor());
        try (final InputStream stream = contents.getContentStream()) {
            byte[] displayingContentBytes = stream.readNBytes(lengthInBytes);
            final String content = new String(displayingContentBytes);
            if (editor != null) {
                editorControl.setWordWrap(false);
                editor.setInput(new StringEditorInput("Limited Content ", content, true, StandardCharsets.UTF_8.name()));
                messageBar.showMessage(NLS.bind(ResultSetMessages.panel_editor_text_content_limitation_lbl, lengthInBytes / 1000));
            }
            applyEditorStyle();
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
            String text = control.getText();
            if (getPanelSettings().getBoolean(PREF_TEXT_EDITOR_MINIFY)) {
                text = minify(text);
            }
            value.updateContents(
                monitor,
                new StringContentStorage(text));
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

    private class SaveMinifyValue extends Action {
        SaveMinifyValue() {
            super(ResultSetMessages.panel_editor_text_minify_name, Action.AS_CHECK_BOX);
        }

        @Override
        public boolean isChecked() {
            return getPanelSettings().getBoolean(PREF_TEXT_EDITOR_MINIFY);
        }

        @Override
        public void run() {
            boolean newMF = !getPanelSettings().getBoolean(PREF_TEXT_EDITOR_MINIFY);
            getPanelSettings().put(PREF_TEXT_EDITOR_MINIFY, newMF);
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

    private static class MessageBar extends Composite {
        private final Link message;

        public MessageBar(@NotNull Composite parent) {
            super(parent, SWT.NONE);

            final GridLayout layout = new GridLayout();
            layout.marginWidth = 0;
            layout.marginHeight = 0;

            setLayout(layout);
            setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

            UIUtils.createLabelSeparator(this, SWT.HORIZONTAL);

            message = UIUtils.createInfoLink(
                this,
                "",
                () -> UIUtils.showPreferencesFor(getShell(), null, PrefPageResultSetEditors.PAGE_ID)
            );
        }

        public void showMessage(@NotNull String text) {
            message.setText(text);
            UIUtils.setControlVisible(this, true);
            getParent().layout(true, true);
        }

        public void hideMessage() {
            UIUtils.setControlVisible(this, false);
            getParent().layout(true, true);
        }
    }

    public boolean supportMinify() {
        return false;
    }

    public String minify(String value) {
        return value;
    }
}

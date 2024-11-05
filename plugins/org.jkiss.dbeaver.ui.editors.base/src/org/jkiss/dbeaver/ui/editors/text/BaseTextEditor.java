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
package org.jkiss.dbeaver.ui.editors.text;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.GroupMarker;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IUndoManager;
import org.eclipse.jface.text.TextViewer;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ST;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.texteditor.AbstractDecoratedTextEditor;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditorActionConstants;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.ICommentsSupport;
import org.jkiss.dbeaver.ui.ISingleControlEditor;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.DialogUtils;
import org.jkiss.dbeaver.ui.editors.*;
import org.jkiss.dbeaver.ui.editors.internal.EditorsMessages;
import org.jkiss.dbeaver.utils.ContentUtils;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * Abstract text editor.
 * Contains some common dbeaver text editor adaptions.
 */
public abstract class BaseTextEditor extends AbstractDecoratedTextEditor implements ISingleControlEditor {

    public static final String TEXT_EDITOR_CONTEXT = "org.eclipse.ui.textEditorScope";

    public static final String GROUP_SQL_PREFERENCES = "sql.preferences";
    public static final String GROUP_SQL_ADDITIONS = "sql.additions";
    public static final String GROUP_SQL_EXTRAS = "sql.extras";

    private final List<IActionContributor> actionContributors = new ArrayList<>();

    public void addContextMenuContributor(IActionContributor contributor) {
        actionContributors.add(contributor);
    }

    public static BaseTextEditor getTextEditor(IEditorPart editor)
    {
        if (editor == null) {
            return null;
        }
        if (editor instanceof BaseTextEditor) {
            return (BaseTextEditor) editor;
        }
        return editor.getAdapter(BaseTextEditor.class);
    }

//    protected ScriptPositionColumn getScriptColumn()
//    {
//        return fScriptColumn;
//    }


    @Override
    protected void doSetInput(IEditorInput input) throws CoreException {
        if (input != getEditorInput()) {
            releaseEditorInput();
        }
        super.doSetInput(input);
    }

    @Override
    public void dispose()
    {
        releaseEditorInput();
//        fLineColumn = null;
        super.dispose();
    }

    public void releaseEditorInput() {
        IEditorInput editorInput = getEditorInput();
        if (editorInput instanceof IStatefulEditorInput) {
            ((IStatefulEditorInput) editorInput).release();
        }
    }

    @Nullable
    public IDocument getDocument()
    {
        IDocumentProvider provider = getDocumentProvider();
        if (provider == null) {
            return null;
        }
        return provider.getDocument(getEditorInput());
    }

    @Nullable
    @Override
    public StyledText getEditorControl() {
        final TextViewer textViewer = getTextViewer();
        return textViewer == null ? null : textViewer.getTextWidget();
    }

    @Override
    public void createPartControl(Composite parent)
    {
        //setPreferenceStore(new PreferenceStoreDelegate(DBWorkbench.getPlatform().getPreferenceStore()));

        super.createPartControl(parent);

        if (getSite() instanceof SubEditorSite) {
            // SWT.DEL shortcut is disabled in AbstractTextEditor.createNavigationActions
            // Dunno why (there is a weird explanations about bug closed in 2004)
            // but it blocks DEL button in nested editors
            getTextViewer().getTextWidget().setKeyBinding(SWT.DEL, ST.DELETE_NEXT);

            // Disable parent text editor key-bindings
            // This works when text editor is embedded in another text editor (e.g. SQL editor)
            // Commented because in fact this doesn't work. Owner editor still hooks/suppresses all extra commands
            //UIUtils.enableHostEditorKeyBindingsSupport(((SubEditorSite) getSite()).getParentSite(), getTextViewer().getTextWidget());
        }
    }

    @Override
    protected void editorContextMenuAboutToShow(IMenuManager menu)
    {
        //super.editorContextMenuAboutToShow(menu);

        menu.add(new GroupMarker(GROUP_SQL_ADDITIONS));
        menu.add(new GroupMarker(GROUP_SQL_EXTRAS));
        menu.add(new Separator());
        menu.add(new Separator(ITextEditorActionConstants.GROUP_COPY));
        menu.add(new Separator(ITextEditorActionConstants.GROUP_PRINT));
        menu.add(new Separator(ITextEditorActionConstants.GROUP_EDIT));
        menu.add(new Separator(ITextEditorActionConstants.GROUP_FIND));
        menu.add(new Separator(IWorkbenchActionConstants.GROUP_ADD));
        menu.add(new Separator(ITextEditorActionConstants.GROUP_UNDO));
        menu.add(new GroupMarker(ITextEditorActionConstants.GROUP_SAVE));
        menu.add(new Separator(ITextEditorActionConstants.GROUP_REST));
        menu.add(new Separator());
        menu.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
        menu.add(new Separator());
        menu.add(new GroupMarker(GROUP_SQL_PREFERENCES));

        if (isEditable()) {
            addAction(menu, ITextEditorActionConstants.GROUP_UNDO, ITextEditorActionConstants.UNDO);
            addAction(menu, ITextEditorActionConstants.GROUP_SAVE, ITextEditorActionConstants.SAVE);
            addAction(menu, ITextEditorActionConstants.GROUP_COPY, ITextEditorActionConstants.CUT);
            addAction(menu, ITextEditorActionConstants.GROUP_COPY, ITextEditorActionConstants.COPY);
            addAction(menu, ITextEditorActionConstants.GROUP_COPY, ITextEditorActionConstants.PASTE);
            IAction action= getAction(ITextEditorActionConstants.QUICK_ASSIST);
            if (action != null && action.isEnabled()) {
                addAction(menu, ITextEditorActionConstants.GROUP_EDIT, ITextEditorActionConstants.QUICK_ASSIST);
            }
        } else {
            addAction(menu, ITextEditorActionConstants.GROUP_COPY, ITextEditorActionConstants.COPY);
        }

        IAction preferencesAction = getAction(ITextEditorActionConstants.CONTEXT_PREFERENCES);
        if (preferencesAction != null) {
            menu.appendToGroup(GROUP_SQL_PREFERENCES, preferencesAction);
        }

        for (IActionContributor ac : actionContributors) {
            ac.contributeActions(menu);
        }
    }

    @Nullable
    public TextViewer getTextViewer()
    {
        return (TextViewer) getSourceViewer();
    }

    @Nullable
    public SourceViewer getViewer()
    {
        return (SourceViewer) super.getSourceViewer();
    }

    public void enableUndoManager(boolean enable)
    {
        TextViewer textViewer = getTextViewer();
        final IUndoManager undoManager = textViewer.getUndoManager();
        if (undoManager != null) {
            if (!enable) {
                undoManager.disconnect();
            } else {
                undoManager.connect(textViewer);
            }
        }
    }

    @Nullable
    public ICommentsSupport getCommentsSupport()
    {
        return null;
    }

    protected boolean isReadOnly()
    {
        return false;
    }

    public void loadFromExternalFile() {
        final File[] loadFile = DialogUtils.openFileList(getSite().getShell(), EditorsMessages.file_dialog_select_files, new String[]{"*.sql", "*.txt", "*", "*.*"});
        if (loadFile == null) {
            return;
        }

        StringBuilder newContent = new StringBuilder();
        for (File file : loadFile) {
            try {
                newContent.append(Files.readString(file.toPath(), GeneralUtils.DEFAULT_FILE_CHARSET));
                newContent.append(System.lineSeparator());
            } catch (IOException e) {
                DBWorkbench.getPlatformUI().showError(
                        EditorsMessages.file_dialog_cannot_load_file,
                        EditorsMessages.file_dialog_cannot_load_file + " '" + file.getAbsolutePath() + "' - " + e.getMessage());
            }
        }
        if (!CommonUtils.isEmpty(newContent)) {
            IDocument document = getDocument();
            if (document != null) {
                document.set(newContent.toString());
            }
        }

    }

    public void saveToExternalFile() {
        saveToExternalFile(null);
    }

    public void saveToExternalFile(@Nullable String currentDirectory) {
        IEditorInput editorInput = getEditorInput();
        File curFile = EditorUtils.getLocalFileFromInput(editorInput);
        String fileName = curFile == null ? null : curFile.getName();

        if (CommonUtils.isNotEmpty(currentDirectory)) {
            DialogUtils.setCurDialogFolder(currentDirectory);
        }

        final IDocument document = getDocument();
        final File saveFile = DialogUtils.selectFileForSave(getSite().getShell(), EditorsMessages.file_dialog_save_as_file, new String[]{"*.sql", "*.txt", "*", "*.*"}, fileName);
        if (document == null || saveFile == null) {
            return;
        }

        try {
            UIUtils.runInProgressService(monitor -> {
                try {
                    StringReader cr = new StringReader(document.get());
                    ContentUtils.saveContentToFile(cr, saveFile, ResourcesPlugin.getEncoding(), monitor);
                } catch (Exception e) {
                    throw new InvocationTargetException(e);
                }
            });
        } catch (InterruptedException e) {
            // do nothing
        } catch (InvocationTargetException e) {
            DBWorkbench.getPlatformUI().showError(EditorsMessages.file_dialog_save_failed, null, e.getTargetException());
        }

        afterSaveToFile(saveFile);
    }

    protected void afterSaveToFile(File saveFile) {

    }

    @Nullable
    public int[] getCurrentLines()
    {
        return null;
    }

    protected boolean isNonPersistentEditor() {
        return getEditorInput() instanceof INonPersistentEditorInput;
    }

}

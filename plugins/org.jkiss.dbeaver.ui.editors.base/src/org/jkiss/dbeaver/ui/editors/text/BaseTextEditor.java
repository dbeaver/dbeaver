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
package org.jkiss.dbeaver.ui.editors.text;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.GroupMarker;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.text.Document;
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
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.ICommentsSupport;
import org.jkiss.dbeaver.ui.ISingleControlEditor;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.DialogUtils;
import org.jkiss.dbeaver.ui.editors.EditorUtils;
import org.jkiss.dbeaver.ui.editors.INonPersistentEditorInput;
import org.jkiss.dbeaver.ui.editors.IStatefulEditorInput;
import org.jkiss.dbeaver.ui.editors.SubEditorSite;
import org.jkiss.dbeaver.utils.ContentUtils;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.IOUtils;

import java.io.*;
import java.lang.reflect.InvocationTargetException;

/**
 * Abstract text editor.
 * Contains some common dbeaver text editor adaptions.
 */
public abstract class BaseTextEditor extends AbstractDecoratedTextEditor implements ISingleControlEditor {

    public static final String TEXT_EDITOR_CONTEXT = "org.eclipse.ui.textEditorScope";

    public static final String GROUP_SQL_PREFERENCES = "sql.preferences";
    public static final String GROUP_SQL_ADDITIONS = "sql.additions";
    public static final String GROUP_SQL_EXTRAS = "sql.extras";

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
    public Document getDocument()
    {
        IDocumentProvider provider = getDocumentProvider();
        return provider == null ? null : (Document)provider.getDocument(getEditorInput());
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

        menu.add(new Separator(ITextEditorActionConstants.GROUP_UNDO));
        menu.add(new GroupMarker(ITextEditorActionConstants.GROUP_SAVE));
        menu.add(new Separator(ITextEditorActionConstants.GROUP_COPY));
        menu.add(new Separator(ITextEditorActionConstants.GROUP_PRINT));
        menu.add(new Separator(ITextEditorActionConstants.GROUP_EDIT));
        menu.add(new Separator(ITextEditorActionConstants.GROUP_FIND));
        menu.add(new Separator(IWorkbenchActionConstants.GROUP_ADD));
        menu.add(new Separator(ITextEditorActionConstants.GROUP_REST));
        menu.add(new Separator());
        menu.add(new GroupMarker(GROUP_SQL_ADDITIONS));
        menu.add(new GroupMarker(GROUP_SQL_EXTRAS));
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

    public void handleActivate()
    {
        safelySanityCheckState(getEditorInput());
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

    public void loadFromExternalFile()
    {
        final File loadFile = DialogUtils.openFile(getSite().getShell(), new String[]{"*.sql", "*.txt", "*", "*.*"});
        if (loadFile == null) {
            return;
        }

        String newContent = null;
        try {
            try (Reader reader = new InputStreamReader(
                new FileInputStream(loadFile),
                GeneralUtils.DEFAULT_FILE_CHARSET))
            {
                StringWriter buffer = new StringWriter();
                IOUtils.copyText(reader, buffer);
                newContent = buffer.toString();
            }
        }
        catch (IOException e) {
            DBWorkbench.getPlatformUI().showError(
                    "Can't load file",
                "Can't load file '" + loadFile.getAbsolutePath() + "' - " + e.getMessage());
        }
        if (newContent != null) {
            Document document = getDocument();
            if (document != null) {
                document.set(newContent);
            }
        }
    }

    public void saveToExternalFile()
    {
        IEditorInput editorInput = getEditorInput();
        IFile curFile = EditorUtils.getFileFromInput(editorInput);
        String fileName = curFile == null ? null : curFile.getName();

        final Document document = getDocument();
        final File saveFile = DialogUtils.selectFileForSave(getSite().getShell(), "Save SQL script", new String[]{"*.sql", "*.txt", "*", "*.*"}, fileName);
        if (document == null || saveFile == null) {
            return;
        }

        try {
            UIUtils.runInProgressService(new DBRRunnableWithProgress() {
                @Override
                public void run(final DBRProgressMonitor monitor) throws InvocationTargetException, InterruptedException
                {
                    try {
                        StringReader cr = new StringReader(document.get());
                        ContentUtils.saveContentToFile(cr, saveFile, GeneralUtils.UTF8_ENCODING, monitor);
                    } catch (Exception e) {
                        throw new InvocationTargetException(e);
                    }
                }
            });
        } catch (InterruptedException e) {
            // do nothing
        } catch (InvocationTargetException e) {
            DBWorkbench.getPlatformUI().showError("Save failed", null, e.getTargetException());
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
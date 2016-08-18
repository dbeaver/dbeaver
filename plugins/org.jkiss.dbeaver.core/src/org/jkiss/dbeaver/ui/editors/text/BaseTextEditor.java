/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.ui.editors.text;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.GroupMarker;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IUndoManager;
import org.eclipse.jface.text.TextViewer;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.ide.FileStoreEditorInput;
import org.eclipse.ui.texteditor.AbstractDecoratedTextEditor;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditorActionConstants;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.ui.ICommentsSupport;
import org.jkiss.dbeaver.ui.ISingleControlEditor;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.DialogUtils;
import org.jkiss.dbeaver.ui.editors.EditorUtils;
import org.jkiss.dbeaver.ui.editors.INonPersistentEditorInput;
import org.jkiss.dbeaver.utils.ContentUtils;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.IOUtils;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

/**
 * Abstract text editor.
 * Contains some common dbeaver text editor adaptions.
 */
public abstract class BaseTextEditor extends AbstractDecoratedTextEditor implements ISingleControlEditor {

    public static final String GROUP_SQL_PREFERENCES = "sql.preferences";
    public static final String GROUP_SQL_ADDITIONS = "sql.additions";
    public static final String GROUP_SQL_EXTRAS = "sql.extras";

    private static Map<String, Integer> ACTION_TRANSLATE_MAP;

    public static Map<String, Integer> getActionMap()
    {
        if (ACTION_TRANSLATE_MAP == null) {
            ACTION_TRANSLATE_MAP = new HashMap<>();
            for (IdMapEntry entry : ACTION_MAP) {
                ACTION_TRANSLATE_MAP.put(entry.getActionId(), entry.getAction());
            }
        }
        return ACTION_TRANSLATE_MAP;
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
    public void dispose()
    {
//        fLineColumn = null;
        super.dispose();
    }

    @Nullable
    public Document getDocument()
    {
        IDocumentProvider provider = getDocumentProvider();
        return provider == null ? null : (Document)provider.getDocument(getEditorInput());
    }

    @Nullable
    @Override
    public Control getEditorControl() {
        final TextViewer textViewer = getTextViewer();
        return textViewer == null ? null : textViewer.getTextWidget();
    }

    @Override
    public void createPartControl(Composite parent)
    {
        //setPreferenceStore(new PreferenceStoreDelegate(DBeaverCore.getGlobalPreferenceStore()));

        super.createPartControl(parent);
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

    public TextViewer getTextViewer()
    {
        return (TextViewer) getSourceViewer();
    }

    public ISourceViewer getViewer()
    {
        return getSourceViewer();
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
            UIUtils.showErrorDialog(
                getSite().getShell(),
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
            DBeaverUI.runInProgressService(new DBRRunnableWithProgress() {
                @Override
                public void run(final DBRProgressMonitor monitor) throws InvocationTargetException, InterruptedException
                {
                    try {
                        StringReader cr = new StringReader(document.get());
                        ContentUtils.saveContentToFile(cr, saveFile, GeneralUtils.DEFAULT_FILE_CHARSET_NAME, monitor);
                    } catch (Exception e) {
                        throw new InvocationTargetException(e);
                    }
                }
            });
        } catch (InterruptedException e) {
            // do nothing
        } catch (InvocationTargetException e) {
            UIUtils.showErrorDialog(getSite().getShell(), "Save failed", null, e.getTargetException());
        }

        try {
            IFileStore fileStore = EFS.getStore(saveFile.toURI());
            IEditorInput input = new FileStoreEditorInput(fileStore);
            setExternalFileProperties(input);
            init(getEditorSite(), input);
        } catch (CoreException e) {
            UIUtils.showErrorDialog(getSite().getShell(), "File save", "Can't open SQL editor from external file", e);
        }
    }

    protected void setExternalFileProperties(IEditorInput input) {

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
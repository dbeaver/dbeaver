/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
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

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IUndoManager;
import org.eclipse.jface.text.TextViewer;
import org.eclipse.jface.text.source.CompositeRuler;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.IVerticalRuler;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.texteditor.AbstractTextEditor;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditorActionConstants;
import org.eclipse.ui.texteditor.StatusTextEditor;
import org.eclipse.ui.texteditor.rulers.*;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.ui.ICommentsSupport;
import org.jkiss.dbeaver.ui.ISingleControlEditor;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.DialogUtils;
import org.jkiss.dbeaver.ui.editors.EditorUtils;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditorInput;
import org.jkiss.dbeaver.ui.preferences.PreferenceStoreDelegate;
import org.jkiss.dbeaver.ui.resources.ResourceUtils;
import org.jkiss.dbeaver.utils.ContentUtils;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.IOUtils;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Abstract text editor.
 * Contains some common dbeaver text editor adaptions.
 */
public abstract class BaseTextEditor extends StatusTextEditor implements ISingleControlEditor {

    private final static String LINE_NUMBER_RULER = "lineNumberRule";

    private LineNumberColumn fLineColumn;
    private ScriptPositionColumn fScriptColumn;

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

    protected ScriptPositionColumn getScriptColumn()
    {
        return fScriptColumn;
    }

    @Override
    public void dispose()
    {
        fLineColumn = null;
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
        setPreferenceStore(new PreferenceStoreDelegate(DBeaverCore.getGlobalPreferenceStore()));

        super.createPartControl(parent);
    }

    @Override
    protected void editorContextMenuAboutToShow(IMenuManager menu)
    {
        super.editorContextMenuAboutToShow(menu);
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

    @Override
    protected void rulerContextMenuAboutToShow(IMenuManager menu) {
        menu.add(new Separator(ITextEditorActionConstants.GROUP_RULERS));
        menu.add(new Separator(ITextEditorActionConstants.GROUP_REST));

        super.rulerContextMenuAboutToShow(menu);

        addRulerContributionActions(menu);
    }

    /**
     * Adds "show" actions for all contributed rulers that support it.
     *
     * @param menu the ruler context menu
     * @since 3.3
     */
    private void addRulerContributionActions(IMenuManager menu) {
        // store directly in generic editor preferences
        final IColumnSupport support= (IColumnSupport) getAdapter(IColumnSupport.class);
        IPreferenceStore store = getPreferenceStore();
        final RulerColumnPreferenceAdapter adapter= new RulerColumnPreferenceAdapter(store, AbstractTextEditor.PREFERENCE_RULER_CONTRIBUTIONS);
        List descriptors = RulerColumnRegistry.getDefault().getColumnDescriptors();
        for (Iterator t = descriptors.iterator(); t.hasNext();) {
            final RulerColumnDescriptor descriptor= (RulerColumnDescriptor) t.next();
            if (!descriptor.isIncludedInMenu() || !support.isColumnSupported(descriptor))
                continue;
            final boolean isVisible= support.isColumnVisible(descriptor);
            IAction action= new Action("Show " + descriptor.getName(), IAction.AS_CHECK_BOX) {
                @Override
                public void run() {
                    if (descriptor.isGlobal())
                        // column state is modified via preference listener of AbstractTextEditor
                        adapter.setEnabled(descriptor, !isVisible);
                    else
                        // directly modify column for this editor instance
                        support.setColumnVisible(descriptor, !isVisible);
                }
            };
            action.setChecked(isVisible);
            action.setImageDescriptor(descriptor.getIcon());
            menu.appendToGroup(ITextEditorActionConstants.GROUP_RULERS, action);
        }
    }

    @Override
    public void showChangeInformation(boolean show)
    {
        if (show == isChangeInformationShowing())
            return;

        IColumnSupport columnSupport = (IColumnSupport) getAdapter(IColumnSupport.class);

        // only handle visibility of the combined column, but not the number/change only state
        if (show && fLineColumn == null) {
            RulerColumnDescriptor lineNumberColumnDescriptor = RulerColumnRegistry.getDefault().getColumnDescriptor(LineNumberColumn.ID);
            if (lineNumberColumnDescriptor != null)
                columnSupport.setColumnVisible(lineNumberColumnDescriptor, true);
        }
    }

    @Override
    public boolean isChangeInformationShowing()
    {
        return fLineColumn != null && fLineColumn.isShowingChangeInformation();
    }

    @Override
    protected final IColumnSupport createColumnSupport() {
        return new ColumnSupport(this, RulerColumnRegistry.getDefault()) {
            @Override
            protected void initializeColumn(IContributedRulerColumn column) {
                super.initializeColumn(column);
                RulerColumnDescriptor descriptor= column.getDescriptor();
                IVerticalRuler ruler = getVerticalRuler();
                if (ruler instanceof CompositeRuler) {
                    if (LineNumberColumn.ID.equals(descriptor.getId())) {
                        fLineColumn= ((LineNumberColumn) column);
                    }
                }
                if (column instanceof ScriptPositionColumn) {
                    fScriptColumn = (ScriptPositionColumn)column;
                }
            }
            @Override
            public void dispose() {
                fLineColumn= null;
                super.dispose();
            }
        };
    }

    @Override
    protected void handlePreferenceStoreChanged(PropertyChangeEvent event) {

        try {

            ISourceViewer sourceViewer= getSourceViewer();
            if (sourceViewer == null)
                return;

            String property= event.getProperty();

            if (LINE_NUMBER_RULER.equals(property)) {
                // only handle visibility of the combined column, but not the number/change only state
                IColumnSupport columnSupport= (IColumnSupport)getAdapter(IColumnSupport.class);
                if (fLineColumn == null) {
                    RulerColumnDescriptor lineNumberColumnDescriptor= RulerColumnRegistry.getDefault().getColumnDescriptor(LineNumberColumn.ID);
                    if (lineNumberColumnDescriptor != null)
                        columnSupport.setColumnVisible(lineNumberColumnDescriptor, true);
                }
            }
        } finally {
            super.handlePreferenceStoreChanged(event);
        }
    }

    @Override
    protected IVerticalRuler createVerticalRuler()
    {
        return new CompositeRuler();
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
        final File loadFile = DialogUtils.openFile(getSite().getShell(), new String[]{"*.sql", "*.txt", "*.*"});
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
                IOUtils.copyText(reader, buffer, 10000);
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
        IFile curFile = EditorUtils.getFileFromEditorInput(editorInput);
        String fileName = curFile == null ? null : curFile.getName();

        final Document document = getDocument();
        final File saveFile = DialogUtils.selectFileForSave(getSite().getShell(), "Save SQL script", new String[]{"*.sql", "*.txt", "*.*"}, fileName);
        if (document == null || saveFile == null) {
            return;
        }

        try {
            DBeaverUI.runInProgressService(new DBRRunnableWithProgress() {
                @Override
                public void run(final DBRProgressMonitor monitor) throws InvocationTargetException, InterruptedException
                {
                    UIUtils.runInUI(getSite().getShell(), new Runnable() {
                        @Override
                        public void run()
                        {
                            doSave(monitor.getNestedMonitor());
                        }
                    });

                    try {
                        ContentUtils.saveContentToFile(new StringReader(document.get()), saveFile, GeneralUtils.DEFAULT_FILE_CHARSET_NAME, monitor);
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

        if (curFile != null) {
            try {
                // TODO: change to EFS
                IPath location = new Path(saveFile.getAbsolutePath());
                IFolder scriptsFolder = ResourceUtils.getScriptsFolder(curFile.getProject(), true);
                IFile newFile = scriptsFolder.getFile(location.lastSegment());
                newFile.createLink(location, IResource.NONE, null);

                SQLEditorInput newInput = new SQLEditorInput(newFile);
                init(getEditorSite(), newInput);
            } catch (CoreException e) {
                UIUtils.showErrorDialog(getSite().getShell(), "File link", "Can't link SQL editor with external file", e);
            }
        }
    }

    @Nullable
    public int[] getCurrentLines()
    {
        return null;
    }
}
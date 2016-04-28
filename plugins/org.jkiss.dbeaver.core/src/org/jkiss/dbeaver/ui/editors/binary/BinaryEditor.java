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
package org.jkiss.dbeaver.ui.editors.binary;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.*;
import org.eclipse.ui.part.EditorPart;
import org.eclipse.ui.part.WorkbenchPart;
import org.eclipse.ui.texteditor.ITextEditorActionConstants;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPPreferenceListener;
import org.jkiss.dbeaver.model.DBPPreferenceStore;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.dbeaver.ui.editors.EditorUtils;
import org.jkiss.dbeaver.ui.editors.binary.pref.HexPreferencesPage;
import org.jkiss.dbeaver.utils.ContentUtils;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;


public class BinaryEditor extends EditorPart implements ISelectionProvider, IMenuListener, IResourceChangeListener {

    private static final Log log = Log.getLog(HexEditControl.class);

    //static final String textSavingFilePleaseWait = "Saving file, please wait";

    private HexManager manager;
    private DBPPreferenceListener preferencesChangeListener = null;
    private Set<ISelectionChangedListener> selectionListeners = null;  // of ISelectionChangedListener

    public BinaryEditor()
    {
        super();
    }

    protected HexManager getManager()
    {
        return manager;
    }

    @Override
    public void resourceChanged(IResourceChangeEvent event)
    {
        IResourceDelta delta= event.getDelta();
        if (delta == null) {
            return;
        }
        IPath localPath = null;
        IEditorInput input = getEditorInput();
        if (input instanceof IPathEditorInput) {
            localPath = ((IPathEditorInput)input).getPath();
        }
        if (localPath == null) {
            return;
        }
        localPath = ContentUtils.convertPathToWorkspacePath(localPath);
        delta = delta.findMember(localPath);
        if (delta == null) {
            return;
        }
        if (delta.getKind() == IResourceDelta.CHANGED) {
            // Refresh editor
            getSite().getShell().getDisplay().asyncExec(new Runnable() {
                @Override
                public void run()
                {
                    if (manager != null) {
                        loadBinaryContent();
                    }
                }
            });
        }
    }

    @Override
    public void addSelectionChangedListener(ISelectionChangedListener listener)
    {
        if (listener == null) return;

        if (selectionListeners == null) {
            selectionListeners = new HashSet<>();
        }
        selectionListeners.add(listener);
    }


    @Override
    public void createPartControl(Composite parent)
    {
        IEditorInput editorInput = getEditorInput();
        IStorage storage = null;
        {
            IFile file = EditorUtils.getFileFromEditorInput(editorInput);
            if (file != null) {
                storage = file;
            }
        }
        if (storage == null) {
            storage = editorInput.getAdapter(IStorage.class);
        }

        manager = new HexManager();
        manager.setTextFont(HexPreferencesPage.getPrefFontData());
        manager.setMenuListener(this);
        int editorStyle = SWT.NONE;
        if (storage != null && storage.isReadOnly()) {
            editorStyle = SWT.READ_ONLY;
        }
        manager.createEditorPart(parent, editorStyle);

        FillLayout fillLayout = new FillLayout();
        parent.setLayout(fillLayout);

        loadBinaryContent();

        // Register any global actions with the site's IActionBars.
        IActionBars bars = getEditorSite().getActionBars();
        createEditorAction(bars, IWorkbenchCommandConstants.EDIT_UNDO);
        createEditorAction(bars, IWorkbenchCommandConstants.EDIT_REDO);
        createEditorAction(bars, IWorkbenchCommandConstants.EDIT_CUT);
        createEditorAction(bars, IWorkbenchCommandConstants.EDIT_COPY);
        createEditorAction(bars, IWorkbenchCommandConstants.EDIT_PASTE);
        createEditorAction(bars, IWorkbenchCommandConstants.EDIT_DELETE);
        createEditorAction(bars, IWorkbenchCommandConstants.EDIT_SELECT_ALL);
        createEditorAction(bars, IWorkbenchCommandConstants.EDIT_FIND_AND_REPLACE);
        createEditorAction(bars, ITextEditorActionConstants.GOTO_LINE);

        manager.addListener(new Listener() {
            @Override
            public void handleEvent(Event event)
            {
                firePropertyChange(PROP_DIRTY);
                updateActionsStatus();
            }
        });

        bars.updateActionBars();

        preferencesChangeListener = new DBPPreferenceListener() {
            @Override
            public void preferenceChange(PreferenceChangeEvent event)
            {
                if (HexPreferencesPage.PROP_FONT_DATA.equals(event.getProperty()))
                    manager.setTextFont((FontData) event.getNewValue());
            }
        };
        DBPPreferenceStore store = DBeaverCore.getGlobalPreferenceStore();
        store.addPropertyChangeListener(preferencesChangeListener);

        manager.addLongSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                if (selectionListeners == null) return;

                long[] longSelection = HexEditControl.getLongSelection(e);
                SelectionChangedEvent event = new SelectionChangedEvent(
                    BinaryEditor.this,
                    new StructuredSelection(new Object[]{
                        longSelection[0], longSelection[1]}));
                for (ISelectionChangedListener selectionListener : selectionListeners) {
                    selectionListener.selectionChanged(event);
                }
            }
        });
        getSite().getPage().addSelectionListener(//getSite().getPage().getActiveEditor().getSite().getId(),
             new ISelectionListener() {
                 @Override
                 public void selectionChanged(IWorkbenchPart part,
                                              ISelection selection)
                 {
                     //if ("org.jkiss.dbeaver.ui.editors.binary".equals(part.getSite().getId())) return;
                 }
             });
//	getSite().setSelectionProvider(this);

    }

    private void createEditorAction(IActionBars bars, String id)
    {
        bars.setGlobalActionHandler(id, new EditorAction(id));
    }

    private void loadBinaryContent()
    {
        String charset = null;
        IEditorInput editorInput = getEditorInput();
        File systemFile = null;
        if (editorInput instanceof IPathEditorInput) {
            systemFile = ((IPathEditorInput) editorInput).getPath().toFile();
        }
        if (systemFile != null) {
            // open file
            try {
                manager.openFile(systemFile, charset);
            }
            catch (IOException e) {
                log.error("Can't open binary content", e);
            }
            setPartName(systemFile.getName());
        }
    }


    /**
     * Removes preferences-changed listener
     *
     * @see WorkbenchPart#dispose()
     */
    @Override
    public void dispose()
    {
        if (manager != null) {
            manager.dispose();
            manager = null;
        }

        DBPPreferenceStore store = DBeaverCore.getGlobalPreferenceStore();
        store.removePropertyChangeListener(preferencesChangeListener);

        ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);

        super.dispose();
    }


    /**
     * @see org.eclipse.ui.part.EditorPart#doSave(org.eclipse.core.runtime.IProgressMonitor)
     */
    @Override
    public void doSave(IProgressMonitor monitor)
    {
        IEditorInput editorInput = getEditorInput();
        // Sync file changes
        IFile file = EditorUtils.getFileFromEditorInput(editorInput);
        if (file != null) {
            final IPath absolutePath = file.getLocation();
            File systemFile = absolutePath.toFile();
            // Save to file
            try {
                manager.getContent().get(systemFile);
            }
            catch (IOException e) {
                log.error("Can't save binary content", e);
            }
            // Sync file changes
            ContentUtils.syncFile(RuntimeUtils.makeMonitor(monitor), file);
        }
    }


    /**
     * @see org.eclipse.ui.part.EditorPart#doSaveAs()
     */
    @Override
    public void doSaveAs()
    {
    }

    @Override
    public Object getAdapter(Class required)
    {
        if (BinaryContent.class.isAssignableFrom(required)) {
            return manager.getContent();
        } else if (HexManager.class.isAssignableFrom(required)) {
            return manager;
        } else {
            return super.getAdapter(required);
        }
    }

    @Override
    public ISelection getSelection()
    {
        long[] longSelection = manager.getSelection();
        return new StructuredSelection(new Object[]
            {longSelection[0], longSelection[1]});
    }

    @Override
    public void init(IEditorSite site, final IEditorInput input)
        throws PartInitException
    {
        boolean reset = getEditorInput() != null;
        setSite(site);
        if (!(input instanceof IPathEditorInput)) {
            throw new PartInitException("Editor Input is not a file");
        }
        setInput(input);
        if (reset) {
            loadBinaryContent();
        } else {
            // when opening an external file the workbench (Eclipse 3.1) calls HexEditorActionBarContributor.
            // MyStatusLineContributionItem.fill() before HexEditorActionBarContributor.setActiveEditor()
            // but we need an editor to fill the status bar.
            site.setSelectionProvider(this);

            ResourcesPlugin.getWorkspace().addResourceChangeListener(this);
        }
    }


    @Override
    public boolean isDirty()
    {
        return manager.isDirty();
    }


    @Override
    public boolean isSaveAsAllowed()
    {
        return true;
    }


    @Override
    public void removeSelectionChangedListener(ISelectionChangedListener listener)
    {
        if (selectionListeners != null) {
            selectionListeners.remove(listener);
        }
    }

    @Override
    public void setFocus()
    {
        // useless. It is called before ActionBarContributor.setActiveEditor() so focusing is done there
    }


    @Override
    public void setSelection(ISelection selection)
    {
        if (selection.isEmpty()) return;
        StructuredSelection aSelection = (StructuredSelection) selection;
        long[] startEnd = (long[]) aSelection.getFirstElement();
        long start = startEnd[0];
        long end = start;
        if (startEnd.length > 1) {
            end = startEnd[1];
        }
        if (aSelection.size() > 1) {
            startEnd = (long[]) aSelection.toArray()[1];
            end = startEnd[0];
            if (startEnd.length > 1) {
                end = startEnd[1];
            }
        }
        manager.setSelection(start, end);
    }


    /**
     * Updates the status of actions: enables/disables them depending on whether there is text selected
     * and whether inserting or overwriting. Undo/redo actions enabled/disabled as well.
     */
    public void updateActionsStatus()
    {
        boolean textSelected = manager.isTextSelected();
        boolean lengthModifiable = textSelected && !manager.isOverwriteMode();
        IActionBars bars = getEditorSite().getActionBars();
        IAction action = bars.getGlobalActionHandler(IWorkbenchCommandConstants.EDIT_UNDO);
        if (action != null) action.setEnabled(manager.canUndo());

        action = bars.getGlobalActionHandler(IWorkbenchCommandConstants.EDIT_REDO);
        if (action != null) action.setEnabled(manager.canRedo());

        action = bars.getGlobalActionHandler(IWorkbenchCommandConstants.EDIT_CUT);
        if (action != null) action.setEnabled(lengthModifiable);

        action = bars.getGlobalActionHandler(IWorkbenchCommandConstants.EDIT_COPY);
        if (action != null) action.setEnabled(textSelected);

        action = bars.getGlobalActionHandler(IWorkbenchCommandConstants.EDIT_DELETE);
        if (action != null) action.setEnabled(lengthModifiable);

        bars.updateActionBars();
    }

    @Override
    public void menuAboutToShow(IMenuManager manager)
    {
        manager.add(new EditorAction(IWorkbenchCommandConstants.EDIT_COPY, CoreMessages.dialog_find_replace_copy));
        manager.add(new EditorAction(IWorkbenchCommandConstants.EDIT_PASTE, CoreMessages.dialog_find_replace_paste));
        manager.add(new EditorAction(IWorkbenchCommandConstants.EDIT_SELECT_ALL, CoreMessages.controls_querylog_action_select_all));
        manager.add(new EditorAction(IWorkbenchCommandConstants.EDIT_FIND_AND_REPLACE, CoreMessages.dialog_find_replace_find_replace));
        manager.add(new EditorAction(ITextEditorActionDefinitionIds.LINE_GOTO, CoreMessages.dialog_find_replace_goto_line));
        manager.add(new Separator());
        manager.add(new EditorAction(IWorkbenchCommandConstants.EDIT_UNDO, CoreMessages.dialog_find_replace_undo));
        manager.add(new EditorAction(IWorkbenchCommandConstants.EDIT_REDO, CoreMessages.dialog_find_replace_redo));
    }

    class EditorAction extends Action {
        String actionId = null;

        EditorAction(String actionId, String text)
        {
            super(text);
            this.actionId = actionId;
            setActionDefinitionId(actionId);
        }

        EditorAction(String id)
        {
            actionId = id;
            setActionDefinitionId(actionId);
        }

        @Override
        public void run()
        {
            if (actionId.equals(IWorkbenchCommandConstants.EDIT_UNDO))
                manager.doUndo();
            else if (actionId.equals(IWorkbenchCommandConstants.EDIT_REDO))
                manager.doRedo();
            else if (actionId.equals(IWorkbenchCommandConstants.EDIT_CUT))
                manager.doCut();
            else if (actionId.equals(IWorkbenchCommandConstants.EDIT_COPY))
                manager.doCopy();
            else if (actionId.equals(IWorkbenchCommandConstants.EDIT_PASTE))
                manager.doPaste();
            else if (actionId.equals(IWorkbenchCommandConstants.EDIT_DELETE))
                manager.doDelete();
            else if (actionId.equals(IWorkbenchCommandConstants.EDIT_SELECT_ALL))
                manager.doSelectAll();
            else if (actionId.equals(IWorkbenchCommandConstants.EDIT_FIND_AND_REPLACE))
                manager.doFind();
            else if (actionId.equals(ITextEditorActionDefinitionIds.LINE_GOTO))
                manager.doGoTo();
        }
    }

}

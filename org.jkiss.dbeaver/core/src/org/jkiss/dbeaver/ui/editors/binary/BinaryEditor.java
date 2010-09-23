/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.editors.binary;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.action.*;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
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
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;


public class BinaryEditor extends EditorPart implements ISelectionProvider, IMenuListener, IResourceChangeListener {

    static final Log log = LogFactory.getLog(HexEditControl.class);

    static final String textSavingFilePleaseWait = "Saving file, please wait";

    private HexManager manager = null;
    IContentOutlinePage outlinePage = null;
    IPropertyChangeListener preferencesChangeListener = null;
    Set<ISelectionChangedListener> selectionListeners = null;  // of ISelectionChangedListener


    public BinaryEditor()
    {
        super();
    }


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
        delta = delta.findMember(localPath);
        if (delta == null) {
            return;
        }
        if (delta.getKind() == IResourceDelta.CHANGED) {
            // Refresh editor
            getSite().getShell().getDisplay().asyncExec(new Runnable() {
                public void run()
                {
                    loadBinaryContent();
                }
            });
        }
    }

    public void addSelectionChangedListener(ISelectionChangedListener listener)
    {
        if (listener == null) return;

        if (selectionListeners == null) {
            selectionListeners = new HashSet<ISelectionChangedListener>();
        }
        selectionListeners.add(listener);
    }


    public void createPartControl(Composite parent)
    {
        getManager().setTextFont(HexConfig.getFontData());
        getManager().setFindReplaceLists(HexConfig.getFindReplaceFindList(), HexConfig.getFindReplaceReplaceList());
        getManager().setMenuListener(this);
        int editorStyle = SWT.NONE;
        if (getEditorInput() instanceof IStorageEditorInput) {
            try {
                if (((IStorageEditorInput)getEditorInput()).getStorage().isReadOnly()) {
                    editorStyle |= SWT.READ_ONLY;
                }
            } catch (CoreException e) {
                log.warn(e);
                // do nothing
            }
        }
        getManager().createEditorPart(parent, editorStyle);
        FillLayout fillLayout = new FillLayout();
        parent.setLayout(fillLayout);

        loadBinaryContent();

        // Register any global actions with the site's IActionBars.
        IActionBars bars = getEditorSite().getActionBars();
        String id = IWorkbenchCommandConstants.EDIT_UNDO;
        bars.setGlobalActionHandler(id, new EditorAction(id));
        id = IWorkbenchCommandConstants.EDIT_REDO;
        bars.setGlobalActionHandler(id, new EditorAction(id));
        id = IWorkbenchCommandConstants.EDIT_CUT;
        bars.setGlobalActionHandler(id, new EditorAction(id));
        id = IWorkbenchCommandConstants.EDIT_COPY;
        bars.setGlobalActionHandler(id, new EditorAction(id));
        id = IWorkbenchCommandConstants.EDIT_PASTE;
        bars.setGlobalActionHandler(id, new EditorAction(id));
        id = IWorkbenchCommandConstants.EDIT_DELETE;
        bars.setGlobalActionHandler(id, new EditorAction(id));
        id = IWorkbenchCommandConstants.EDIT_SELECT_ALL;
        bars.setGlobalActionHandler(id, new EditorAction(id));
        id = IWorkbenchCommandConstants.EDIT_FIND_AND_REPLACE;
        bars.setGlobalActionHandler(id, new EditorAction(id));
        id = ITextEditorActionConstants.GOTO_LINE;
        bars.setGlobalActionHandler(id, new EditorAction(id));

        manager.addListener(new Listener() {
            public void handleEvent(Event event)
            {
                firePropertyChange(PROP_DIRTY);
                updateActionsStatus();
            }
        });

        bars.updateActionBars();

        preferencesChangeListener = new IPropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent event)
            {
                if (PreferencesPage.preferenceFontData.equals(event.getProperty()))
                    manager.setTextFont((FontData) event.getNewValue());
            }
        };
        IPreferenceStore store = HexConfig.getInstance().getPreferenceStore();
        store.addPropertyChangeListener(preferencesChangeListener);

        manager.addLongSelectionListener(new SelectionAdapter() {
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
                 public void selectionChanged(IWorkbenchPart part,
                                              ISelection selection)
                 {
                     //if ("org.jkiss.dbeaver.ui.editors.binary".equals(part.getSite().getId())) return;
                 }
             });
//	getSite().setSelectionProvider(this);

    }

    private void loadBinaryContent()
    {
        String charset = null;
        IEditorInput unresolved = getEditorInput();
        File systemFile = null;
        if (unresolved instanceof IPathEditorInput) {  // eg. FileInPlaceEditorInput
            IPathEditorInput file = (IPathEditorInput) unresolved;
            systemFile = file.getPath().toFile();
        }
        // open file
        try {
            manager.openFile(systemFile, charset);
        }
        catch (IOException e) {
            log.error("Could not open binary content", e);
        }
        if (systemFile != null) {
            setPartName(systemFile.getName());
        }
    }


    /**
     * Removes preferences-changed listener
     *
     * @see WorkbenchPart#dispose()
     */
    public void dispose()
    {
        if (manager != null) {
            manager.dispose();
            manager = null;
        }

        IPreferenceStore store = HexConfig.getInstance().getPreferenceStore();
        store.removePropertyChangeListener(preferencesChangeListener);

        ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);

        super.dispose();
    }


    /**
     * @see org.eclipse.ui.part.EditorPart#doSave(org.eclipse.core.runtime.IProgressMonitor)
     */
    public void doSave(IProgressMonitor monitor)
    {
    }


    /**
     * @see org.eclipse.ui.part.EditorPart#doSaveAs()
     */
    public void doSaveAs()
    {
    }


    public Object getAdapter(Class required)
    {
        Object result;
        if (IContentOutlinePage.class.isAssignableFrom(required)) {
            if (outlinePage == null) {
                outlinePage = getOutlinePage();
            }
            result = outlinePage;
        } else if (BinaryContent.class.isAssignableFrom(required)) {
            result = getManager().getContent();
        } else if (HexManager.class.isAssignableFrom(required)) {
            result = getManager();
        } else {
            result = super.getAdapter(required);
        }
        return result;
    }


    /**
     * Getter for the manager instance.
     *
     * @return the manager
     */
    public HexManager getManager()
    {
        if (manager == null)
            manager = new HexManager();

        return manager;
    }


    IContentOutlinePage getOutlinePage()
    {
        IExtensionRegistry registry = Platform.getExtensionRegistry();
        IExtensionPoint point = registry.getExtensionPoint("org.jkiss.dbeaver.ui.editors.binary.outline");
        if (point == null) return null;

        IExtension[] extensions = point.getExtensions();
        if (extensions.length == 0) return null;
        IConfigurationElement[] elements = extensions[0].getConfigurationElements();
        String className = null;
        for (IConfigurationElement element : elements) {
            if ("outline".equals(element.getName())) {
                className = element.getAttribute("class");
                break;
            }
        }

        Bundle aBundle = Platform.getBundle(extensions[0].getNamespaceIdentifier());
        IContentOutlinePage result = null;
        if (aBundle != null) {
            try {
                aBundle.start();
            }
            catch (BundleException e) {
                return null;
            }
            try {
                // throws IllegalAccessException, InstantiationException, ClassNotFoundException
                result = (IContentOutlinePage) aBundle.loadClass(className).newInstance();
            }
            catch (Exception e) {
                return null;
            }
        }

        return result;
    }


    public ISelection getSelection()
    {
        long[] longSelection = getManager().getSelection();
        return new StructuredSelection(new Object[]
            {longSelection[0], longSelection[1]});
    }


    boolean implementsInterface(IEditorInput input, String interfaceName)
    {
        Class[] classes = input.getClass().getInterfaces();
        for (Class aClass : classes) {
            if (interfaceName.equals(aClass.getName()))
                return true;
        }

        return false;
    }


    public void init(IEditorSite site, final IEditorInput input)
        throws PartInitException
    {
        setSite(site);
        if (!(input instanceof IPathEditorInput)) {
            throw new PartInitException("Editor Input is not a file");
        }
        setInput(input);
        // when opening an external file the workbench (Eclipse 3.1) calls HexEditorActionBarContributor.
        // MyStatusLineContributionItem.fill() before HexEditorActionBarContributor.setActiveEditor()
        // but we need an editor to fill the status bar.
        site.setSelectionProvider(this);

        ResourcesPlugin.getWorkspace().addResourceChangeListener(this);
    }


    public boolean isDirty()
    {
        return getManager().isDirty();
    }


    public boolean isSaveAsAllowed()
    {
        return true;
    }


    public void removeSelectionChangedListener(ISelectionChangedListener listener)
    {
        if (selectionListeners != null) {
            selectionListeners.remove(listener);
        }
    }

    public void setFocus()
    {
        // useless. It is called before ActionBarContributor.setActiveEditor() so focusing is done there
    }


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
        getManager().setSelection(start, end);
    }


    /**
     * Updates the status of actions: enables/disables them depending on whether there is text selected
     * and whether inserting or overwriting. Undo/redo actions enabled/disabled as well.
     */
    public void updateActionsStatus()
    {
        boolean textSelected = getManager().isTextSelected();
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

    public void menuAboutToShow(IMenuManager manager)
    {
        manager.add(new EditorAction(IWorkbenchCommandConstants.EDIT_COPY, "Copy"));
        manager.add(new EditorAction(IWorkbenchCommandConstants.EDIT_PASTE, "Paste"));
        manager.add(new EditorAction(IWorkbenchCommandConstants.EDIT_SELECT_ALL, "Select All"));
        manager.add(new EditorAction(IWorkbenchCommandConstants.EDIT_FIND_AND_REPLACE, "Find/Replace"));
        manager.add(new EditorAction(ITextEditorActionConstants.GOTO_LINE, "Go to line"));
        manager.add(new Separator());
        manager.add(new EditorAction(IWorkbenchCommandConstants.EDIT_UNDO, "Undo"));
        manager.add(new EditorAction(IWorkbenchCommandConstants.EDIT_REDO, "Redo"));
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

        public void run()
        {
            if (actionId.equals(IWorkbenchCommandConstants.EDIT_UNDO))
                getManager().doUndo();
            else if (actionId.equals(IWorkbenchCommandConstants.EDIT_REDO))
                getManager().doRedo();
            else if (actionId.equals(IWorkbenchCommandConstants.EDIT_CUT))
                getManager().doCut();
            else if (actionId.equals(IWorkbenchCommandConstants.EDIT_COPY))
                getManager().doCopy();
            else if (actionId.equals(IWorkbenchCommandConstants.EDIT_PASTE))
                getManager().doPaste();
            else if (actionId.equals(IWorkbenchCommandConstants.EDIT_DELETE))
                getManager().doDelete();
            else if (actionId.equals(IWorkbenchCommandConstants.EDIT_SELECT_ALL))
                getManager().doSelectAll();
            else if (actionId.equals(IWorkbenchCommandConstants.EDIT_FIND_AND_REPLACE))
                getManager().doFind();
            else if (actionId.equals(ITextEditorActionConstants.GOTO_LINE))
                getManager().doGoTo();
        }
    }

}

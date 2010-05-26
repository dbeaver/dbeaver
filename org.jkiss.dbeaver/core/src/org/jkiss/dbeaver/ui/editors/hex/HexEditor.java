/*
 * hex, a java hex editor
 * Copyright (C) 2006, 2009 Jordi Bergenthal, pestatije(-at_)users.sourceforge.net
 * The official hex site is sourceforge.net/projects/hex
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package org.jkiss.dbeaver.ui.editors.hex;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.action.*;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IPathEditorInput;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IStorageEditorInput;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.editors.text.ILocationProvider;
import org.eclipse.ui.part.EditorPart;
import org.eclipse.ui.part.WorkbenchPart;
import org.eclipse.ui.texteditor.IWorkbenchActionDefinitionIds;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;


public class HexEditor extends EditorPart implements ISelectionProvider, IMenuListener, IResourceChangeListener {

    static Log log = LogFactory.getLog(HexTexts.class);

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
            if (actionId.equals(IWorkbenchActionDefinitionIds.UNDO))
                getManager().doUndo();
            else if (actionId.equals(IWorkbenchActionDefinitionIds.REDO))
                getManager().doRedo();
            else if (actionId.equals(IWorkbenchActionDefinitionIds.CUT))
                getManager().doCut();
            else if (actionId.equals(IWorkbenchActionDefinitionIds.COPY))
                getManager().doCopy();
            else if (actionId.equals(IWorkbenchActionDefinitionIds.PASTE))
                getManager().doPaste();
            else if (actionId.equals(IWorkbenchActionDefinitionIds.DELETE))
                getManager().doDelete();
            else if (actionId.equals(IWorkbenchActionDefinitionIds.SELECT_ALL))
                getManager().doSelectAll();
            else if (actionId.equals(IWorkbenchActionDefinitionIds.FIND_REPLACE))
                getManager().doFind();
        }
    }

    static final String textSavingFilePleaseWait = "Saving file, please wait";

    private HexManager manager = null;
    IContentOutlinePage outlinePage = null;
    IPropertyChangeListener preferencesChangeListener = null;
    Set<ISelectionChangedListener> selectionListeners = null;  // of ISelectionChangedListener


    public HexEditor()
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
        if (input instanceof IFileEditorInput) {
            localPath = ((IFileEditorInput)input).getFile().getFullPath();
        } else if (input instanceof IPathEditorInput) {
            localPath = ((IPathEditorInput)input).getPath();
        } else if (input instanceof ILocationProvider) {
            localPath = ((ILocationProvider)input).getPath(input);
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
            this.loadBinaryContent();
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
        String id = IWorkbenchActionDefinitionIds.UNDO;
        bars.setGlobalActionHandler(id, new EditorAction(id));
        id = IWorkbenchActionDefinitionIds.REDO;
        bars.setGlobalActionHandler(id, new EditorAction(id));
        id = IWorkbenchActionDefinitionIds.CUT;
        bars.setGlobalActionHandler(id, new EditorAction(id));
        id = IWorkbenchActionDefinitionIds.COPY;
        bars.setGlobalActionHandler(id, new EditorAction(id));
        id = IWorkbenchActionDefinitionIds.PASTE;
        bars.setGlobalActionHandler(id, new EditorAction(id));
        id = IWorkbenchActionDefinitionIds.DELETE;
        bars.setGlobalActionHandler(id, new EditorAction(id));
        id = IWorkbenchActionDefinitionIds.SELECT_ALL;
        bars.setGlobalActionHandler(id, new EditorAction(id));
        id = IWorkbenchActionDefinitionIds.FIND_REPLACE;
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

                long[] longSelection = HexTexts.getLongSelection(e);
                SelectionChangedEvent event = new SelectionChangedEvent(
                    HexEditor.this,
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
                     //if ("org.jkiss.dbeaver.ui.editors.hex".equals(part.getSite().getId())) return;
                 }
             });
//	getSite().setSelectionProvider(this);

    }

    private void loadBinaryContent()
    {
        String charset = null;
        IEditorInput unresolved = getEditorInput();
        File systemFile = null;
        IFile localFile = null;
        if (unresolved instanceof IFileEditorInput) {
            localFile = ((IFileEditorInput) unresolved).getFile();
        } else if (unresolved instanceof IPathEditorInput) {  // eg. FileInPlaceEditorInput
            IPathEditorInput file = (IPathEditorInput) unresolved;
            systemFile = file.getPath().toFile();
        } else if (unresolved instanceof ILocationProvider) {
            ILocationProvider location = (ILocationProvider) unresolved;
            IWorkspaceRoot rootWorkspace = ResourcesPlugin.getWorkspace().getRoot();
            localFile = rootWorkspace.getFile(location.getPath(location));
        }
        // charset
        if (localFile != null) {
            systemFile = localFile.getLocation().toFile();
            try {
                charset = localFile.getCharset(true);
            }
            catch (CoreException e1) {
                log.warn(e1);
            }
        }
        // open file
        try {
            manager.openFile(systemFile, charset);
        }
        catch (IOException e) {
            log.error("Could not open hex content", e);
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
        IPreferenceStore store = HexConfig.getInstance().getPreferenceStore();
        store.removePropertyChangeListener(preferencesChangeListener);

        ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);
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
        IExtensionPoint point = registry.getExtensionPoint("org.jkiss.dbeaver.ui.editors.hex.outline");
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
        if (!(input instanceof IFileEditorInput) &&
            !(input instanceof IPathEditorInput) &&
            !(input instanceof ILocationProvider))
        {
            throw new PartInitException("Editor Input is not a file");
        }
        setInput(input);
        // when opening an external file the workbench (Eclipse 3.1) calls HexEditorActionBarContributor.
        // MyStatusLineContributionItem.fill() before HexEditorActionBarContributor.setActiveEditor()
        // but we need an editor to fill the status bar.
        site.getActionBarContributor().setActiveEditor(this);
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
        IAction action = bars.getGlobalActionHandler(IWorkbenchActionDefinitionIds.UNDO);
        if (action != null) action.setEnabled(manager.canUndo());

        action = bars.getGlobalActionHandler(IWorkbenchActionDefinitionIds.REDO);
        if (action != null) action.setEnabled(manager.canRedo());

        action = bars.getGlobalActionHandler(IWorkbenchActionDefinitionIds.CUT);
        if (action != null) action.setEnabled(lengthModifiable);

        action = bars.getGlobalActionHandler(IWorkbenchActionDefinitionIds.COPY);
        if (action != null) action.setEnabled(textSelected);

        action = bars.getGlobalActionHandler(IWorkbenchActionDefinitionIds.DELETE);
        if (action != null) action.setEnabled(lengthModifiable);

        bars.updateActionBars();
    }

    public void menuAboutToShow(IMenuManager manager)
    {
        manager.add(new EditorAction(IWorkbenchActionDefinitionIds.COPY, "Copy"));
        manager.add(new EditorAction(IWorkbenchActionDefinitionIds.PASTE, "Paste"));
        manager.add(new EditorAction(IWorkbenchActionDefinitionIds.SELECT_ALL, "Select All"));
        manager.add(new EditorAction(IWorkbenchActionDefinitionIds.FIND_REPLACE, "Find/Replace"));
        manager.add(new Separator());
        manager.add(new EditorAction(IWorkbenchActionDefinitionIds.UNDO, "Undo"));
        manager.add(new EditorAction(IWorkbenchActionDefinitionIds.REDO, "Redo"));
    }

}

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

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
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
import org.eclipse.ui.IPathEditorInput;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.editors.text.ILocationProvider;
import org.eclipse.ui.part.EditorPart;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.part.WorkbenchPart;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.HashSet;
import java.util.Set;


public class HexEditor extends EditorPart implements ISelectionProvider {


    class MyAction extends Action {
        String myId = null;

        MyAction(String id)
        {
            myId = id;
        }

        public void run()
        {
            if (myId.equals(ActionFactory.UNDO.getId()))
                getManager().doUndo();
            else if (myId.equals(ActionFactory.REDO.getId()))
                getManager().doRedo();
            else if (myId.equals(ActionFactory.CUT.getId()))
                getManager().doCut();
            else if (myId.equals(ActionFactory.COPY.getId()))
                getManager().doCopy();
            else if (myId.equals(ActionFactory.PASTE.getId()))
                getManager().doPaste();
            else if (myId.equals(ActionFactory.DELETE.getId()))
                getManager().doDelete();
            else if (myId.equals(ActionFactory.SELECT_ALL.getId()))
                getManager().doSelectAll();
            else if (myId.equals(ActionFactory.FIND.getId()))
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
        getManager().setFindReplaceLists(HexConfig.getFindReplaceFindList(),
                                         HexConfig.getFindReplaceReplaceList());
        manager.createEditorPart(parent);
        FillLayout fillLayout = new FillLayout();
        parent.setLayout(fillLayout);

        String charset = null;
        IEditorInput unresolved = getEditorInput();
        File systemFile = null;
        IFile localFile = null;
        if (unresolved instanceof FileEditorInput) {
            localFile = ((FileEditorInput) unresolved).getFile();
        } else if (unresolved instanceof IPathEditorInput) {  // eg. FileInPlaceEditorInput
            IPathEditorInput file = (IPathEditorInput) unresolved;
            systemFile = file.getPath().toFile();
        } else if (unresolved instanceof ILocationProvider) {
            ILocationProvider location = (ILocationProvider) unresolved;
            IWorkspaceRoot rootWorkspace = ResourcesPlugin.getWorkspace().getRoot();
            localFile = rootWorkspace.getFile(location.getPath(location));
        } else {
            URI uri = inputForVersion3_3(unresolved);
            if (uri != null) {
                IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
                IFile[] files = new IFile[0];
                try {
                    // throws NoSuchMethodException
                    Method method = ResourcesPlugin.class.getMethod("findFilesForLocationURI",
                                                                    new Class[]{URI.class});
                    // throws IllegalAccessException, IllegalArgumentException, InvocationTargetException
                    files = (IFile[]) method.invoke(root, uri);
                }
                catch (Exception e) {
                    // do nothing
                }  // keep going with no charset
                // since 3.2
                //IFile[] files = ResourcesPlugin.getWorkspace().getRoot().findFilesForLocationURI(uri);
                if (files.length != 0) {
                    localFile = files[0];
                } else {
                    systemFile = new File(uri);
                }
            } else {
                systemFile = null;
            }
        }
        // charset
        if (localFile != null) {
            systemFile = localFile.getLocation().toFile();
            try {
                charset = localFile.getCharset(true);
            }
            catch (CoreException e1) {
                e1.printStackTrace();
            }
        }
        // open file
        try {
            manager.openFile(systemFile, charset);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
        if (systemFile != null) {
            setPartName(systemFile.getName());
        }

        // Register any global actions with the site's IActionBars.
        IActionBars bars = getEditorSite().getActionBars();
        String id = ActionFactory.UNDO.getId();
        bars.setGlobalActionHandler(id, new MyAction(id));
        id = ActionFactory.REDO.getId();
        bars.setGlobalActionHandler(id, new MyAction(id));
        id = ActionFactory.CUT.getId();
        bars.setGlobalActionHandler(id, new MyAction(id));
        id = ActionFactory.COPY.getId();
        bars.setGlobalActionHandler(id, new MyAction(id));
        id = ActionFactory.PASTE.getId();
        bars.setGlobalActionHandler(id, new MyAction(id));
        id = ActionFactory.DELETE.getId();
        bars.setGlobalActionHandler(id, new MyAction(id));
        id = ActionFactory.SELECT_ALL.getId();
        bars.setGlobalActionHandler(id, new MyAction(id));
        id = ActionFactory.FIND.getId();
        bars.setGlobalActionHandler(id, new MyAction(id));

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


    /**
     * Removes preferences-changed listener
     *
     * @see WorkbenchPart#dispose()
     */
    public void dispose()
    {
        IPreferenceStore store = HexConfig.getInstance().getPreferenceStore();
        store.removePropertyChangeListener(preferencesChangeListener);
    }


    /**
     * @see org.eclipse.ui.part.EditorPart#doSave(org.eclipse.core.runtime.IProgressMonitor)
     */
    public void doSave(IProgressMonitor monitor)
    {
        monitor.beginTask(textSavingFilePleaseWait, IProgressMonitor.UNKNOWN);
        boolean successful = getManager().saveFile();
        monitor.done();
        if (!successful) {
            manager.showErrorBox(getEditorSite().getShell());
        }
    }


    /**
     * @see org.eclipse.ui.part.EditorPart#doSaveAs()
     */
    public void doSaveAs()
    {
        saveToFile(false);
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
        if (!(input instanceof IPathEditorInput) &&
            !(input instanceof ILocationProvider) &&
            (!implementsInterface(input, "org.eclipse.ui.IURIEditorInput") ||  // since 3.3 only
                inputForVersion3_3(input) == null))
        {
            throw new PartInitException("Input is not a file");
        }
        setInput(input);
        // when opening an external file the workbench (Eclipse 3.1) calls HexEditorActionBarContributor.
        // MyStatusLineContributionItem.fill() before HexEditorActionBarContributor.setActiveEditor()
        // but we need an editor to fill the status bar.
        site.getActionBarContributor().setActiveEditor(this);
        site.setSelectionProvider(this);
    }


    private URI inputForVersion3_3(IEditorInput input)
    {
        URI result;
        try {
            Class aClass = input.getClass();
            // throws NoSuchMethodException
            Method uriMethod = aClass.getMethod("getURI");
            // throws IllegalAccessException, InvocationTargetException
            result = (URI) uriMethod.invoke(input);
        }
        catch (Throwable e) {
            return null;
        }

        return result;
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


    void saveToFile(final boolean selection)
    {
        final File file = getManager().showSaveAsDialog(getEditorSite().getShell(), selection);
        if (file == null) return;

        IRunnableWithProgress runnable = new IRunnableWithProgress() {
            public void run(IProgressMonitor monitor)
            {
                monitor.beginTask(textSavingFilePleaseWait, IProgressMonitor.UNKNOWN);
                boolean successful;
                if (selection)
                    successful = manager.doSaveSelectionAs(file);
                else
                    successful = manager.saveAsFile(file);
                monitor.done();
                if (successful && !selection) {
                    setPartName(file.getName());
                    firePropertyChange(PROP_DIRTY);
                }
                if (!successful)
                    manager.showErrorBox(getEditorSite().getShell());
            }
        };
        ProgressMonitorDialog monitorDialog = new ProgressMonitorDialog(getEditorSite().getShell());
        try {
            monitorDialog.run(false, false, runnable);
        }
        catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }
        catch (InterruptedException e) {
            throw new RuntimeException(e);
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
        IAction action = bars.getGlobalActionHandler(ActionFactory.UNDO.getId());
        if (action != null) action.setEnabled(manager.canUndo());

        action = bars.getGlobalActionHandler(ActionFactory.REDO.getId());
        if (action != null) action.setEnabled(manager.canRedo());

        action = bars.getGlobalActionHandler(ActionFactory.CUT.getId());
        if (action != null) action.setEnabled(lengthModifiable);

        action = bars.getGlobalActionHandler(ActionFactory.COPY.getId());
        if (action != null) action.setEnabled(textSelected);

        action = bars.getGlobalActionHandler(ActionFactory.DELETE.getId());
        if (action != null) action.setEnabled(lengthModifiable);

        bars.updateActionBars();
    }
}

/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.editors.splitted;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.ISafeRunnable;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Item;
import org.eclipse.ui.*;
import org.eclipse.ui.part.EditorPart;
import org.eclipse.ui.part.IWorkbenchPartOrientation;
import org.eclipse.ui.part.PageSwitcher;
import org.eclipse.ui.services.IDisposable;
import org.eclipse.ui.services.IServiceLocator;
import org.jkiss.dbeaver.ext.ui.IEmbeddedWorkbenchPart;

import java.util.ArrayList;
import java.util.List;

public abstract class SplitterEditorPart extends EditorPart {
    static final Log log = LogFactory.getLog(SplitterEditorPart.class);

    private CTabFolder container;
    private Cursor hyperlinkCursor;
    private List<IWorkbenchPart> nestedParts = new ArrayList<IWorkbenchPart>();

    protected SplitterEditorPart()
    {
        super();
        hyperlinkCursor = new Cursor(Display.getCurrent(), SWT.CURSOR_HAND);
    }

    public int addPage(Control control)
    {
        int index = getPageCount();
        addPage(index, control);
        return index;
    }

    public void addPage(int index, Control control)
    {
        createItem(index, control);
    }

    public int addPage(IEditorPart editor, IEditorInput input)
        throws PartInitException
    {
        int index = getPageCount();
        addPage(index, editor, input);
        return index;
    }

    public void addPage(int index, IEditorPart editor, IEditorInput input)
        throws PartInitException
    {
        IEditorSite site = new SplitterEditorSite(this, editor);
        editor.init(site, input);
        addPagePart(index, editor);
    }

    private void addPagePart(int index, IWorkbenchPart part)
        throws PartInitException
    {
        Composite parent2 = new Composite(getContainer(), getOrientation(part));
        parent2.setLayout(new FillLayout());
        part.createPartControl(parent2);
        part.addPropertyListener(new IPropertyListener() {
            public void propertyChanged(Object source, int propertyId)
            {
                SplitterEditorPart.this.handlePropertyChange(propertyId);
            }
        });
        // create item for page only after createPartControl has succeeded
        Item item = createItem(index, parent2);
        // remember the editor, as both data on the item, and in the list of
        // editors (see field comment)
        item.setData(part);
        nestedParts.add(part);
    }

    protected int getContainerStyle()
    {
        return SWT.BOTTOM | SWT.FLAT;
    }

    protected int getContainerMargin()
    {
        return 1;
    }

    private int getOrientation(IWorkbenchPart editor)
    {
        if (editor instanceof IWorkbenchPartOrientation) {
            return ((IWorkbenchPartOrientation) editor).getOrientation();
        }
        return getOrientation();
    }

    private CTabFolder createContainer(Composite parent)
    {
        // use SWT.FLAT style so that an extra 1 pixel border is not reserved
        // inside the folder
        GridLayout gl = new GridLayout(1, false);
        gl.marginHeight = gl.marginWidth = getContainerMargin();
        parent.setLayout(gl);
        final CTabFolder newContainer = new CTabFolder(parent, getContainerStyle());
        newContainer.setLayoutData(new GridData(GridData.FILL_BOTH));
        newContainer.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e)
            {
                int newPageIndex = newContainer.indexOf((CTabItem) e.item);
                pageChange(newPageIndex);
            }
        });

        newContainer.setSimple(false);
        //newContainer.setCursor(hyperlinkCursor);
        return newContainer;
    }

    private CTabItem createItem(int index, Control control)
    {
        CTabItem item = new CTabItem(getTabFolder(), SWT.NONE, index);
        item.setControl(control);
        return item;
    }

    protected abstract void createPages();

    public final void createPartControl(Composite parent)
    {
        Composite pageContainer = createPageContainer(parent);
        this.container = createContainer(pageContainer);
        createPages();
        // set the active page (page 0 by default), unless it has already been
        // done
        if (getActivePage() == -1) {
            setActivePage(0);
        }
        initializePageSwitching();
        postPagesCreate();
    }

    protected void postPagesCreate()
    {
    }

    protected void initializePageSwitching()
    {
        new PageSwitcher(this) {
            public Object[] getPages()
            {
                int pageCount = getPageCount();
                Object[] result = new Object[pageCount];
                for (int i = 0; i < pageCount; i++) {
                    result[i] = i;
                }
                return result;
            }

            public String getName(Object page)
            {
                return getPageText(((Integer) page));
            }

            public ImageDescriptor getImageDescriptor(Object page)
            {
                Image image = getPageImage(((Integer) page));
                if (image == null)
                    return null;

                return ImageDescriptor.createFromImage(image);
            }

            public void activatePage(Object page)
            {
                setActivePage(((Integer) page));
            }

            public int getCurrentPageIndex()
            {
                return getActivePage();
            }
        };
    }

    protected Composite createPageContainer(Composite parent)
    {
        return parent;
    }

    public void dispose()
    {
        for (IWorkbenchPart part : nestedParts) {
            disposePart(part);
        }
        nestedParts.clear();

        if (hyperlinkCursor != null) {
            hyperlinkCursor.dispose();
        }
        super.dispose();
    }

    protected IWorkbenchPart getActiveEditor()
    {
        int index = getActivePage();
        if (index != -1) {
            return getEditor(index);
        }
        return null;
    }

    protected int getActivePage()
    {
        CTabFolder tabFolder = getTabFolder();
        if (tabFolder != null && !tabFolder.isDisposed()) {
            return tabFolder.getSelectionIndex();
        }
        return -1;
    }

    protected CTabFolder getContainer()
    {
        return container;
    }

    protected Control getControl(int pageIndex)
    {
        return getItem(pageIndex).getControl();
    }

    protected IWorkbenchPart getEditor(int pageIndex)
    {
        Item item = getItem(pageIndex);
        if (item != null) {
            Object data = item.getData();
            if (data instanceof IWorkbenchPart) {
                return (IWorkbenchPart) data;
            }
        }
        return null;
    }

    private CTabItem getItem(int pageIndex)
    {
        return getTabFolder().getItem(pageIndex);
    }

    protected int getPageCount()
    {
        CTabFolder folder = getTabFolder();
        // May not have been created yet, or may have been disposed.
        if (folder != null && !folder.isDisposed()) {
            return folder.getItemCount();
        }
        return 0;
    }

    protected Image getPageImage(int pageIndex)
    {
        return getItem(pageIndex).getImage();
    }

    protected String getPageText(int pageIndex)
    {
        return getItem(pageIndex).getText();
    }

    private CTabFolder getTabFolder()
    {
        return container;
    }

    protected void handlePropertyChange(int propertyId)
    {
        firePropertyChange(propertyId);
    }

    public void init(IEditorSite site, IEditorInput input)
        throws PartInitException
    {
        setSite(site);
        setInput(input);
        site.setSelectionProvider(new SplitterSelectionProvider(this));
    }

    public boolean isDirty()
    {
        // use nestedEditors to avoid SWT requests; see bug 12996
        for (Object nestedPart : nestedParts) {
            IEditorPart editor = (IEditorPart) nestedPart;
            if (editor.isDirty()) {
                return true;
            }
        }
        return false;
    }

    protected void pageChange(int newPageIndex)
    {
        deactivateSite();

        IPartService partService = (IPartService) getSite().getService(IPartService.class);
        if (partService.getActivePart() == this) {
            setFocus();
        }

        IWorkbenchPart activePart = getEditor(newPageIndex);

        IEditorActionBarContributor contributor = getEditorSite().getActionBarContributor();
        if (contributor != null && contributor instanceof SplitterEditorActionBarContributor) {
            ((SplitterEditorActionBarContributor) contributor).setActivePage(activePart);
        }

        if (activePart != null) {
            ISelectionProvider selectionProvider = activePart.getSite().getSelectionProvider();
            if (selectionProvider != null) {
                ISelectionProvider outerProvider = getSite().getSelectionProvider();
                if (outerProvider instanceof SplitterSelectionProvider) {
                    SelectionChangedEvent event = new SelectionChangedEvent(
                        selectionProvider, selectionProvider.getSelection());

                    SplitterSelectionProvider provider = (SplitterSelectionProvider) outerProvider;
                    provider.fireSelectionChanged(event);
                    provider.firePostSelectionChanged(event);
                }
            }
        }

        activateSite();
    }

    protected final void deactivateSite()
    {
        // Deactivate the nested services from the last active service locator.
        final int pageIndex = getActivePage();
        final IWorkbenchPart part = getEditor(pageIndex);
        if (part instanceof IEmbeddedWorkbenchPart) {
            ((IEmbeddedWorkbenchPart) part).deactivatePart();
        }
    }

    protected final void activateSite()
    {
        final int pageIndex = getActivePage();
        final IWorkbenchPart part = getEditor(pageIndex);

        if (part instanceof IEmbeddedWorkbenchPart) {
            ((IEmbeddedWorkbenchPart) part).activatePart();
        }
    }

    private void disposePart(final IWorkbenchPart part)
    {
        SafeRunner.run(new ISafeRunnable() {
            public void run()
            {
                part.dispose();
            }

            public void handleException(Throwable e)
            {
                // Exception has already being logged by Core. Do nothing.
            }
        });
    }

    public void removePage(int pageIndex)
    {
        assert(pageIndex >= 0 && pageIndex < getPageCount());
        // get editor (if any) before disposing item
        IWorkbenchPart part = getEditor(pageIndex);

        // get control for the item if it's not an editor
        CTabItem item = getItem(pageIndex);
        IServiceLocator pageLocator = null;
        if (item.getData() instanceof IServiceLocator) {
            pageLocator = (IServiceLocator) item.getData();
        }
        Control pageControl = item.getControl();

        // dispose item before disposing editor, in case there's an exception
        // in editor's dispose
        item.dispose();

        if (pageControl != null) {
            pageControl.dispose();
        }

        // dispose editor (if any)
        if (part != null) {
            nestedParts.remove(part);
            disposePart(part);
        }
        if (pageLocator != null) {
            if (pageLocator instanceof IDisposable) {
                ((IDisposable) pageLocator).dispose();
            }
        }
    }

    protected void setActivePage(int pageIndex)
    {
        assert(pageIndex >= 0 && pageIndex < getPageCount());
        getTabFolder().setSelection(pageIndex);
        pageChange(pageIndex);
    }

    protected void setControl(int pageIndex, Control control)
    {
        getItem(pageIndex).setControl(control);
    }

    public void setFocus()
    {
        setFocus(getActivePage());
    }

    private void setFocus(int pageIndex)
    {
        if (pageIndex < 0 || pageIndex >= getPageCount()) {
            // page index out of bounds, don't set focus.
            return;
        }
        final IWorkbenchPart part = getEditor(pageIndex);
        if (part != null) {
            part.setFocus();
        } else {
            // Give the page's control focus.
            final Control control = getControl(pageIndex);
            if (control != null) {
                control.setFocus();
            }
        }
    }

    protected void setPageImage(int pageIndex, Image image)
    {
        getItem(pageIndex).setImage(image);
    }

    protected void setPageText(int pageIndex, String text)
    {
        getItem(pageIndex).setText(text);
    }

    protected void setPageToolTip(int pageIndex, String text)
    {
        getItem(pageIndex).setToolTipText(text);
    }

    public Object getAdapter(Class adapter)
    {
        Object result = super.getAdapter(adapter);
        // restrict delegating to the UI thread for bug 144851
        if (result == null && Display.getCurrent() != null) {
            IWorkbenchPart innerEditor = getActiveEditor();
            // see bug 138823 - prevent some subclasses from causing
            // an infinite loop
            if (innerEditor != null && innerEditor != this) {
                if (adapter.isInstance(innerEditor)) {
                    return innerEditor;
                }
                result = innerEditor.getAdapter(adapter);
            }
        }
        return result;
    }

    public final void setActivePart(IWorkbenchPart part)
    {
        int count = getPageCount();
        for (int i = 0; i < count; i++) {
            IWorkbenchPart pagePart = getEditor(i);
            if (pagePart == part) {
                setActivePage(i);
                break;
            }
        }
    }
}
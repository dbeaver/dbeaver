/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.editors.content;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.*;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.IContentEditorPart;
import org.jkiss.dbeaver.ext.IDataSourceProvider;
import org.jkiss.dbeaver.model.data.DBDContent;
import org.jkiss.dbeaver.model.data.DBDValueController;
import org.jkiss.dbeaver.model.data.DBDValueEditor;
import org.jkiss.dbeaver.model.data.DBDValueHandler;
import org.jkiss.dbeaver.model.navigator.DBNEvent;
import org.jkiss.dbeaver.runtime.RuntimeUtils;
import org.jkiss.dbeaver.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.ColumnInfoPanel;
import org.jkiss.dbeaver.ui.editors.MultiPageDatabaseEditor;

import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

/**
 * LOBEditor
 */
public class ContentEditor extends MultiPageDatabaseEditor<ContentEditorInput> implements IDataSourceProvider, DBDValueEditor, IResourceChangeListener
{
    public static boolean openEditor(DBDValueController valueController, IContentEditorPart[] editorParts)
    {
        ContentEditorInput editorInput;
        // Save data to file
        try {
            LOBInitializer initializer = new LOBInitializer(valueController, editorParts);
            valueController.getValueSite().getWorkbenchWindow().run(true, true, initializer);
            editorInput = initializer.editorInput;
        } catch (Throwable e) {
            if (e instanceof InvocationTargetException) {
                e = ((InvocationTargetException)e).getTargetException();
            }
            UIUtils.showErrorDialog(valueController.getValueSite().getShell(), "Cannot open content editor", null, e);
            return false;
        }
        try {
            valueController.getValueSite().getWorkbenchWindow().getActivePage().openEditor(
                editorInput,
                ContentEditor.class.getName());
        }
        catch (PartInitException e) {
            log.error("Could not open LOB editorPart", e);
            return false;
        }
        return true;
    }

    //public static final long MAX_TEXT_LENGTH = 10 * 1024 * 1024;
    //public static final long MAX_IMAGE_LENGTH = 10 * 1024 * 1024;

    static final Log log = LogFactory.getLog(ContentEditor.class);

    public void refreshDatabaseContent(DBNEvent event) {

    }

    static class ContentPartInfo {
        IContentEditorPart editorPart;
        boolean activated;
        public int index = -1;

        private ContentPartInfo(IContentEditorPart editorPart) {
            this.editorPart = editorPart;
        }
    }

    private static class LOBInitializer implements IRunnableWithProgress {
        DBDValueController valueController;
        IContentEditorPart[] editorParts;
        ContentEditorInput editorInput;

        private LOBInitializer(DBDValueController valueController, IContentEditorPart[] editorParts)
        {
            this.valueController = valueController;
            this.editorParts = editorParts;
        }

        public void run(IProgressMonitor monitor)
            throws InvocationTargetException, InterruptedException
        {
            try {
                editorInput = new ContentEditorInput(
                    valueController,
                    editorParts,
                    RuntimeUtils.makeMonitor(monitor));
            } catch (DBException e) {
                throw new InvocationTargetException(e);
            }
        }
    }

    private boolean valueEditorRegistered = false;

    private List<ContentPartInfo> contentParts = new ArrayList<ContentPartInfo>();
    private ColumnInfoPanel infoPanel;
    private boolean dirty;
    private boolean partsLoaded;
    private boolean saveInProgress;

    public ContentEditor()
    {
    }

    public ContentPartInfo getContentEditor(IEditorPart editor) {
        for (ContentPartInfo contentPart : contentParts) {
            if (contentPart.editorPart == editor) {
                return contentPart;
            }
        }
        return null;
    }

    public void doSave(final IProgressMonitor monitor)
    {
        if (!isDirty()) {
            // Nothing to save
            return;
        }
        // Execute save in UI thread
        getSite().getShell().getDisplay().syncExec(new Runnable() {
            public void run()
            {
                try {
                    // Check for dirty parts
                    final List<IContentEditorPart> dirtyParts = new ArrayList<IContentEditorPart>();
                    for (ContentPartInfo partInfo : contentParts) {
                        if (partInfo.activated && partInfo.editorPart.isDirty()) {
                            dirtyParts.add(partInfo.editorPart);
                        }
                    }

                    IContentEditorPart dirtyPart = null;
                    if (dirtyParts.isEmpty()) {
                        // No modified parts - no additional save required
                    } else if (dirtyParts.size() == 1) {
                        // Single part modified - save it
                        dirtyPart = dirtyParts.get(0);
                    } else {
                        // Multiple parts modified - need to choose one
                        dirtyPart = SelectContentPartDialog.selectContentPart(getSite().getShell(), dirtyParts);
                    }

                    if (dirtyPart != null) {
                        saveInProgress = true;
                        try {
                            dirtyPart.doSave(monitor);
                        }
                        finally {
                            saveInProgress = false;
                        }
                    }
                    // Set dirty flag - if error will occure during content save
                    // then document remains dirty
                    ContentEditor.this.dirty = true;

                    getEditorInput().updateContentFromFile(monitor);

                    // Close editor
                    closeValueEditor();
                }
                catch (Exception e) {
                    UIUtils.showErrorDialog(
                        getSite().getShell(),
                        "Could not save content",
                        "Could not save content to database",
                        e);
                }
            }
        });
    }

    public void init(IEditorSite site, IEditorInput input)
        throws PartInitException
    {
        super.init(site, input);

        getValueController().registerEditor(this);
        valueEditorRegistered = true;

        // Fill nested editorParts info
        IContentEditorPart[] editorParts = getEditorInput().getEditors();
        for (IContentEditorPart editorPart : editorParts) {
            contentParts.add(new ContentPartInfo(editorPart));
            editorPart.initPart(this);
        }

        ResourcesPlugin.getWorkspace().addResourceChangeListener(this);
    }

    public void dispose()
    {
        this.partsLoaded = true;
        ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);

        if (valueEditorRegistered) {
            getValueController().unregisterEditor(this);
            valueEditorRegistered = false;
        }
        if (getEditorInput() != null) {
            // Release LOB input resources
            try {
                getEditorInput().release(VoidProgressMonitor.INSTANCE);
            } catch (Throwable e) {
                log.warn("Error releasing LOB input", e);
            }
        }
        super.dispose();
    }

    public boolean isDirty()
    {
        if (dirty) {
            return true;
        }
        for (ContentPartInfo contentPart : contentParts) {
            if (contentPart.activated && contentPart.editorPart.isDirty()) {
                return true;
            }
        }
        return false;
    }

    public boolean isSaveAsAllowed()
    {
        return false;
    }

    @Override
    protected IEditorSite createSite(IEditorPart editor)
    {
        return new ContentEditorSite(this, editor);
    }

    protected void createPages() {
        DBDContent content = getContent();
        if (content == null) {
            return;
        }
        String contentType = null;
        try {
            contentType = content.getContentType();
        } catch (Exception e) {
            log.error("Could not determine value content type", e);
        }
        long contentLength;
        try {
            contentLength = content.getContentLength();
        } catch (Exception e) {
            log.error("Could not determine value content length", e);
            // Get file length
            contentLength = getEditorInput().getFile().getFullPath().toFile().length();
        }
        MimeType mimeType = null;
        if (contentType != null) {
            try {
                mimeType = new MimeType(contentType);
            } catch (MimeTypeParseException e) {
                log.error("Invalid content MIME type", e);
            }
        }
        IEditorPart defaultPage = null, preferedPage = null;
        for (ContentPartInfo contentPart : contentParts) {
            IContentEditorPart editorPart = contentPart.editorPart;
            if (contentLength > editorPart.getMaxContentLength()) {
                continue;
            }
            if (preferedPage != null && editorPart.isOptionalContent()) {
                // Do not add optional parts if we already have prefered one
                continue;
            }
            try {
                int index = addPage(editorPart, getEditorInput());
                setPageText(index, editorPart.getContentTypeTitle());
                setPageImage(index, editorPart.getContentTypeImage());
                contentPart.activated = true;
                contentPart.index = index;
                // Check MIME type
                if (mimeType != null && mimeType.getPrimaryType().equals(editorPart.getPreferedMimeType())) {
                    defaultPage = editorPart;
                }
                if (editorPart.isPreferedContent()) {
                    preferedPage = editorPart;
                }
            } catch (PartInitException e) {
                log.error(e);
            }
        }
        if (preferedPage != null) {
            // Remove all optional pages
            for (ContentPartInfo contentPart : contentParts) {
                if (contentPart.activated && contentPart.editorPart != preferedPage && contentPart.editorPart.isOptionalContent()) {
                    removePage(contentPart.index);
                }
            }

            // Set default page
            setActiveEditor(preferedPage);
        } else if (defaultPage != null) {
            setActiveEditor(defaultPage);
        }

        this.partsLoaded = true;
    }

    public void removePage(int pageIndex) {
        for (ContentPartInfo contentPart : contentParts) {
            if (contentPart.index == pageIndex) {
                contentPart.index = -1;
            } else if (contentPart.index > pageIndex) {
                contentPart.index--;
            }
        }
        super.removePage(pageIndex);
    }

    protected Composite createPageContainer(Composite parent)
    {
        Composite panel = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout(1, false);
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        layout.verticalSpacing = 0;
        layout.horizontalSpacing = 0;
        panel.setLayout(layout);
        GridData gd = new GridData(GridData.FILL_BOTH);
        panel.setLayoutData(gd);

        {
            infoPanel = new ColumnInfoPanel(panel, SWT.NONE, getValueController());
            gd = new GridData(GridData.FILL_HORIZONTAL);
            gd.exclude = true;
            infoPanel.setLayoutData(gd);
            infoPanel.setVisible(false);
        }

        Composite editotPanel = new Composite(panel, SWT.NONE);
        layout = new GridLayout(1, false);
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        layout.verticalSpacing = 0;
        layout.horizontalSpacing = 0;
        editotPanel.setLayout(layout);
        gd = new GridData(GridData.FILL_BOTH);
        editotPanel.setLayoutData(gd);

        return editotPanel;
    }

    void toggleInfoBar()
    {
        boolean visible = infoPanel.isVisible();
        visible = !visible;
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.exclude = !visible;
        infoPanel.setLayoutData(gd);
        infoPanel.setVisible(visible);
        infoPanel.getParent().layout();
    }

    DBDValueHandler getValueHandler()
    {
        DBDValueController valueController = getValueController();
        return valueController == null ? null : valueController.getValueHandler();
    }

    DBDContent getContent()
    {
        Object value = getValueController().getValue();
        if (value instanceof DBDContent) {
            return (DBDContent) value;
        } else {
            return null;
        }
    }

    public DBDValueController getValueController()
    {
        ContentEditorInput input = getEditorInput();
        return input == null ? null : input.getValueController();
    }

    public void showValueEditor()
    {
        this.getEditorSite().getWorkbenchWindow().getActivePage().activate(this);
    }

    public void closeValueEditor()
    {
        IWorkbenchPage workbenchPage = this.getEditorSite().getWorkbenchWindow().getActivePage();
        if (workbenchPage != null) {
            workbenchPage.closeEditor(this, false);
        } else {
            // Special case - occured when entire workbench is closed
            // We need to unregister editor and release all resource here
            if (valueEditorRegistered) {
                getValueController().unregisterEditor(this);
                valueEditorRegistered = false;
            }
        }
    }

    public void setFocus()
    {
    }

    public void resourceChanged(IResourceChangeEvent event)
    {
        if (!partsLoaded || saveInProgress) {
            // No content change before all parts are loaded
            return;
        }
        IResourceDelta delta= event.getDelta();
        if (delta == null) {
            return;
        }
        delta = delta.findMember(getEditorInput().getPath());
        if (delta == null) {
            return;
        }
        if (delta.getKind() == IResourceDelta.CHANGED &&
            (delta.getFlags() & IResourceDelta.CONTENT) != 0)
        {
            // Content was changed somehow so mark editor as dirty
            dirty = true;
            getSite().getShell().getDisplay().asyncExec(new Runnable() {
                public void run()
                {
                    firePropertyChange(PROP_DIRTY);
                }
            });
        }
    }

}
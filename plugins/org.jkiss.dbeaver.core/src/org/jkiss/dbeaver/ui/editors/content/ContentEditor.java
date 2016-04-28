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
package org.jkiss.dbeaver.ui.editors.content;

import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.*;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.data.DBDContent;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.dbeaver.ui.data.IValueController;
import org.jkiss.dbeaver.ui.data.IValueEditorStandalone;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.ColumnInfoPanel;
import org.jkiss.dbeaver.ui.editors.MultiPageAbstractEditor;
import org.jkiss.dbeaver.utils.ContentUtils;

import javax.activation.MimeType;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

/**
 * LOBEditor
 */
public class ContentEditor extends MultiPageAbstractEditor implements IValueEditorStandalone, IResourceChangeListener
{
    @Override
    public ContentEditorInput getEditorInput()
    {
        return (ContentEditorInput)super.getEditorInput();
    }

    @Nullable
    public static ContentEditor openEditor(IValueController valueController, ContentEditorPart[] editorParts)
    {
        ContentEditorInput editorInput;
        // Save data to file
        try {
            LOBInitializer initializer = new LOBInitializer(valueController, editorParts, null);
            //valueController.getValueSite().getWorkbenchWindow().run(true, true, initializer);
            DBeaverUI.runInProgressService(initializer);
            editorInput = initializer.editorInput;
        } catch (Throwable e) {
            if (e instanceof InvocationTargetException) {
                e = ((InvocationTargetException)e).getTargetException();
            }
            UIUtils.showErrorDialog(valueController.getValueSite().getShell(), "Cannot open content editor", null, e);
            return null;
        }
        try {
            return (ContentEditor) valueController.getValueSite().getWorkbenchWindow().getActivePage().openEditor(
                editorInput,
                ContentEditor.class.getName());
        }
        catch (PartInitException e) {
            log.error("Can't open CONTENT editorPart", e);
            return null;
        }
    }

    //public static final long MAX_TEXT_LENGTH = 10 * 1024 * 1024;
    //public static final long MAX_IMAGE_LENGTH = 10 * 1024 * 1024;

    private static final Log log = Log.getLog(ContentEditor.class);

    static class ContentPartInfo {
        ContentEditorPart editorPart;
        boolean activated;
        public int index = -1;

        private ContentPartInfo(ContentEditorPart editorPart) {
            this.editorPart = editorPart;
        }
    }

    private static class LOBInitializer implements DBRRunnableWithProgress {
        IValueController valueController;
        ContentEditorPart[] editorParts;
        ContentEditorInput editorInput;

        private LOBInitializer(IValueController valueController, ContentEditorPart[] editorParts, @Nullable ContentEditorInput editorInput)
        {
            this.valueController = valueController;
            this.editorParts = editorParts;
            this.editorInput = editorInput;
        }

        @Override
        public void run(DBRProgressMonitor monitor) throws InvocationTargetException, InterruptedException
        {
            try {
                if (editorInput == null) {
                    editorInput = new ContentEditorInput(
                        valueController,
                        editorParts,
                        monitor);
                } else {
                    editorInput.refreshContent(monitor, valueController);
                }
            } catch (DBException e) {
                throw new InvocationTargetException(e);
            }
        }

    }

    private List<ContentPartInfo> contentParts = new ArrayList<>();
    private ColumnInfoPanel infoPanel;
    private boolean dirty;
    private boolean partsLoaded;
    private boolean saveInProgress;

    public ContentEditor()
    {
    }

    @Nullable
    public ContentPartInfo getContentEditor(IEditorPart editor) {
        for (ContentPartInfo contentPart : contentParts) {
            if (contentPart.editorPart == editor) {
                return contentPart;
            }
        }
        return null;
    }

    @Override
    public void doSave(final IProgressMonitor monitor)
    {
        if (!isDirty()) {
            // Nothing to save
            return;
        }
        // Execute save in UI thread
        UIUtils.runInUI(getSite().getShell(), new Runnable() {
            @Override
            public void run()
            {
                try {
                    // Check for dirty parts
                    final List<ContentEditorPart> dirtyParts = new ArrayList<>();
                    for (ContentPartInfo partInfo : contentParts) {
                        if (partInfo.activated && partInfo.editorPart.isDirty()) {
                            dirtyParts.add(partInfo.editorPart);
                        }
                    }

                    ContentEditorPart dirtyPart = null;
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
                    // Set dirty flag - if error will occur during content save
                    // then document remains dirty
                    ContentEditor.this.dirty = true;

                    ContentEditorInput editorInput = getEditorInput();
                    editorInput.updateContentFromFile(monitor);
                    editorInput.getValueController().updateValue(editorInput.getContent());

                    // Close editor
                    closeValueEditor();
                }
                catch (Exception e) {
                    UIUtils.showErrorDialog(
                        getSite().getShell(),
                        "Can't save content",
                        "Can't save content to database",
                        e);
                }
            }
        });
    }

    @Override
    public void doSaveAs()
    {

    }

    @Override
    public void init(IEditorSite site, IEditorInput input)
        throws PartInitException
    {
        super.init(site, input);
        setPartName(input.getName());

        DBDContent content = getContent();
        if (content == null) {
            return;
        }

        MimeType mimeType = ContentUtils.getMimeType(content.getContentType());

        // Fill nested editorParts info
        ContentEditorPart[] editorParts = getEditorInput().getEditors();
        for (ContentEditorPart editorPart : editorParts) {
            contentParts.add(new ContentPartInfo(editorPart));
            editorPart.initPart(this, mimeType);
        }

        ResourcesPlugin.getWorkspace().addResourceChangeListener(this);
    }

    @Override
    public void dispose()
    {
        this.partsLoaded = true;
        ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);

        if (getEditorInput() != null) {
            // Release CONTENT input resources
            try {
                getEditorInput().release(VoidProgressMonitor.INSTANCE);
            } catch (Throwable e) {
                log.warn("Error releasing CONTENT input", e);
            }
        }
        super.dispose();
    }

    @Override
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

    @Override
    public boolean isSaveAsAllowed()
    {
        return false;
    }

    @Override
    protected IEditorSite createSite(IEditorPart editor)
    {
        return new ContentEditorSite(this, editor);
    }

    @Override
    protected void createPages() {
        super.createPages();
        DBDContent content = getContent();
        if (content == null) {
            return;
        }
        String contentType = null;
        try {
            contentType = content.getContentType();
        } catch (Exception e) {
            log.error("Can't determine value content type", e);
        }
        long contentLength;
        try {
            contentLength = content.getContentLength();
        } catch (Exception e) {
            log.warn("Can't determine value content length", e);
            // Get file length
            contentLength = getEditorInput().getContentFile().length();
        }
        MimeType mimeType = ContentUtils.getMimeType(contentType);
        IEditorPart defaultPage = null, preferredPage = null;
        for (ContentPartInfo contentPart : contentParts) {
            ContentEditorPart editorPart = contentPart.editorPart;
            if (contentLength > editorPart.getMaxContentLength()) {
                continue;
            }
            if (preferredPage != null && editorPart.isOptionalContent()) {
                // Do not add optional parts if we already have prefered one
                continue;
            }
            try {
                int index = addPage(editorPart, getEditorInput());
                setPageText(index, editorPart.getContentTypeTitle());
                setPageImage(index, DBeaverIcons.getImage(editorPart.getContentTypeImage()));
                contentPart.activated = true;
                contentPart.index = index;
                // Check MIME type
                if (mimeType != null && mimeType.getPrimaryType().equals(editorPart.getPreferredMimeType())) {
                    defaultPage = editorPart;
                }
                if (editorPart.isPreferredContent()) {
                    preferredPage = editorPart;
                }
            } catch (PartInitException e) {
                log.error(e);
            }
        }
        if (preferredPage != null) {
            // Remove all optional pages
            for (ContentPartInfo contentPart : contentParts) {
                if (contentPart.activated && contentPart.editorPart != preferredPage && contentPart.editorPart.isOptionalContent()) {
                    removePage(contentPart.index);
                }
            }

            // Set default page
            setActiveEditor(preferredPage);
        } else if (defaultPage != null) {
            setActiveEditor(defaultPage);
        }

        this.partsLoaded = true;
    }

    @Override
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

    @Override
    protected Composite createPageContainer(Composite parent)
    {
        Composite panel = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout(1, false);
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        layout.verticalSpacing = 0;
        layout.horizontalSpacing = 0;
        panel.setLayout(layout);
        if (parent.getLayout() instanceof GridLayout) {
            panel.setLayoutData(new GridData(GridData.FILL_BOTH));
        }

        {
            IValueController valueController = getValueController();
            assert valueController != null;
            infoPanel = new ColumnInfoPanel(panel, SWT.NONE, valueController);
            GridData gd = new GridData(GridData.FILL_HORIZONTAL);
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
        editotPanel.setLayoutData(new GridData(GridData.FILL_BOTH));

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

    @Nullable
    DBDContent getContent()
    {
        IValueController valueController = getValueController();
        Object value = valueController == null? null : valueController.getValue();
        if (value instanceof DBDContent) {
            return (DBDContent) value;
        } else {
            return null;
        }
    }

    @Nullable
    public IValueController getValueController()
    {
        ContentEditorInput input = getEditorInput();
        return input == null ? null : input.getValueController();
    }

    @Override
    public void createControl() {

    }

    @Override
    public Control getControl()
    {
        int activePage = getActivePage();
        return activePage < 0 ? null : getControl(activePage);
    }

    @Override
    public Object extractEditorValue() throws DBException
    {
        DBeaverUI.runInUI(DBeaverUI.getActiveWorkbenchWindow(), new DBRRunnableWithProgress() {
            @Override
            public void run(DBRProgressMonitor monitor) throws InvocationTargetException, InterruptedException
            {
                try {
                    getEditorInput().updateContentFromFile(RuntimeUtils.getNestedMonitor(monitor));
                } catch (DBException e) {
                    throw new InvocationTargetException(e);
                }
            }
        });

        return getEditorInput().getContent();
    }

    @Override
    public void primeEditorValue(@Nullable Object value) throws DBException
    {
        IValueController valueController = getEditorInput().getValueController();
        LOBInitializer initializer = new LOBInitializer(valueController, getEditorInput().getEditors(), getEditorInput());
        try {
            //valueController.getValueSite().getWorkbenchWindow().run(true, true, initializer);
            DBeaverUI.runInProgressService(initializer);
        } catch (InvocationTargetException e) {
            UIUtils.showErrorDialog(valueController.getValueSite().getShell(), "Cannot refresh content editor", null, e);
        } catch (InterruptedException e) {
            // ignore
        }
    }

    @Override
    public void showValueEditor()
    {
        this.getEditorSite().getWorkbenchWindow().getActivePage().activate(this);
    }

    @Override
    public void closeValueEditor()
    {
        IWorkbenchPage workbenchPage = this.getEditorSite().getWorkbenchWindow().getActivePage();
        if (workbenchPage != null) {
            workbenchPage.closeEditor(this, false);
        }
    }

    @Override
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
        delta = delta.findMember(ContentUtils.convertPathToWorkspacePath(getEditorInput().getPath()));
        if (delta == null) {
            return;
        }
        if (delta.getKind() == IResourceDelta.CHANGED &&
            (delta.getFlags() & IResourceDelta.CONTENT) != 0)
        {
            // Content was changed somehow so mark editor as dirty
            dirty = true;
            getSite().getShell().getDisplay().asyncExec(new Runnable() {
                @Override
                public void run()
                {
                    firePropertyChange(PROP_DIRTY);
                }
            });
        }
    }

}
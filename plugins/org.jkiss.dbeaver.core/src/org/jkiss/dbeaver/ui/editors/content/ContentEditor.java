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
package org.jkiss.dbeaver.ui.editors.content;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.IContributionManager;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.*;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.model.data.DBDContent;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.runtime.ui.DBUserInterface;
import org.jkiss.dbeaver.ui.IRefreshablePart;
import org.jkiss.dbeaver.ui.data.IStreamValueManager;
import org.jkiss.dbeaver.ui.data.IValueController;
import org.jkiss.dbeaver.ui.data.IValueEditorStandalone;
import org.jkiss.dbeaver.ui.data.registry.StreamValueManagerDescriptor;
import org.jkiss.dbeaver.ui.data.registry.ValueManagerRegistry;
import org.jkiss.dbeaver.ui.dialogs.ColumnInfoPanel;
import org.jkiss.dbeaver.ui.dialogs.DialogUtils;
import org.jkiss.dbeaver.ui.editors.MultiPageAbstractEditor;
import org.jkiss.dbeaver.utils.RuntimeUtils;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * LOBEditor
 */
public class ContentEditor extends MultiPageAbstractEditor implements IValueEditorStandalone, IRefreshablePart
{
    @Override
    public ContentEditorInput getEditorInput()
    {
        return (ContentEditorInput)super.getEditorInput();
    }

    @Nullable
    public static ContentEditor openEditor(IValueController valueController, DBDContent content)
    {
        ContentEditorInput editorInput;
        // Save data to file
        try {
            LOBInitializer initializer = new LOBInitializer(
                valueController,
                content);
            //valueController.getValueSite().getWorkbenchWindow().run(true, true, initializer);
            DBeaverUI.runInProgressService(initializer);
            editorInput = initializer.editorInput;
        } catch (Throwable e) {
            if (e instanceof InvocationTargetException) {
                e = ((InvocationTargetException)e).getTargetException();
            }
            DBUserInterface.getInstance().showError("Cannot open content editor", null, e);
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
        IEditorPart editorPart;
        boolean isDefault;
        boolean activated;
        public int index = -1;

        private ContentPartInfo(IEditorPart editorPart, boolean isDefault) {
            this.editorPart = editorPart;
            this.isDefault = isDefault;
        }
    }

    private static class LOBInitializer implements DBRRunnableWithProgress {
        IValueController valueController;
        DBDContent content;
        IEditorPart[] editorParts;
        IEditorPart defaultPart;
        ContentEditorInput editorInput;

        public LOBInitializer(IValueController valueController, DBDContent content) {
            this.valueController = valueController;
            this.content = content;
        }

        private LOBInitializer(IValueController valueController, IEditorPart[] editorParts, IEditorPart defaultPart, @Nullable ContentEditorInput editorInput)
        {
            this.valueController = valueController;
            this.editorParts = editorParts;
            this.defaultPart = defaultPart;
            this.editorInput = editorInput;
        }

        @Override
        public void run(DBRProgressMonitor monitor) throws InvocationTargetException, InterruptedException
        {
            try {
                if (content != null && editorParts == null) {
                    Map<StreamValueManagerDescriptor, IStreamValueManager.MatchType> streamManagers =
                        ValueManagerRegistry.getInstance().getApplicableStreamManagers(monitor, valueController.getValueType(), content);
                    List<IEditorPart> parts = new ArrayList<>();
                    IStreamValueManager.MatchType defaultMatch = null;
                    for (Map.Entry<StreamValueManagerDescriptor, IStreamValueManager.MatchType> entry : streamManagers.entrySet()) {
                        IStreamValueManager streamValueManager = entry.getKey().getInstance();
                        try {
                            IEditorPart editorPart = streamValueManager.createEditorPart(valueController);
                            IStreamValueManager.MatchType matchType = entry.getValue();
                            if (defaultPart == null) {
                                defaultPart = editorPart;
                                defaultMatch = matchType;
                            } else {
                                boolean setDefault = false;
                                switch (matchType) {
                                    case EXCLUSIVE:
                                    case PRIMARY:
                                        setDefault = true;
                                        break;
                                    case DEFAULT:
                                        setDefault = (defaultMatch == IStreamValueManager.MatchType.APPLIES);
                                        break;
                                    default:
                                        break;
                                }
                                if (setDefault) {
                                    defaultPart = editorPart;
                                    defaultMatch = matchType;
                                }
                            }
                            parts.add(editorPart);
                        } catch (DBException e) {
                            log.error(e);
                        }
                    }
                    editorParts = parts.toArray(new IEditorPart[parts.size()]);
                }

                if (editorInput == null) {
                    editorInput = new ContentEditorInput(
                        valueController,
                        editorParts,
                        defaultPart,
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

    @Override
    public void doSave(final IProgressMonitor monitor)
    {
        if (!isDirty()) {
            // Nothing to save
            return;
        }
        // Execute save in UI thread
        DBeaverUI.syncExec(new Runnable() {
            @Override
            public void run() {
                try {
                    // Check for dirty parts
                    final List<IEditorPart> dirtyParts = new ArrayList<>();
                    for (ContentPartInfo partInfo : contentParts) {
                        if (partInfo.activated && partInfo.editorPart.isDirty()) {
                            dirtyParts.add(partInfo.editorPart);
                        }
                    }

                    IEditorPart dirtyPart = null;
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
                        } finally {
                            saveInProgress = false;
                        }
                    }
                    // Set dirty flag - if error will occur during content save
                    // then document remains dirty
                    ContentEditor.this.dirty = true;

                    ContentEditorInput editorInput = getEditorInput();
                    editorInput.updateContentFromFile(monitor);
                    editorInput.getValueController().updateValue(editorInput.getContent(), true);

                    // Close editor
                    closeValueEditor();
                } catch (Exception e) {
                    DBUserInterface.getInstance().showError(
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
        Shell shell = getSite().getShell();
        final File saveFile = DialogUtils.selectFileForSave(shell, getPartName());
        if (saveFile == null) {
            return;
        }
        try {
            getSite().getWorkbenchWindow().run(true, true, new IRunnableWithProgress() {
                @Override
                public void run(IProgressMonitor monitor)
                        throws InvocationTargetException, InterruptedException
                {
                    try {
                        getEditorInput().saveToExternalFile(saveFile, monitor);
                    }
                    catch (CoreException e) {
                        throw new InvocationTargetException(e);
                    }
                }
            });
        }
        catch (InvocationTargetException e) {
            DBUserInterface.getInstance().showError(
                    "Can't save content",
                    "Can't save content to file '" + saveFile.getAbsolutePath() + "'",
                    e.getTargetException());
        }
        catch (InterruptedException e) {
            // do nothing
        }
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

        // Fill nested editorParts info
        IEditorPart[] editorParts = getEditorInput().getEditors();
        for (IEditorPart editorPart : editorParts) {
            contentParts.add(new ContentPartInfo(editorPart, editorPart == getEditorInput().getDefaultEditor()));
            //editorPart.init(site, input);
        }

        //ResourcesPlugin.getWorkspace().addResourceChangeListener(this);
    }

    @Override
    public void dispose()
    {
        this.partsLoaded = true;
        //ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);

        if (getEditorInput() != null) {
            // Release CONTENT input resources
            try {
                getEditorInput().release();
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
    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }

    @Override
    public boolean isSaveAsAllowed()
    {
        return true;
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
        ContentPartInfo defaultPage = null;
        for (ContentPartInfo contentPart : contentParts) {
            if (contentPart.isDefault) {
                defaultPage = contentPart;
            }
            IEditorPart editorPart = contentPart.editorPart;
            try {
                int index = addPage(editorPart, getEditorInput());
                setPageText(index, editorPart.getTitle());
                setPageImage(index, editorPart.getTitleImage());
                contentPart.activated = true;
                contentPart.index = index;
            } catch (PartInitException e) {
                log.error(e);
            }
        }
        if (defaultPage != null) {
            setActiveEditor(defaultPage.editorPart);
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
        DBeaverUI.runInUI(new DBRRunnableWithProgress() {
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
        ContentEditorInput input = getEditorInput();
        IValueController valueController = input.getValueController();
        LOBInitializer initializer = new LOBInitializer(valueController, input.getEditors(), input.getDefaultEditor(), input);
        try {
            //valueController.getValueSite().getWorkbenchWindow().run(true, true, initializer);
            DBeaverUI.runInProgressService(initializer);
        } catch (InvocationTargetException e) {
            DBUserInterface.getInstance().showError("Cannot refresh content editor", null, e);
        } catch (InterruptedException e) {
            // ignore
        }
    }

    @Override
    public void contributeActions(@NotNull IContributionManager manager, @NotNull IValueController controller) throws DBCException {

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
    public void refreshPart(Object source, boolean force) {
        getEditorInput().refreshContentParts(source);
        fireContentChanged();
    }

    public void fireContentChanged() {
        firePropertyChange(ContentEditor.PROP_DIRTY);
    }

/*
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
            DBeaverUI.asyncExec(new Runnable() {
                @Override
                public void run() {
                    firePropertyChange(PROP_DIRTY);
                }
            });
        }
    }
*/

}
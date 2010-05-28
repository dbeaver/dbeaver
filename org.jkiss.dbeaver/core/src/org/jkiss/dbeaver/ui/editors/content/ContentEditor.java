/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.editors.content;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.MultiPageEditorPart;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.IContentEditorPart;
import org.jkiss.dbeaver.ext.ui.IDataSourceUser;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.data.DBDContent;
import org.jkiss.dbeaver.model.data.DBDContentCharacter;
import org.jkiss.dbeaver.model.data.DBDValueController;
import org.jkiss.dbeaver.model.data.DBDValueEditor;
import org.jkiss.dbeaver.model.data.DBDValueHandler;
import org.jkiss.dbeaver.model.dbc.DBCSession;
import org.jkiss.dbeaver.model.struct.DBSDataSourceContainer;
import org.jkiss.dbeaver.ui.DBIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.ColumnInfoPanel;
import org.jkiss.dbeaver.utils.DBeaverUtils;

import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

/**
 * LOBEditor
 */
public class ContentEditor extends MultiPageEditorPart implements IDataSourceUser, DBDValueEditor, IResourceChangeListener
{
    public static final long MAX_TEXT_LENGTH = 10 * 1024 * 1024;
    public static final long MAX_IMAGE_LENGTH = 10 * 1024 * 1024;

    static Log log = LogFactory.getLog(ContentEditor.class);

    private boolean valueEditorRegistered = false;

    private List<ContentPartInfo> contentParts = new ArrayList<ContentPartInfo>();
    private ColumnInfoPanel infoPanel;
    private boolean dirty;
    private boolean partsLoaded;

    static class ContentPartInfo {
        IContentEditorPart editorPart;
        boolean activated;

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
                editorInput = new ContentEditorInput(valueController, editorParts, monitor);
            } catch (CoreException e) {
                throw new InvocationTargetException(e);
            }
        }
    }

    public ContentEditor()
    {
    }

    @Override
    public ContentEditorInput getEditorInput() {
        return (ContentEditorInput)super.getEditorInput();
    }

    public ContentPartInfo getContentEditor(IEditorPart editor) {
        for (ContentPartInfo contentPart : contentParts) {
            if (contentPart.editorPart == editor) {
                return contentPart;
            }
        }
        return null;
    }

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
            log.error("Could not init LOB data", e);
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

    public void doSave(IProgressMonitor monitor)
    {
        try {
            getEditorInput().updateContentFromFile(monitor);
            this.dirty = false;
            getSite().getShell().getDisplay().asyncExec(new Runnable() {
                public void run()
                {
                    firePropertyChange(PROP_DIRTY);
                }
            });
        }
        catch (Exception e) {
            DBeaverUtils.showErrorDialog(
                getSite().getShell(),
                "Could not save content",
                "Could not save content to database",
                e);
        }
    }

    public void doSaveAs()
    {
    }

    public void init(IEditorSite site, IEditorInput input)
        throws PartInitException
    {
        if (!(input instanceof ContentEditorInput)) {
            throw new PartInitException("Invalid Input: Must be ContentEditorInput");
        }

        setSite(site);
        setInput(input);
        setPartName(input.getName());
        setTitleImage(input.getImageDescriptor().createImage());

        getValueController().registerEditor(this);
        valueEditorRegistered = true;

        // Fill nested editorParts info
        IContentEditorPart[] editorParts = getEditorInput().getEditors();
        for (IContentEditorPart editorPart : editorParts) {
            contentParts.add(new ContentPartInfo(editorPart));
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
                getEditorInput().release(new NullProgressMonitor());
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
        int defaultPage = -1;
        for (ContentPartInfo contentPart : contentParts) {
            IContentEditorPart editorPart = contentPart.editorPart;
            if (contentLength > editorPart.getMaxContentLength()) {
                continue;
            }
            try {
                int index = addPage(contentPart.editorPart, getEditorInput());
                setPageText(index, editorPart.getContentTypeTitle());
                setPageImage(index, editorPart.getContentTypeImage());
                contentPart.activated = true;
                // Check MIME type
                if (mimeType != null && mimeType.getPrimaryType().equals(editorPart.getPreferedMimeType())) {
                    defaultPage = index;
                }
            } catch (PartInitException e) {
                log.error(e);
            }
        }
        if (defaultPage != -1) {
            setActivePage(defaultPage);
        }

        this.partsLoaded = true;
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

    private void createToolbar(Composite panel) {
        Composite toolbarGroup = new Composite(panel, SWT.NONE);
        {
            GridData gd = new GridData(GridData.FILL_HORIZONTAL);
            gd.horizontalIndent = 0;
            gd.verticalIndent = 0;
            toolbarGroup.setLayoutData(gd);
            GridLayout layout = new GridLayout(3, false);
            layout.marginHeight = 0;
            layout.marginWidth = 0;
            toolbarGroup.setLayout(layout);
        }

        Object value = getValueController().getValue();
        DBDContent content = getContent();
        try {
            Composite contentGroup = new Composite(toolbarGroup, SWT.NONE);

            GridData gd = new GridData(GridData.FILL_HORIZONTAL);
            contentGroup.setLayoutData(gd);
            RowLayout layout = new RowLayout(SWT.HORIZONTAL);
            layout.center = true;
            layout.spacing = 5;
            contentGroup.setLayout(layout);

            // Content length
            Label label = new Label(contentGroup, SWT.NONE);
            label.setText("Content Length:");
            Text text = new Text(contentGroup, SWT.BORDER | SWT.READ_ONLY);
            if (content != null) {
                text.setText(String.valueOf(content.getContentLength()));
            }

            // Content type
            label = new Label(contentGroup, SWT.NONE);
            label.setText("Content Type:");
            Text ctText = new Text(contentGroup, SWT.BORDER | SWT.READ_ONLY);
            if (content != null) {
                String contentType = content.getContentType();
                ctText.setText(contentType == null ? "unknown" : contentType);
            }

            // Content sub type
            label = new Label(contentGroup, SWT.NONE);
            label.setText("Content Encoding:");
            Text encodingText = new Text (contentGroup, SWT.BORDER | SWT.READ_ONLY);
            if (content instanceof DBDContentCharacter) {
                String contentEncoding = ((DBDContentCharacter)content).getCharset();
                encodingText.setText(contentEncoding == null ? "" : contentEncoding);
            }
/*
            encodingText.setVisibleItemCount(30);
            SortedMap<String,Charset> charsetMap = Charset.availableCharsets();
            int index = 0;
            int defIndex = -1;
            for (String csName : charsetMap.keySet()) {
                Charset charset = charsetMap.get(csName);
                encodingText.add(charset.displayName());
                if (charset.equals(Charset.defaultCharset())) {
                    defIndex = index;
                }
                index++;
            }
            if (defIndex >= 0) {
                encodingText.select(defIndex);
            }
*/
        }
        catch (Exception e) {
            log.error("Could not initialize LOB editorPart toolbar", e);
        }

        {
            ToolBar toolBar = new ToolBar(toolbarGroup, SWT.FLAT);

            GridData gd = new GridData();
            gd.horizontalAlignment = SWT.RIGHT;
            toolBar.setLayoutData(gd);

            UIUtils.createToolItem(toolBar, "Save To File", DBIcon.SAVE, new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e)
                {
                }
            });
            UIUtils.createToolItem(toolBar, "Load from File", DBIcon.LOAD, new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e)
                {
                }
            });
            UIUtils.createSeparator(toolBar);
            UIUtils.createToolItem(toolBar, "Column Info", DBIcon.INFO, new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e)
                {
                }
            });
            UIUtils.createSeparator(toolBar);
            UIUtils.createToolItem(toolBar, "Apply Changes", DBIcon.ACCEPT, new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e)
                {
                }
            });
            UIUtils.createToolItem(toolBar, "Close", DBIcon.REJECT, new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e)
                {
                }
            });
        }
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
            if (valueEditorRegistered) {
                getValueController().unregisterEditor(this);
                valueEditorRegistered = false;
            }
        }
    }

    public void setFocus()
    {
    }

    public DBSDataSourceContainer getDataSourceContainer() {
        DBPDataSource dataSource = getDataSource();
        return dataSource == null ? null : dataSource.getContainer();
    }

    public DBPDataSource getDataSource() {
        try {
            return getSession().getDataSource();
        }
        catch (DBException e) {
            log.error("Could not obtain session reference", e);
            return null;
        }
    }

    public DBCSession getSession() throws DBException {
        DBDValueController valueController = getValueController();
        if (valueController == null) {
            throw new DBException("No value controller");
        }
        return valueController.getSession();
    }

    public void resourceChanged(IResourceChangeEvent event)
    {
        if (!partsLoaded) {
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
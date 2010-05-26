/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.editors.lob;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
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
import org.eclipse.ui.IEditorActionBarContributor;
import org.eclipse.ui.editors.text.TextEditorActionContributor;
import org.eclipse.ui.part.EditorActionBarContributor;
import org.eclipse.ui.part.MultiPageEditorPart;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.ui.IDataSourceUser;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.data.DBDStreamHandler;
import org.jkiss.dbeaver.model.data.DBDValueController;
import org.jkiss.dbeaver.model.data.DBDValueEditor;
import org.jkiss.dbeaver.model.data.DBDValueHandler;
import org.jkiss.dbeaver.model.dbc.DBCSession;
import org.jkiss.dbeaver.model.struct.DBSDataSourceContainer;
import org.jkiss.dbeaver.ui.DBIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.hex.HexEditor;
import org.jkiss.dbeaver.ui.editors.hex.HexEditorActionBarContributor;

import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

/**
 * LOBEditor
 */
public class LOBEditor extends MultiPageEditorPart implements IDataSourceUser, DBDValueEditor
{
    public static final long MAX_TEXT_LENGTH = 10 * 1024 * 1024;
    public static final long MAX_IMAGE_LENGTH = 10 * 1024 * 1024;

    static Log log = LogFactory.getLog(LOBEditor.class);

    private LOBEditorInput lobInput;
    private boolean valueEditorRegistered = false;

    private List<ContentEditor> contentEditors = new ArrayList<ContentEditor>();

    static class ContentEditor {
        IEditorPart editor;
        IEditorActionBarContributor actionBarContributor;
        String title;
        String tollTip;
        Image image;
        String preferedMimeType;
        long maxContentLength;
        boolean activated;

        private ContentEditor(IEditorPart editor, EditorActionBarContributor actionBarContributor, String title,
                              String tollTip, Image image, String preferedMimeType, long maxContentLength) {
            this.editor = editor;
            this.actionBarContributor = actionBarContributor;
            this.title = title;
            this.tollTip = tollTip;
            this.image = image;
            this.preferedMimeType = preferedMimeType;
            this.maxContentLength = maxContentLength;
        }
    }

    private static class LOBInitializer implements IRunnableWithProgress {
        DBDValueController valueController;
        LOBEditorInput editorInput;

        private LOBInitializer(DBDValueController valueController)
        {
            this.valueController = valueController;
        }

        public void run(IProgressMonitor monitor)
            throws InvocationTargetException, InterruptedException
        {
            try {
                editorInput = new LOBEditorInput(valueController, monitor);
            } catch (CoreException e) {
                throw new InvocationTargetException(e);
            }
        }
    }

    public LOBEditor()
    {
        contentEditors.add(new ContentEditor(
            new HexEditor(),
            new HexEditorActionBarContributor(), "Binary",
            "Binary Editor",
            DBIcon.HEX.getImage(),
            "application",
            Long.MAX_VALUE));
        contentEditors.add(new ContentEditor(
            new LOBTextEditor(),
            new TextEditorActionContributor(), "Text",
            "Text Editor",
            DBIcon.TEXT.getImage(),
            "text",
            MAX_TEXT_LENGTH));
        contentEditors.add(new ContentEditor(
            new LOBImageEditor(),
            null, "Image",
            "Image Editor",
            DBIcon.IMAGE.getImage(),
            "image",
            MAX_IMAGE_LENGTH));
    }

    @Override
    public LOBEditorInput getEditorInput() {
        return (LOBEditorInput)super.getEditorInput();
    }

    public ContentEditor getContentEditor(IEditorPart editor) {
        for (ContentEditor contentEditor : contentEditors) {
            if (contentEditor.editor == editor) {
                return contentEditor;
            }
        }
        return null;
    }

    public static boolean openEditor(DBDValueController valueController)
    {
        LOBEditorInput editorInput;
        // Save data to file
        try {
            LOBInitializer initializer = new LOBInitializer(valueController);
            valueController.getValueSite().getWorkbenchWindow().run(false, true, initializer);
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
            LOBEditor.class.getName());
        }
        catch (PartInitException e) {
            log.error("Could not open LOB editor", e);
            return false;
        }
        return true;
    }

    public void doSave(IProgressMonitor monitor)
    {
    }

    public void doSaveAs()
    {
    }

    public void init(IEditorSite site, IEditorInput input)
        throws PartInitException
    {
        if (!(input instanceof LOBEditorInput)) {
            throw new PartInitException("Invalid Input: Must be LOBEditorInput");
        }

        this.lobInput = (LOBEditorInput)input;

        setSite(site);
        setInput(input);
        setPartName(this.lobInput.getName());
        setTitleImage(this.lobInput.getImageDescriptor().createImage());

        getValueController().registerEditor(this);
        valueEditorRegistered = true;
    }

    public void dispose()
    {
        if (valueEditorRegistered) {
            getValueController().unregisterEditor(this);
            valueEditorRegistered = false;
        }
        if (lobInput != null) {
            // Release LOB input resources
            try {
                lobInput.release(new NullProgressMonitor());
            } catch (Throwable e) {
                log.warn("Error releasing LOB input", e);
            }
            lobInput = null;
        }
        super.dispose();
    }

    public boolean isDirty()
    {
        for (ContentEditor contentEditor : contentEditors) {
            if (contentEditor.activated && contentEditor.editor.isDirty()) {
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
        return new LOBEditorSite(this, editor);
    }

    protected void createPages() {
        DBDStreamHandler streamHandler = getStreamHandler();
        if (streamHandler == null) {
            return;
        }
        String contentType = null;
        try {
            contentType = streamHandler.getContentType(getValueController().getValue());
        } catch (Exception e) {
            log.error("Could not determine value content type", e);
        }
        long contentLength;
        try {
            contentLength = streamHandler.getContentLength(getValueController().getValue());
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
        for (ContentEditor contentEditor : contentEditors) {
            if (contentLength > contentEditor.maxContentLength) {
                continue;
            }
            try {
                int index = addPage(contentEditor.editor, lobInput);
                setPageText(index, contentEditor.title);
                setPageImage(index, contentEditor.image);
                contentEditor.activated = true;
                // Check MIME type
                if (mimeType != null && mimeType.getPrimaryType().equals(contentEditor.preferedMimeType)) {
                    defaultPage = index;
                }
            } catch (PartInitException e) {
                log.error(e);
            }
        }
        if (defaultPage != -1) {
            setActivePage(defaultPage);
        }
    }

    protected Composite createPageContainer(Composite parent)
    {
        Composite panel = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout(1, false);
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        panel.setLayout(layout);
        GridData gd = new GridData(GridData.FILL_BOTH);
        panel.setLayoutData(gd);

        Composite editotPanel = new Composite(panel, SWT.NONE);
        layout = new GridLayout(1, false);
        editotPanel.setLayout(layout);
        gd = new GridData(GridData.FILL_BOTH);
        editotPanel.setLayoutData(gd);

        //ColumnInfoPanel infoPanel = new ColumnInfoPanel(panel, SWT.NONE, getValueController());
/*
        infoPanel = new ColumnInfoPanel(panel, SWT.NONE, getValueController()) {
            @Override
            protected void createInfoItems(Tree infoTree, DBDValueController valueController)
            {
                TreeItem columnTypeItem = new TreeItem(infoTree, SWT.NONE);
                columnTypeItem.setText(new String[] {
                    "Maximum Length",
                    String.valueOf(valueController.getColumnMetaData().getDisplaySize()) });
            }

        };

*/
        //createToolbar(panel);

        return editotPanel;
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
        DBDStreamHandler streamHandler = getStreamHandler();
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
            if (streamHandler != null) {
                text.setText(String.valueOf(streamHandler.getContentLength(value)));
            }

            // Content type
            label = new Label(contentGroup, SWT.NONE);
            label.setText("Content Type:");
            Text ctText = new Text(contentGroup, SWT.BORDER | SWT.READ_ONLY);
            if (streamHandler != null) {
                String contentType = streamHandler.getContentType(value);
                ctText.setText(contentType == null ? "unknown" : contentType);
            }

            // Content sub type
            label = new Label(contentGroup, SWT.NONE);
            label.setText("Content Encoding:");
            Text encodingText = new Text (contentGroup, SWT.BORDER | SWT.READ_ONLY);
            if (streamHandler != null) {
                String contentEncoding = streamHandler.getContentEncoding(value);
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
            log.error("Could not initialize LOB editor toolbar", e);
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

    DBDStreamHandler getStreamHandler()
    {
        DBDValueHandler valueHandler = getValueHandler();
        return valueHandler != null && valueHandler instanceof DBDStreamHandler ? (DBDStreamHandler) valueHandler : null;
    }

    public DBDValueController getValueController()
    {
        return lobInput == null ? null : lobInput.getValueController();
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

}
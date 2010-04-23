/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.editors.lob;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.part.EditorPart;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.ui.IDataSourceUser;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.data.DBDValueController;
import org.jkiss.dbeaver.model.data.DBDValueEditor;
import org.jkiss.dbeaver.model.dbc.DBCSession;
import org.jkiss.dbeaver.model.struct.DBSDataSourceContainer;
import org.jkiss.dbeaver.ui.controls.ColumnInfoPanel;

import java.nio.charset.Charset;
import java.util.SortedMap;

/**
 * LOBEditor
 */
public class LOBEditor extends EditorPart implements IDataSourceUser, DBDValueEditor
{
    static Log log = LogFactory.getLog(LOBEditor.class);

    private ColumnInfoPanel infoPanel;
    private LOBEditorInput lobInput;
    private boolean valueEditorRegistered = false;

    public static boolean openEditor(DBDValueController valueController)
    {
        LOBEditorInput editorInput = new LOBEditorInput(valueController);
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
        setSite(site);
        setInput(input);
        this.lobInput = (LOBEditorInput)input;
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
        super.dispose();
    }

    public boolean isDirty()
    {
        return false;
    }

    public boolean isSaveAsAllowed()
    {
        return false;
    }

    public void createPartControl(Composite parent)
    {
        Composite panel = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout(1, false);
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        panel.setLayout(layout);
        GridData gd = new GridData(GridData.FILL_BOTH);
        panel.setLayoutData(gd);

        infoPanel = new ColumnInfoPanel(panel, SWT.NONE, getValueController()) {
            @Override
            protected void createInfoControls(Composite infoGroup, DBDValueController valueController)
            {
                Label label = new Label(infoGroup, SWT.NONE);
                label.setText("Maximum Length: ");
                GridData gd = new GridData(GridData.FILL_HORIZONTAL);
                gd.minimumWidth = 50;
                Text text = new Text(infoGroup, SWT.BORDER | SWT.READ_ONLY);
                text.setText(String.valueOf(getValueController().getColumnMetaData().getDisplaySize()));
                text.setLayoutData(gd);
            }

            @Override
            protected int createInfoGroups(Composite infoGroup, DBDValueController valueController)
            {
                Group contentGroup = new Group(infoGroup, SWT.NONE);
                contentGroup.setText("Content");
                GridData gd = new GridData(GridData.FILL_BOTH);
                gd.horizontalIndent = 0;
                gd.verticalIndent = 0;
                contentGroup.setLayoutData(gd);
                contentGroup.setLayout(new GridLayout(2, false));

                // Content length
                Label label = new Label(contentGroup, SWT.NONE);
                label.setText("Content Length: ");
                gd = new GridData(GridData.FILL_HORIZONTAL);
                gd.minimumWidth = 50;
                Text text = new Text(contentGroup, SWT.BORDER | SWT.READ_ONLY);
                text.setText("1000");
                text.setLayoutData(gd);

                // Content type
                label = new Label(contentGroup, SWT.NONE);
                label.setText("Content Type: ");
                gd = new GridData(GridData.BEGINNING);
                gd.minimumWidth = 50;
                Combo ctCombo = new Combo(contentGroup, SWT.READ_ONLY);
                ctCombo.add("Binary");
                ctCombo.add("Text");
                ctCombo.add("Image");
                ctCombo.select(0);
                ctCombo.setLayoutData(gd);

                // Content sub type
                label = new Label(contentGroup, SWT.NONE);
                label.setText("Sub Type: ");
                gd = new GridData(GridData.BEGINNING);
                gd.minimumWidth = 50;
                Combo subTypeCombo = new Combo(contentGroup, SWT.READ_ONLY);
                subTypeCombo.setLayoutData(gd);

                // Content sub type
                label = new Label(contentGroup, SWT.NONE);
                label.setText("Encoding: ");
                gd = new GridData(GridData.FILL_HORIZONTAL);
                gd.minimumWidth = 50;
                Combo encodingCombo = new Combo(contentGroup, SWT.READ_ONLY);
                encodingCombo.setVisibleItemCount(30);
                SortedMap<String,Charset> charsetMap = Charset.availableCharsets();
                int index = 0;
                int defIndex = -1;
                for (String csName : charsetMap.keySet()) {
                    Charset charset = charsetMap.get(csName);
                    //charset.displayName()
                    encodingCombo.add(charset.displayName());
                    if (charset.equals(Charset.defaultCharset())) {
                        defIndex = index;
                    }
                    index++;
                }
                if (defIndex >= 0) {
                    encodingCombo.select(defIndex);
                }
                encodingCombo.setLayoutData(gd);
/*
                subTypeCombo.add("Binary");
                subTypeCombo.add("Text");
                subTypeCombo.add("Image");
*/
                ctCombo.setLayoutData(gd);

                return 1;
            }
        };


        Text valuePanel = new Text(panel, SWT.BORDER);
        gd = new GridData(GridData.FILL_BOTH);
        valuePanel.setLayoutData(gd);
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
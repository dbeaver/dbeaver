/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.controls.resultset.export;

import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.ui.dialogs.WizardDataTransferPage;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.proptree.EditablePropertiesControl;

class ResultSetExportPageSettings extends WizardDataTransferPage {

    private TableViewer exporterTable;

    ResultSetExportPageSettings() {
        super("Settings");
        setTitle("Settings");
        setDescription("Set export settings");
    }

    @Override
    protected boolean allowNewContainerName() {
        return false;
    }

    public void handleEvent(Event event) {

    }

    public void createControl(Composite parent) {
        initializeDialogUnits(parent);

        Composite composite = new Composite(parent, SWT.NULL);
        GridLayout gl = new GridLayout();
        gl.marginHeight = 0;
        gl.marginWidth = 0;
        composite.setLayout(gl);
        composite.setLayoutData(new GridData(GridData.FILL_BOTH));

        {
            Group generalSettings = new Group(composite, SWT.NONE);
            generalSettings.setText("General");
            gl = new GridLayout(2, false);
            generalSettings.setLayout(gl);
            generalSettings.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

            UIUtils.createLabelText(generalSettings, "To file", "");
            UIUtils.createLabelText(generalSettings, "LOBs", "");
            UIUtils.createLabelText(generalSettings, "Compress", "");
        }

        Group exporterSettings = new Group(composite, SWT.NONE);
        exporterSettings.setText("Exporter settings");
        exporterSettings.setLayoutData(new GridData(GridData.FILL_BOTH));
        exporterSettings.setLayout(new GridLayout(1, false));
        
        EditablePropertiesControl propsEditor = new EditablePropertiesControl(exporterSettings, SWT.NONE);
        propsEditor.setLayoutData(new GridData(GridData.FILL_BOTH));

        setControl(composite);
    }
}
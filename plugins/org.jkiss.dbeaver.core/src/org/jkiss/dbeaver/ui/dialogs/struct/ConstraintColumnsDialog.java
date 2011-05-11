/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.dialogs.struct;

import net.sf.jkiss.utils.CommonUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.navigator.DBNContainer;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.model.struct.DBSConstraintType;
import org.jkiss.dbeaver.model.struct.DBSTable;
import org.jkiss.dbeaver.model.struct.DBSTableColumn;
import org.jkiss.dbeaver.ui.DBIcon;
import org.jkiss.dbeaver.ui.UIUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.List;

/**
 * ConstraintColumnsDialog
 *
 * @author Serge Rider
 */
public class ConstraintColumnsDialog extends Dialog {

    static final Log log = LogFactory.getLog(ConstraintColumnsDialog.class);

    private DBSTable table;
    private Collection<DBSConstraintType> constraintTypes;
    private DBNDatabaseNode tableNode;
    private Table columnsTable;
    private Button buttonUp;
    private Button buttonDown;

    public ConstraintColumnsDialog(Shell shell, DBSTable table, Collection<DBSConstraintType> constraintTypes) {
        super(shell);
        setShellStyle(SWT.APPLICATION_MODAL | SWT.SHELL_TRIM);
        this.table = table;
        this.tableNode = DBeaverCore.getInstance().getNavigatorModel().findNode(table);
        Assert.isNotNull(this.tableNode);
        this.constraintTypes = constraintTypes;
        Assert.isTrue(!CommonUtils.isEmpty(this.constraintTypes));
    }

    @Override
    public boolean close() {
        return super.close();
    }

    @Override
    protected Control createDialogArea(Composite parent)
    {
        Composite dialogGroup = (Composite)super.createDialogArea(parent);

        final Composite panel = UIUtils.createPlaceholder(dialogGroup, 1);
        panel.setLayoutData(new GridData(GridData.FILL_BOTH));

        {
            final Composite typeGroup = new Composite(panel, SWT.NONE);
            typeGroup.setLayout(new GridLayout(2, false));
            typeGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

            UIUtils.createControlLabel(typeGroup, "Type");
            final Combo typeCombo = new Combo(typeGroup, SWT.DROP_DOWN | SWT.READ_ONLY);
            typeCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

            for (DBSConstraintType constraintType : constraintTypes) {
                typeCombo.add(constraintType.getName());
            }
            typeCombo.select(0);
            typeCombo.setEnabled(constraintTypes.size() > 1);
        }
        {
            Composite columnsGroup = UIUtils.createControlGroup(panel, "Columns", 2, GridData.FILL_BOTH, 0);
            columnsTable = new Table(columnsGroup, SWT.BORDER | SWT.SINGLE | SWT.CHECK);
            final GridData gd = new GridData(GridData.FILL_BOTH);
            gd.widthHint = 200;
            gd.heightHint = 200;
            columnsTable.setLayoutData(gd);
            columnsTable.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e)
                {
                    handleItemSelect((TableItem) e.item);
                }
            });

            Composite buttonsPanel = UIUtils.createPlaceholder(columnsGroup, 1);
            buttonsPanel.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING | GridData.FILL_VERTICAL));
            buttonUp = new Button(buttonsPanel, SWT.PUSH);
            buttonUp.setImage(DBIcon.ARROW_UP.getImage());
            buttonUp.setText("Up");
            buttonUp.setEnabled(false);

            buttonDown = new Button(buttonsPanel, SWT.PUSH);
            buttonDown.setImage(DBIcon.ARROW_DOWN.getImage());
            buttonDown.setText("Down");
            buttonDown.setEnabled(false);
        }

        // Load columns
        final List<DBNDatabaseNode> columnNodes = new ArrayList<DBNDatabaseNode>();
        try {
            DBeaverCore.getInstance().runInProgressService(new DBRRunnableWithProgress() {
                public void run(DBRProgressMonitor monitor) throws InvocationTargetException, InterruptedException
                {
                    try {
                        final List<DBNDatabaseNode> folders = tableNode.getChildren(monitor);
                        for (DBNDatabaseNode node : folders) {
                            if (node instanceof DBNContainer && DBSTableColumn.class.isAssignableFrom(((DBNContainer) node).getItemsClass())) {
                                columnNodes.addAll(node.getChildren(monitor));
                            }
                        }
                    } catch (DBException e) {
                        throw new InvocationTargetException(e);
                    }
                }
            });
        } catch (InvocationTargetException e) {
            UIUtils.showErrorDialog(getShell(), "Load columns", "Error loading table columns", e.getTargetException());
        } catch (InterruptedException e) {
            // do nothing
        }

        for (DBNDatabaseNode columnNode : columnNodes) {
            TableItem columnItem = new TableItem(columnsTable, SWT.NONE);
            columnItem.setImage(columnNode.getNodeIcon());
            columnItem.setText(columnNode.getNodeName());
        }
        //columnsTable.set

        return dialogGroup;
    }

    private void handleItemSelect(TableItem item)
    {
        if (item.getChecked()) {
            buttonUp.setEnabled(true);
            buttonDown.setEnabled(true);
        } else {
            buttonUp.setEnabled(false);
            buttonDown.setEnabled(false);
        }
    }

    @Override
    protected void okPressed()
    {
        super.okPressed();
    }

    protected void configureShell(Shell shell) {
        super.configureShell(shell);
        shell.setText(tableNode.getNodeName() + " Constraint Columns");
    }

}

/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.controls;

import org.eclipse.swt.widgets.Composite;
import org.jkiss.dbeaver.ext.ui.IDatabaseObjectEditor;

public class ObjectEditorPageControl extends ProgressPageControl {

/*
    private Button saveChangesButton;
    private Button viewChangesButton;
    private Button resetChangesButton;
*/

    public ObjectEditorPageControl(Composite parent, int style, IDatabaseObjectEditor workbenchPart)
    {
        super(parent, style, workbenchPart);

/*
        workbenchPart.addPropertyListener(new IPropertyListener() {
            public void propertyChanged(Object source, int propId)
            {
                if (propId == IEditorPart.PROP_DIRTY) {
                    boolean dirty = ((IEditorPart) source).isDirty();
                    saveChangesButton.setEnabled(dirty);
                    viewChangesButton.setEnabled(dirty);
                    resetChangesButton.setEnabled(dirty);
                }
            }
        });
*/
    }

    public IDatabaseObjectEditor getEditorPart()
    {
        return (IDatabaseObjectEditor) getWorkbenchPart();
    }

    @Override
    protected Composite createProgressPanel(Composite container) {
        return super.createProgressPanel(container);

/*
        viewChangesButton = new Button(panel, SWT.PUSH);
        viewChangesButton.setText("Preview");
        viewChangesButton.setImage(DBIcon.SQL_SCRIPT_EXECUTE.getImage());
        viewChangesButton.setToolTipText("View all changes as script");
        viewChangesButton.setEnabled(false);
        viewChangesButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {

            }
        });

        saveChangesButton = new Button(panel, SWT.PUSH);
        saveChangesButton.setText("Save");
        saveChangesButton.setImage(DBIcon.ACCEPT.getImage());
        saveChangesButton.setToolTipText("Persist all changes");
        saveChangesButton.setEnabled(false);
        saveChangesButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                DBeaverCore.getInstance().runAndWait(new DBRRunnableWithProgress() {
                    public void run(DBRProgressMonitor monitor) throws InvocationTargetException, InterruptedException
                    {
                        getEditorPart().doSave(monitor.getNestedMonitor());
                        try {
                            getObjectManager().saveChanges(monitor);
                        } catch (DBException e1) {
                            throw new InvocationTargetException(e1);
                        }
                    }
                });
            }
        });

        resetChangesButton = new Button(panel, SWT.PUSH);
        resetChangesButton.setText("Reset");
        resetChangesButton.setImage(DBIcon.REJECT.getImage());
        resetChangesButton.setToolTipText("Reset all changes");
        resetChangesButton.setEnabled(false);
        resetChangesButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                try {
                    DBeaverCore.getInstance().runAndWait2(new DBRRunnableWithProgress() {
                        public void run(DBRProgressMonitor monitor) throws InvocationTargetException, InterruptedException
                        {
                            getEditorPart().getObjectManager().resetChanges(monitor);
                            getEditorPart().resetObjectChanges();
                        }
                    });
                } catch (InvocationTargetException e1) {
                    log.error("Error resetting editor's content", e1.getTargetException());
                } catch (InterruptedException e1) {
                    // do nothing
                }
            }
        });
*/
    }

}

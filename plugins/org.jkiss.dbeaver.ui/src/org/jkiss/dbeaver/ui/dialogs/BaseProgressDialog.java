/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.dialogs;

import org.eclipse.jface.dialogs.ControlEnableState;
import org.eclipse.jface.operation.ModalContext;
import org.eclipse.jface.wizard.ProgressMonitorPart;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.runtime.DBRRunnableContext;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.model.runtime.DefaultProgressMonitor;

import java.lang.reflect.InvocationTargetException;

/**
 * BaseProgressDialog
 */
public class BaseProgressDialog extends BaseDialog implements DBRRunnableContext {

    private int runningOperations;
    private ProgressMonitorPart monitorPart;
    private Composite mainComposite;

    public BaseProgressDialog(Shell parentShell, String title, @Nullable DBPImage icon) {
        super(parentShell, title, icon);
    }

    @Override
    protected Composite createDialogArea(Composite parent) {
        mainComposite = super.createDialogArea(parent);

        return mainComposite;
    }

    @Override
    protected Control createButtonBar(Composite parent) {
        // Horizontal separator
//        new Label(mainComposite, SWT.HORIZONTAL | SWT.SEPARATOR)
//            .setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        // Progress monitor
        monitorPart = new ProgressMonitorPart(mainComposite, null, true) {
            @Override
            public void setCanceled(boolean b) {
                super.setCanceled(b);
                if (b) {
                    cancelCurrentOperation();
                }
            }
        };
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.grabExcessHorizontalSpace = true;
        gd.horizontalIndent = 20;
        gd.verticalIndent = 0;
        gd.exclude = true;
        monitorPart.setLayoutData(gd);
        monitorPart.setVisible(false);

        return super.createButtonBar(parent);
    }

    protected void cancelCurrentOperation() {

    }

    @Override
    public void run(boolean fork, boolean cancelable, DBRRunnableWithProgress runnable) throws InvocationTargetException, InterruptedException {
        // Code copied from WizardDialog
        if (monitorPart != null) {
            ((GridData)monitorPart.getLayoutData()).exclude = false;
            monitorPart.setVisible(true);
            monitorPart.getParent().layout();
            monitorPart.attachToCancelComponent(null);
        }
        //ControlEnableState pageEnableState = ControlEnableState.disable(mainComposite);
        ControlEnableState buttonsEnableState = ControlEnableState.disable(getButtonBar());
        try {
            runningOperations++;
            ModalContext.run(monitor ->
                runnable.run(new DefaultProgressMonitor(monitor)),
                true,
                monitorPart,
                getShell().getDisplay());
        } finally {
            runningOperations--;
            buttonsEnableState.restore();
            //pageEnableState.restore();
            if (monitorPart != null) {
                monitorPart.done();
                ((GridData)monitorPart.getLayoutData()).exclude = true;
                monitorPart.setVisible(false);
                monitorPart.getParent().layout();
            }
        }
    }

}

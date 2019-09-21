/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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

package org.jkiss.dbeaver.tools.transfer.ui.wizard;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.task.DBTTask;
import org.jkiss.dbeaver.tools.transfer.IDataTransferConsumer;
import org.jkiss.dbeaver.tools.transfer.IDataTransferProducer;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.task.TaskConfigurationWizardDialog;

import java.util.Collection;

/**
 * Data transfer wizard dialog
 */
public class DataTransferWizardDialog extends TaskConfigurationWizardDialog<DataTransferWizard> {

    private DataTransferWizardDialog(IWorkbenchWindow window, DataTransferWizard wizard) {
        super(window, wizard);
    }

    private DataTransferWizardDialog(IWorkbenchWindow window, DataTransferWizard wizard, IStructuredSelection selection) {
        super(window, wizard, selection);
    }

    public static int openWizard(
        @NotNull IWorkbenchWindow workbenchWindow,
        @Nullable Collection<IDataTransferProducer> producers,
        @Nullable Collection<IDataTransferConsumer> consumers)
    {
        DataTransferWizard wizard = new DataTransferWizard(UIUtils.getDefaultRunnableContext(), producers, consumers, null);
        DataTransferWizardDialog dialog = new DataTransferWizardDialog(workbenchWindow, wizard);
        return dialog.open();
    }

    public static int openWizard(
        @NotNull IWorkbenchWindow workbenchWindow,
        @Nullable Collection<IDataTransferProducer> producers,
        @Nullable Collection<IDataTransferConsumer> consumers,
        @Nullable IStructuredSelection selection)
    {
        DataTransferWizard wizard = new DataTransferWizard(UIUtils.getDefaultRunnableContext(), producers, consumers, null);
        DataTransferWizardDialog dialog = new DataTransferWizardDialog(workbenchWindow, wizard, selection);
        return dialog.open();
    }

    public static int openWizard(
        @NotNull IWorkbenchWindow workbenchWindow,
        @NotNull DBTTask task)
    {
        DataTransferWizard wizard = new DataTransferWizard(UIUtils.getDefaultRunnableContext(), task);
        DataTransferWizardDialog dialog = new DataTransferWizardDialog(workbenchWindow, wizard, null);
        return dialog.open();
    }

}

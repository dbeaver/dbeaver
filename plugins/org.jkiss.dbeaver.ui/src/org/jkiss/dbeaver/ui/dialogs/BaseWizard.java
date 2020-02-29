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

import org.eclipse.jface.dialogs.IDialogPage;
import org.eclipse.jface.wizard.Wizard;
import org.jkiss.dbeaver.model.runtime.DBRRunnableContext;
import org.jkiss.dbeaver.runtime.RunnableContextDelegate;

/**
 * BaseWizard
 */
public abstract class BaseWizard extends Wizard
{
    protected static boolean isPageActive(IDialogPage page) {
        return page != null && page.getControl() != null;
    }

    public DBRRunnableContext getRunnableContext() {
        return new RunnableContextDelegate(getContainer());
    }

}
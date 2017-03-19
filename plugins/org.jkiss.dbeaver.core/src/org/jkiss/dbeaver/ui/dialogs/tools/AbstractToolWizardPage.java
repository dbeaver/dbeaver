/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
 * Copyright (C) 2011-2012 Eugene Fradkin (eugene.fradkin@gmail.com)
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

package org.jkiss.dbeaver.ui.dialogs.tools;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.jkiss.dbeaver.ui.UIUtils;

public abstract class AbstractToolWizardPage<WIZARD extends AbstractToolWizard> extends WizardPage {

    protected final WIZARD wizard;

    protected AbstractToolWizardPage(WIZARD wizard, String pageName)
    {
        super(pageName);
        this.wizard = wizard;
    }

    @Override
    public boolean isPageComplete()
    {
        return wizard.getClientHome() != null && super.isPageComplete();
    }


    protected static void createCheckButtons(Composite buttonsPanel, final Table table) {
        Button selectAll = UIUtils.createPushButton(buttonsPanel, "All", null);
        selectAll.addSelectionListener(new CheckListener(table, true));
        Button clearSelection = UIUtils.createPushButton(buttonsPanel, "None", null);
        clearSelection.addSelectionListener(new CheckListener(table, false));
    }


    private static class CheckListener extends SelectionAdapter {
        private final Table table;
        private final boolean check;

        public CheckListener(Table table, boolean check) {
            this.table = table;
            this.check = check;
        }

        @Override
        public void widgetSelected(SelectionEvent e) {
            for (TableItem item : table.getItems()) {
                item.setChecked(check);
            }
        }
    }

}

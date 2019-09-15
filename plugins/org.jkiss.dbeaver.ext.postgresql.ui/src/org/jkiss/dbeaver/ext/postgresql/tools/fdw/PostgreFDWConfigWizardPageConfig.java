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
package org.jkiss.dbeaver.ext.postgresql.tools.fdw;

import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.ActiveWizardPage;


class PostgreFDWConfigWizardPageConfig extends ActiveWizardPage<PostgreFDWConfigWizard> {

    private boolean activated;

    protected PostgreFDWConfigWizardPageConfig()
    {
        super("Configuration");
        setTitle("Configure foreign data wrappers");
        setDescription("Choose foreign wrapper and set option");
    }

    @Override
    public boolean isPageComplete()
    {
        return false;
    }

    @Override
    public void createControl(Composite parent)
    {
        Composite composite = UIUtils.createComposite(parent, 1);

        {
            Group settingsGroup = UIUtils.createControlGroup(composite, "Settings", 1, GridData.FILL_BOTH, 0);

        }

        setControl(composite);
    }

    @Override
    public void activatePage() {
        if (!activated) {
            activated = true;
        }
        super.activatePage();
    }

}

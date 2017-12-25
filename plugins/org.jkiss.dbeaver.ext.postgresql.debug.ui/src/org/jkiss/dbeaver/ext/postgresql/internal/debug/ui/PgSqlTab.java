/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
 * Copyright (C) 2017 Alexander Fedorov (alexander.fedorov@jkiss.org)
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
package org.jkiss.dbeaver.ext.postgresql.internal.debug.ui;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;
import org.jkiss.dbeaver.debug.ui.DatabaseTab;
import org.jkiss.dbeaver.ext.postgresql.PostgreActivator;
import org.jkiss.dbeaver.ext.postgresql.debug.core.PostgreSqlDebugCore;
import org.jkiss.dbeaver.ui.UIUtils;

public class PgSqlTab extends DatabaseTab {
    
    private final ImageRegistry imageRegistry;
    
    public PgSqlTab()
    {
        this.imageRegistry = PostgreActivator.getDefault().getImageRegistry();
    }

    private Text oidText;
    
    @Override
    protected void createComponents(Composite comp)
    {
        super.createComponents(comp);
        createOidComponent(comp);
    }
    
    @Override
    public Image getImage()
    {
        return imageRegistry.get(PostgreActivator.IMG_PG_SQL);
    }

    protected void createOidComponent(Composite comp)
    {
        Group datasourceGroup = UIUtils.createControlGroup(comp, PostgreSqlDebugUiMessages.PgSqlTab_oid_group_text, 2, GridData.FILL_HORIZONTAL, SWT.DEFAULT);

        oidText = UIUtils.createLabelText(datasourceGroup, PostgreSqlDebugUiMessages.PgSqlTab_oid_label_text, PostgreSqlDebugCore.ATTR_OID_DEFAULT);
        oidText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        oidText.addModifyListener(modifyListener);
    }
    
    @Override
    public void setDefaults(ILaunchConfigurationWorkingCopy configuration)
    {
        super.setDefaults(configuration);
        configuration.setAttribute(PostgreSqlDebugCore.ATTR_OID, PostgreSqlDebugCore.ATTR_OID_DEFAULT);
    }
    
    @Override
    public void initializeFrom(ILaunchConfiguration configuration)
    {
        super.initializeFrom(configuration);
        initializeOid(configuration);
    }

    protected void initializeOid(ILaunchConfiguration configuration)
    {
        String oid = null;
        try {
            oid = configuration.getAttribute(PostgreSqlDebugCore.ATTR_OID, (String)null);
        } catch (CoreException e) {
        }
        if (oid == null) {
            oid = PostgreSqlDebugCore.ATTR_OID_DEFAULT;
        }
        oidText.setText(oid);
    }
    
    @Override
    public void performApply(ILaunchConfigurationWorkingCopy configuration)
    {
        super.performApply(configuration);
        configuration.setAttribute(PostgreSqlDebugCore.ATTR_OID, oidText.getText());
    }

}

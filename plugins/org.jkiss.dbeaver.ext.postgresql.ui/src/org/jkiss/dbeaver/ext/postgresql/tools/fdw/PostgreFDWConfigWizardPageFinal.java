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

import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.properties.PropertySourceCustom;
import org.jkiss.dbeaver.runtime.ui.UIServiceSQL;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.ActiveWizardPage;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;


class PostgreFDWConfigWizardPageFinal extends ActiveWizardPage<PostgreFDWConfigWizard> {

    private static final Log log = Log.getLog(PostgreFDWConfigWizardPageFinal.class);
    private boolean activated;
    private Object sqlPanel;

    protected PostgreFDWConfigWizardPageFinal(PostgreFDWConfigWizard wizard)
    {
        super("Script");
        setTitle("Foreign wrappers mapping SQL script");
        setDescription("Preview script and perform install");
        setWizard(wizard);
    }

    @Override
    public boolean isPageComplete()
    {
        return activated && getErrorMessage() == null;
    }

    @Override
    public void createControl(Composite parent)
    {
        Composite composite = UIUtils.createComposite(parent, 1);

        {
            Group settingsGroup = UIUtils.createControlGroup(composite, "Script", 2, GridData.FILL_BOTH, 0);
            settingsGroup.setLayout(new FillLayout());
            UIServiceSQL service = DBWorkbench.getService(UIServiceSQL.class);
            if (service != null) {
                try {
                    sqlPanel = service.createSQLPanel(
                        UIUtils.getActiveWorkbenchWindow().getActivePage().getActivePart().getSite(),
                        settingsGroup,
                        getWizard(),
                        "FDW Script",
                        true,
                        "");
                } catch (DBException e) {
                    log.debug(e);
                    setErrorMessage(e.getMessage());
                }
            }
        }


        setControl(composite);
    }

    @Override
    public void activatePage() {
        if (!activated) {
            activated = true;
        }
        generateScript();
        super.activatePage();
    }

    private void generateScript() {
        StringBuilder script = new StringBuilder();
        try {
            getWizard().getRunnableContext().run(false, true, monitor -> {
                try {
                    generateScript(monitor, script);
                } catch (DBException e) {
                    throw new InvocationTargetException(e);
                }
            });
        } catch (InvocationTargetException e) {
            log.debug(e.getTargetException());
            setErrorMessage(e.getTargetException().getMessage());
            return;
        } catch (InterruptedException e) {
            return;
        }
        setErrorMessage(null);

        String sql = script.toString();

        UIServiceSQL service = DBWorkbench.getService(UIServiceSQL.class);
        if (service != null) {
            service.setSQLPanelText(sqlPanel, sql);
        }
    }

    private void generateScript(DBRProgressMonitor monitor, StringBuilder script) throws DBException {
        PostgreFDWConfigWizard.FDWInfo selectedFDW = getWizard().getSelectedFDW();
        PropertySourceCustom propertySource = getWizard().getFdwPropertySource();
        Map<Object, Object> propValues = propertySource.getPropertiesWithDefaults();

        script.append("-- CREATE EXTENSION ").append(selectedFDW.getId()).append(";\n\n");
        String serverId = getWizard().getFdwServerId();
        script.append("CREATE SERVER ").append(serverId)
            .append("\n\tFOREIGN DATA WRAPPER ").append(selectedFDW.getId())
            .append("\n\tOPTIONS(");
        boolean firstProp = true;
        for (Map.Entry<Object, Object> pe : propValues.entrySet()) {
            String propName = CommonUtils.toString(pe.getKey());
            String propValue = CommonUtils.toString(pe.getValue());
            if (CommonUtils.isEmpty(propName) || CommonUtils.isEmpty(propValue)) {
                continue;
            }
            if (!firstProp) script.append(", ");
            script.append(propName).append(" '").append(propValue).append("'");
            firstProp = false;
        }
        script
            .append(");\n\n");

        script.append("CREATE USER MAPPING FOR CURRENT_USER SERVER ").append(serverId).append(";\n\n");

        //CREATE SERVER clickhouse_svr FOREIGN DATA WRAPPER clickhousedb_fdw OPTIONS(dbname 'default', driver '/usr/local/lib/odbc/libclickhouseodbc.so', host '46.101.202.143');
    }

}

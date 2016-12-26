/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 * Copyright (C) 2011-2012 Eugene Fradkin (eugene.fradkin@gmail.com)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.ext.oracle.views;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.ext.oracle.model.OracleConstants;
import org.jkiss.dbeaver.ext.oracle.model.dict.OracleLanguage;
import org.jkiss.dbeaver.ext.oracle.model.dict.OracleTerritory;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.connection.ConnectionPageAbstract;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;

import java.util.Map;

/**
 * OracleConnectionPage
 */
public class OracleConnectionExtraPage extends ConnectionPageAbstract
{

    private Combo languageCombo;
    private Combo territoryCombo;
    private Text nlsDateFormat;
    private Button hideEmptySchemasCheckbox;
    private Button showDBAAlwaysCheckbox;
    private Button useRuleHint;

    public OracleConnectionExtraPage()
    {
        setTitle("Oracle properties");
        setDescription("Regional settings and performance");
    }

    @Override
    public void dispose()
    {
        super.dispose();
    }

    @Override
    public void createControl(Composite parent)
    {
        Composite cfgGroup = new Composite(parent, SWT.NONE);
        GridLayout gl = new GridLayout(1, false);
        gl.marginHeight = 10;
        gl.marginWidth = 10;
        cfgGroup.setLayout(gl);
        GridData gd = new GridData(GridData.FILL_BOTH);
        cfgGroup.setLayoutData(gd);

        {
            final Group sessionGroup = UIUtils.createControlGroup(cfgGroup, "Session settings", 2, GridData.HORIZONTAL_ALIGN_BEGINNING, 0);

            languageCombo = UIUtils.createLabelCombo(sessionGroup, "Language", SWT.DROP_DOWN);
            languageCombo.setToolTipText("Session language");
            languageCombo.add(OracleConstants.NLS_DEFAULT_VALUE);
            for (OracleLanguage language : OracleLanguage.values()) {
                languageCombo.add(language.getLanguage());
            }
            languageCombo.setText(OracleConstants.NLS_DEFAULT_VALUE);

            territoryCombo = UIUtils.createLabelCombo(sessionGroup, "Territory", SWT.DROP_DOWN);
            territoryCombo.setToolTipText("Session territory");
            territoryCombo.add(OracleConstants.NLS_DEFAULT_VALUE);
            for (OracleTerritory territory : OracleTerritory.values()) {
                territoryCombo.add(territory.getTerritory());
            }
            territoryCombo.setText(OracleConstants.NLS_DEFAULT_VALUE);

            nlsDateFormat = UIUtils.createLabelText(sessionGroup, "NLS Date Format", "");
        }

        {
            final Group contentGroup = UIUtils.createControlGroup(cfgGroup, "Content", 1, GridData.HORIZONTAL_ALIGN_BEGINNING, 0);

            hideEmptySchemasCheckbox = UIUtils.createCheckbox(contentGroup, "Hide empty schemas", true);
            hideEmptySchemasCheckbox.setToolTipText(
                "Check existence of objects within schema and do not show empty schemas in tree. " + GeneralUtils.getDefaultLineSeparator() +
                "Enabled by default but it may cause performance problems on databases with very big number of objects.");

            showDBAAlwaysCheckbox = UIUtils.createCheckbox(contentGroup, "Always show DBA objects", false);
            showDBAAlwaysCheckbox.setToolTipText(
                "Always shows DBA-related metadata objects in tree even if user do not has DBA role.");
        }

        {
            final Group contentGroup = UIUtils.createControlGroup(cfgGroup, "Performance", 1, GridData.HORIZONTAL_ALIGN_BEGINNING, 0);

            useRuleHint = UIUtils.createCheckbox(contentGroup, "Use RULE hint for system catalog queries", true);
            useRuleHint.setToolTipText(
                "Adds RULE hint for some system catalog queries (like columns and constraints reading)." + GeneralUtils.getDefaultLineSeparator() +
                "It significantly increases performance on some Oracle databases (and decreases on others).");
        }

        setControl(cfgGroup);
    }

    @Override
    public boolean isComplete()
    {
        return true;
    }

    @Override
    public void loadSettings()
    {
        //oraHomeSelector.setVisible(isOCI);

        // Load values from new connection info
        DBPConnectionConfiguration connectionInfo = site.getActiveDataSource().getConnectionConfiguration();
        Map<String, String> providerProperties = connectionInfo.getProviderProperties();

        // Settings
        final Object nlsLanguage = providerProperties.get(OracleConstants.PROP_SESSION_LANGUAGE);
        if (nlsLanguage != null) {
            languageCombo.setText(nlsLanguage.toString());
        }

        final Object nlsTerritory = providerProperties.get(OracleConstants.PROP_SESSION_TERRITORY);
        if (nlsTerritory != null) {
            territoryCombo.setText(nlsTerritory.toString());
        }

        final Object dateFormat = providerProperties.get(OracleConstants.PROP_SESSION_NLS_DATE_FORMAT);
        if (dateFormat != null) {
            nlsDateFormat.setText(dateFormat.toString());
        }

        final Object checkSchemaContent = providerProperties.get(OracleConstants.PROP_CHECK_SCHEMA_CONTENT);
        if (checkSchemaContent != null) {
            hideEmptySchemasCheckbox.setSelection(CommonUtils.getBoolean(checkSchemaContent, false));
        }

        showDBAAlwaysCheckbox.setSelection(CommonUtils.getBoolean(providerProperties.get(OracleConstants.PROP_ALWAYS_SHOW_DBA), false));
        useRuleHint.setSelection(CommonUtils.getBoolean(providerProperties.get(OracleConstants.PROP_USE_RULE_HINT), false));
    }

    @Override
    public void saveSettings(DBPDataSourceContainer dataSource)
    {
        Map<String, String> providerProperties = dataSource.getConnectionConfiguration().getProviderProperties();

        {
            // Settings
            if (!OracleConstants.NLS_DEFAULT_VALUE.equals(languageCombo.getText())) {
                providerProperties.put(OracleConstants.PROP_SESSION_LANGUAGE, languageCombo.getText());
            } else {
                providerProperties.remove(OracleConstants.PROP_SESSION_LANGUAGE);
            }

            if (!OracleConstants.NLS_DEFAULT_VALUE.equals(territoryCombo.getText())) {
                providerProperties.put(OracleConstants.PROP_SESSION_TERRITORY, territoryCombo.getText());
            } else {
                providerProperties.remove(OracleConstants.PROP_SESSION_TERRITORY);
            }

            String dateFormat = nlsDateFormat.getText();
            if (!dateFormat.isEmpty()) {
                providerProperties.put(OracleConstants.PROP_SESSION_NLS_DATE_FORMAT, dateFormat);
            } else {
                providerProperties.remove(OracleConstants.PROP_SESSION_NLS_DATE_FORMAT);
            }

            providerProperties.put(
                OracleConstants.PROP_CHECK_SCHEMA_CONTENT,
                String.valueOf(hideEmptySchemasCheckbox.getSelection()));
            providerProperties.put(
                OracleConstants.PROP_ALWAYS_SHOW_DBA,
                String.valueOf(showDBAAlwaysCheckbox.getSelection()));
            providerProperties.put(
                OracleConstants.PROP_USE_RULE_HINT,
                String.valueOf(useRuleHint.getSelection()));
        }
        saveConnectionURL(dataSource.getConnectionConfiguration());
    }

}

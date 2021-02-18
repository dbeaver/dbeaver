/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.editors.sql.preferences.format;

import org.eclipse.jface.dialogs.DialogPage;
import org.eclipse.swt.widgets.Composite;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.sql.format.SQLFormatterConfiguration;
import org.jkiss.dbeaver.model.sql.registry.SQLFormatterDescriptor;

public abstract class BaseFormatterConfigurationPage extends DialogPage implements SQLFormatterConfigurator {

    private SQLFormatterDescriptor formatterDescriptor;
    private SQLFormatterConfiguration configuration;
    public Runnable changeListener;

    public BaseFormatterConfigurationPage()
    {
        super("SQL Format");
    }

    public SQLFormatterDescriptor getFormatterDescriptor() {
        return formatterDescriptor;
    }

    public SQLFormatterConfiguration getConfiguration() {
        return configuration;
    }

    @Override
    public void performHelp() {
        super.performHelp();
    }

    @Override
    public final void createControl(Composite parent) {
        setTitle("SQL Format Configuration");
        setDescription(formatterDescriptor.getDescription());
        Composite composite = createFormatSettings(parent);


        setControl(composite);
    }

    protected abstract Composite createFormatSettings(Composite parent);

    @Override
    public void configure(SQLFormatterDescriptor formatterDescriptor, Runnable changeListener) {
        this.formatterDescriptor = formatterDescriptor;
        this.configuration = configuration;
        this.changeListener = changeListener;
    }

    @Override
    public void loadSettings(DBPPreferenceStore preferenceStore) {

    }

    @Override
    public void saveSettings(DBPPreferenceStore preferenceStore) {

    }

    @Override
    public void resetSettings(DBPPreferenceStore preferenceStore) {

    }

}

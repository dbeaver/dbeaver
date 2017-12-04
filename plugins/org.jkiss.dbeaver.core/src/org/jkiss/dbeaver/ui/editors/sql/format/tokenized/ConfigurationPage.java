/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ui.editors.sql.format.tokenized;

import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Spinner;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.sql.format.BaseFormatterConfigurationPage;

public class ConfigurationPage extends BaseFormatterConfigurationPage {


    @Override
    protected Composite createFormatSettings(Composite parent) {
        Group settings = UIUtils.createControlGroup(parent, "Settings", 2, GridData.FILL_HORIZONTAL, 0);

        Button useSpaces = UIUtils.createCheckbox(settings, "Insert spaces for tabs", "Insert spaces for tabs", false, 2);
        Spinner indentSize = UIUtils.createLabelSpinner(settings, "Indent size", "Insert spaces for tabs", 4, 0, 100);

        return parent;
    }
}

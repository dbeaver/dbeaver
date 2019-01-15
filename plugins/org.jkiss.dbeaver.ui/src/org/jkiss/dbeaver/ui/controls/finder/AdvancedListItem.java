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
package org.jkiss.dbeaver.ui.controls.finder;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;

/**
 * AdvancedListItem
 */
public class AdvancedListItem extends Composite
{
    private static final Log log = Log.getLog(AdvancedListItem.class);

    public AdvancedListItem(AdvancedList list, String text, DBPImage icon) {
        super(list, SWT.NONE);

        setLayout(new GridLayout(1, true));

        Label iconLabel = new Label(this, SWT.NONE);
        iconLabel.setImage(DBeaverIcons.getImage(icon));
        iconLabel.setLayoutData(new GridData(GridData.FILL_BOTH));
        Label textLabel = new Label(this, SWT.CENTER);
        textLabel.setText(text);
        textLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_CENTER));
    }

}
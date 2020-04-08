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

package org.jkiss.dbeaver.ui.editors.object.struct;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.dbeaver.model.virtual.DBVEntity;
import org.jkiss.dbeaver.model.virtual.DBVUtils;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;

/**
 * EditDictionaryPage
 *
 * @author Serge Rider
 */
public class EditDictionaryPage extends AttributesSelectorPage {

    private Text criteriaText;
    private DBVEntity dictionary;
    private Collection<DBSEntityAttribute> descColumns;
    private DBSEntity entity;

    public EditDictionaryPage(
        final DBSEntity entity)
    {
        super("Edit dictionary", entity);
        this.entity = entity;
        this.dictionary = DBVUtils.getVirtualEntity(entity, true);
        UIUtils.runInUI(PlatformUI.getWorkbench().getActiveWorkbenchWindow(), monitor -> {
            try {
                if (dictionary.getDescriptionColumnNames() == null) {
                    Collection<? extends DBSEntityAttribute> tablePK = DBUtils.getBestTableIdentifier(monitor, entity);
                    if (tablePK != null && !tablePK.isEmpty()) {
                        dictionary.setDescriptionColumnNames(DBVEntity.getDefaultDescriptionColumn(monitor, tablePK.iterator().next()));
                    }
                }
                descColumns = dictionary.getDescriptionColumns(monitor, entity);
            } catch (DBException e) {
                throw new InvocationTargetException(e);
            }
        });
    }

    public DBVEntity getDictionary()
    {
        return dictionary;
    }

    @Override
    protected void createContentsBeforeColumns(Composite panel)
    {
        Label label = UIUtils.createControlLabel(panel,
            "Choose dictionary description columns or set custom criteria");
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalSpan = 2;
        label.setLayoutData(gd);
    }

    @Override
    protected void createContentsAfterColumns(Composite panel)
    {
        Composite group = new Composite(panel, SWT.NONE);
        group.setLayout(new GridLayout(1, false));
        group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        UIUtils.createControlLabel(group, "Custom criteria");
        criteriaText = new Text(group, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.WRAP);
        GridData gd = new GridData(GridData.FILL_BOTH);
        gd.heightHint = 50;
        criteriaText.setLayoutData(gd);
        if (!CommonUtils.isEmpty(dictionary.getDescriptionColumnNames())) {
            criteriaText.setText(dictionary.getDescriptionColumnNames());
        }
    }

    @Override
    public boolean isColumnSelected(DBSEntityAttribute attribute)
    {
        return descColumns.contains(attribute);
    }

    @Override
    protected void handleColumnsChange() {
        descColumns = getSelectedAttributes();
        StringBuilder custom = new StringBuilder();
        for (DBSEntityAttribute column : descColumns) {
            if (custom.length() > 0) {
                custom.append(",");
            }
            custom.append(DBUtils.getQuotedIdentifier(column));
        }
        criteriaText.setText(custom.toString());
    }

    @Override
    public boolean isPageComplete() {
        if (!CommonUtils.isEmpty(criteriaText.getText())) {
            return true;
        }
        return super.isPageComplete();
    }

    @Override
    public void performFinish()
    {
        saveDictionarySettings();
        entity.getDataSource().getContainer().persistConfiguration();
    }

    public void saveDictionarySettings() {
        dictionary.setDescriptionColumnNames(criteriaText.getText());
    }

}

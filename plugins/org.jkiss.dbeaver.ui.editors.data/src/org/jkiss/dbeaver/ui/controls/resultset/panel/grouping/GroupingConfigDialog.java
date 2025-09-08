/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.controls.resultset.panel.grouping;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.DBValueFormatting;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.sql.SQLGroupingAttribute;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.contentassist.StringContentProposalProvider;
import org.jkiss.dbeaver.ui.controls.StringEditorTable;
import org.jkiss.dbeaver.ui.dialogs.BaseDialog;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Grouping configuration dialog
 */
class GroupingConfigDialog extends BaseDialog {
    private static final String DIALOG_ID = "DBeaver.GroupingConfigDialog"; //$NON-NLS-1$

    private final GroupingResultsContainer resultsContainer;
    private Table columnsTable;
    private Table functionsTable;

    public GroupingConfigDialog(Shell parentShell, GroupingResultsContainer resultsContainer) {
        super(parentShell, "Grouping configuration", null);
        this.resultsContainer = resultsContainer;
    }

    @Override
    protected IDialogSettings getDialogBoundsSettings() {
        return UIUtils.getDialogSettings(DIALOG_ID);
    }

    @Override
    protected Composite createDialogArea(Composite parent) {
        Composite composite = super.createDialogArea(parent);

        List<String> proposals = new ArrayList<>();
        for (DBDAttributeBinding attr : resultsContainer.getOwnerPresentation().getController().getModel().getAttributes()) {
            proposals.add(attr.getName());
        }
        StringContentProposalProvider proposalProvider = new StringContentProposalProvider(new String[0]);
        proposalProvider.setProposals(proposals.toArray(new String[0]));
        columnsTable = StringEditorTable.createCustomEditableList(composite, "Columns", resultsContainer.getGroupAttributes(), new GroupingAttributeValueManager(), proposalProvider, true);

        Collections.addAll(proposals, "COUNT", "AVG", "MAX", "MIN", "SUM");
        proposalProvider.setProposals(proposals.toArray(new String[0]));
        functionsTable = StringEditorTable.createEditableList(composite, "Functions", resultsContainer.getGroupFunctions(), DBIcon.TREE_FUNCTION, proposalProvider);

        return composite;
    }

    @Override
    protected void okPressed() {
        List<SQLGroupingAttribute> attributes = StringEditorTable.collectCustomValues(columnsTable);
        List<String> functions = StringEditorTable.collectStringValues(functionsTable);
        resultsContainer.setGrouping(attributes, functions);
        super.okPressed();
    }

    private class GroupingAttributeValueManager implements StringEditorTable.TableValuesManager<SQLGroupingAttribute> {
        @NotNull
        @Override
        public DBPImage getIcon(@Nullable SQLGroupingAttribute value) {
            if (value instanceof SQLGroupingAttribute.BoundAttribute bound) {
                DBSDataType type = bound.getBinding().getDataType();
                return type == null ? DBIcon.TYPE_UNKNOWN : DBValueFormatting.getTypeImage(type);
            } else {
                return DBIcon.TREE_ATTRIBUTE;
            }
        }

        @NotNull
        @Override
        public String getString(@Nullable SQLGroupingAttribute value) {
            return value == null ? "" : value.getDisplayName();
        }

        @NotNull
        @Override
        public Boolean isEditable(@Nullable SQLGroupingAttribute value) {
            return value == null || value instanceof SQLGroupingAttribute.CustomAttribute;
        }

        @Nullable
        @Override
        public SQLGroupingAttribute prepareNewValue(@Nullable SQLGroupingAttribute originalValue, @Nullable String string) {
            if (CommonUtils.isNotEmpty(string)) {
                return SQLGroupingAttribute.makeCustom(resultsContainer.getDataContainer().getDataSource(), string);
            } else {
                return null;
            }
        }
    }
}

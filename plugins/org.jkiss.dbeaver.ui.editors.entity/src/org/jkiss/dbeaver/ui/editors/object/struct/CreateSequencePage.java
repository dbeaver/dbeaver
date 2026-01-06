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
package org.jkiss.dbeaver.ui.editors.object.struct;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.rdb.DBSSequence;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.object.internal.ObjectEditorMessages;

public class CreateSequencePage extends BaseObjectEditPage {

    private DBSSequence sequence;
    private String name;

    public CreateSequencePage(DBSSequence sequence) {
        super(ObjectEditorMessages.dialog_struct_create_sequence_title);
        this.sequence = sequence;
    }

    @Override
    public DBSObject getObject() {
        return this.sequence;
    }

    @Override
    protected Control createPageContents(Composite parent) {
        Composite propsGroup = new Composite(parent, SWT.NONE);
        propsGroup.setLayout(new GridLayout(2, false));
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        propsGroup.setLayoutData(gd);

        UIUtils.createLabelText(propsGroup, ObjectEditorMessages.dialog_struct_create_sequence_container, DBUtils.getObjectFullName(sequence.getParentObject(), DBPEvaluationContext.UI)).setEditable(false);
        final Text nameText = UIUtils.createLabelText(propsGroup, ObjectEditorMessages.dialog_struct_create_sequence_name, null);
        nameText.addModifyListener(e -> name = nameText.getText());
        nameText.setFocus();
        return propsGroup;
    }

    public String getSequenceName() {
        return DBObjectNameCaseTransformer.transformName(sequence.getDataSource(), name);
    }
}

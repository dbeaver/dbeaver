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
package org.jkiss.dbeaver.ui.data.hints;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSEntityAssociation;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.controls.resultset.IResultSetController;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetRow;
import org.jkiss.dbeaver.ui.data.IValueHint;
import org.jkiss.dbeaver.ui.data.IValueHintAction;

import java.util.Collections;

/**
 * ValueHintText
 */
public class ValueHintReference implements IValueHint, IValueHintAction {

    @NotNull
    private final DBDAttributeBinding attribute;
    @NotNull
    private final ResultSetRow row;
    @NotNull
    private final DBSEntityAssociation association;

    public ValueHintReference(
        @NotNull DBDAttributeBinding attribute,
        @NotNull ResultSetRow row,
        @NotNull DBSEntityAssociation association
    ) {
        this.attribute = attribute;
        this.row = row;
        this.association = association;
    }

    @Override
    public HintType getHintType() {
        return HintType.ACTION;
    }

    @Override
    public String getHintText() {
        DBSEntity entity = association.getAssociatedEntity();
        return "Navigate to " + (entity == null ? "???" : entity.getName());
    }

    @Override
    public String getHintDescription() {
        return "Navigate to referenced table row";
    }

    @Override
    public DBPImage getHintIcon() {
        return UIIcon.LINK;
    }

    @Override
    public void performAction(@NotNull IResultSetController controller, long state) throws DBException {
        controller.navigateReference(
            new VoidProgressMonitor(),
            controller.getModel(),
            association,
            Collections.singletonList(row),
            false);
    }
}
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
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityAssociation;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraint;
import org.jkiss.dbeaver.model.struct.DBSEntityReferrer;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetRow;
import org.jkiss.dbeaver.ui.data.IValueHint;
import org.jkiss.dbeaver.ui.data.IValueHintContext;
import org.jkiss.dbeaver.ui.data.IValueHintProvider;
import org.jkiss.dbeaver.ui.data.registry.ValueHintText;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;

/**
 * Reference hint provider
 */
public class ReferenceHintProvider implements IValueHintProvider {

    @Nullable
    @Override
    public IValueHint[] getValueHint(
        @NotNull IValueHintContext context,
        @NotNull DBDAttributeBinding attribute,
        @Nullable Object value,
        @NotNull EnumSet<IValueHint.HintType> types,
        int options
    ) {
        List<DBSEntityReferrer> referrers = attribute.getReferrers();
        if (!CommonUtils.isEmpty(referrers)) {
            List<IValueHint> refHints = new ArrayList<>();
            for (DBSEntityReferrer referrer : referrers) {
                if (referrer instanceof DBSEntityAssociation ea) {
                    DBSEntityConstraint refConstr = ea.getReferencedConstraint();
                    refHints.add(
                        new ValueHintText(refConstr.getParentObject().getName(), "Table '" + refConstr.getParentObject().getName() + "' reference", UIIcon.LINK)
                    );
                }
            }
            return refHints.toArray(new IValueHint[0]);
        }
        return null;
    }

    @Override
    public void cacheRequiredData(
        @NotNull DBRProgressMonitor monitor,
        @NotNull IValueHintContext context,
        @NotNull Collection<DBDAttributeBinding> attributes,
        @NotNull Collection<ResultSetRow> rows
    ) throws DBException {
        // noop
    }

}

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
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.data.DBDValueRow;
import org.jkiss.dbeaver.model.data.hints.DBDValueHint;
import org.jkiss.dbeaver.model.data.hints.DBDValueHintContext;
import org.jkiss.dbeaver.model.data.hints.DBDValueHintProvider;
import org.jkiss.dbeaver.model.struct.DBSEntityAssociation;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraint;
import org.jkiss.dbeaver.model.struct.DBSEntityReferrer;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/**
 * Reference hint provider.
 * It is declared in UI bundle because it provides interaction with data editor
 */
public class ReferenceHintProvider implements DBDValueHintProvider {

    @Nullable
    @Override
    public DBDValueHint[] getValueHint(
        @NotNull DBDValueHintContext context,
        @NotNull DBDAttributeBinding attribute,
        @NotNull DBDValueRow row,
        @Nullable Object value,
        @NotNull EnumSet<DBDValueHint.HintType> types,
        int options
    ) {
        if (DBUtils.isNullValue(value)) {
            return null;
        }
        List<DBSEntityReferrer> referrers = attribute.getReferrers();
        if (!CommonUtils.isEmpty(referrers)) {
            List<DBDValueHint> refHints = new ArrayList<>();
            for (DBSEntityReferrer referrer : referrers) {
                if (referrer instanceof DBSEntityAssociation ea) {
                    DBSEntityConstraint refConstr = ea.getReferencedConstraint();
                    if (refConstr != null) {
                        refHints.add(
                            new ValueHintReference(
                                attribute,
                                row,
                                ea));
                    }
                }
            }
            return refHints.toArray(new DBDValueHint[0]);
        }
        return null;
    }

}

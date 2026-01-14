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
package org.jkiss.dbeaver.model.data.hints.standard;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.data.DBDResultSetModel;
import org.jkiss.dbeaver.model.data.hints.DBDAttributeHintProvider;
import org.jkiss.dbeaver.model.data.hints.DBDValueHint;
import org.jkiss.dbeaver.model.data.hints.ValueHintText;
import org.jkiss.dbeaver.model.exec.DBExecUtils;

import java.util.EnumSet;

/**
 * Attribute status hint provider
 */
public class AttributeStatusHintProvider implements DBDAttributeHintProvider {

    @Nullable
    @Override
    public DBDValueHint[] getAttributeHints(
        @NotNull DBDResultSetModel model,
        @NotNull DBDAttributeBinding attribute,
        @NotNull EnumSet<DBDValueHint.HintType> types,
        int options
    ) {
        DBPDataSource dataSource = attribute.getDataSource();
        String readOnlyStatus = model.getReadOnlyStatus(dataSource == null ? null : dataSource.getContainer());
        if (readOnlyStatus == null) {
            readOnlyStatus = DBExecUtils.getAttributeReadOnlyStatus(attribute, true);
        }

        if (readOnlyStatus != null) {
            return new DBDValueHint[] {
                new ValueHintReadOnly(
                    "Read-only: " + readOnlyStatus)
            };
        }

        return null;
    }

    static class ValueHintReadOnly extends ValueHintText {

        public ValueHintReadOnly(String text) {
            super(text, null, null);
        }

        @Override
        public int getHintOptions() {
            return OPTION_READ_ONLY;
        }
    }
}

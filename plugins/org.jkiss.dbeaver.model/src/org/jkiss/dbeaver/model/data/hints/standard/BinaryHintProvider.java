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
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.data.DBDContent;
import org.jkiss.dbeaver.model.data.DBDValueRow;
import org.jkiss.dbeaver.model.data.hints.DBDValueHint;
import org.jkiss.dbeaver.model.data.hints.DBDValueHintContext;
import org.jkiss.dbeaver.model.data.hints.DBDValueHintProvider;
import org.jkiss.dbeaver.model.data.hints.ValueHintText;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.utils.ContentUtils;
import org.jkiss.utils.ByteNumberFormat;
import org.jkiss.utils.CommonUtils;

import java.util.EnumSet;

/**
 * Binary hint provider
 */
public class BinaryHintProvider implements DBDValueHintProvider {

    private static final Log log = Log.getLog(BinaryHintProvider.class);

    private final ByteNumberFormat LENGTH_FORMAT = new ByteNumberFormat();

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
        if (types.contains(DBDValueHint.HintType.STRING) &&
            !DBUtils.isNullValue(value)
        ) {
            long contentLength = 0;
            if (value instanceof DBDContent content && !ContentUtils.isTextContent(content)) {
                try {
                    contentLength = content.getContentLength();
                } catch (DBCException e) {
                    log.debug("Cannot read content length", e);
                }
            } else if (value instanceof CharSequence chars) {
                contentLength = chars.length();
            } else if (value instanceof byte[] bytes) {
                contentLength = bytes.length;
            }
            if (contentLength == 0) {
                return null;
            }
            String formattedLength = LENGTH_FORMAT.format(contentLength);
            return new DBDValueHint[] {
                new ValueHintText(
                    !CommonUtils.isBitSet(options, OPTION_TOOLTIP) ? "[" + formattedLength + "]" : formattedLength,
                    "Data length", null)
            };
        }
        return null;
    }

}

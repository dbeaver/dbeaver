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
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.data.DBDResultSetModel;
import org.jkiss.dbeaver.model.data.DBDValueRow;
import org.jkiss.dbeaver.model.data.hints.DBDCellHintProvider;
import org.jkiss.dbeaver.model.data.hints.DBDValueHint;
import org.jkiss.dbeaver.model.data.hints.ValueHintText;

import java.util.Date;
import java.util.EnumSet;
import java.util.TimeZone;

/**
 * Time zone hint provider
 */
public class TimeZoneCellHintProvider implements DBDCellHintProvider {

    private static final boolean SHOW_TZ_ALWAYS = true;

    @Nullable
    @Override
    public DBDValueHint[] getCellHints(
        @NotNull DBDResultSetModel model,
        @NotNull DBDAttributeBinding attribute,
        @NotNull DBDValueRow row,
        @Nullable Object value,
        @NotNull EnumSet<DBDValueHint.HintType> types,
        int options
    ) {
        if (attribute.getDataKind() == DBPDataKind.DATETIME) {
            String timezone = null;
            if (value instanceof Date ts) {
                int timezoneOffset = ts.getTimezoneOffset() * -60000;
                if (SHOW_TZ_ALWAYS || timezoneOffset != TimeZone.getDefault().getRawOffset()) {
                    timezone = toCustomID(timezoneOffset);
                }
            }
            if (timezone != null) {
                return new DBDValueHint[]{
                    new ValueHintText(
                        timezone,
                        "Timezone", null)
                };
            }
        }
        return null;
    }

    public static String toCustomID(int gmtOffset) {
        char sign;
        int offset = gmtOffset / 60000;
        if (offset >= 0) {
            sign = '+';
        } else {
            sign = '-';
            offset = -offset;
        }
        int hh = offset / 60;
        int mm = offset % 60;

        char[] buf = new char[] { 'G', 'M', 'T', sign, '0', '0', ':', '0', '0' };
        if (hh >= 10) {
            buf[4] += (char) (hh / 10);
        }
        buf[5] += (char) (hh % 10);
        if (mm != 0) {
            buf[7] += (char) (mm / 10);
            buf[8] += (char) (mm % 10);
        }
        return new String(buf);
    }

}

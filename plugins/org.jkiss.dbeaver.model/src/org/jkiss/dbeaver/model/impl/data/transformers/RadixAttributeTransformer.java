/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.model.impl.data.transformers;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.data.DBDAttributeTransformer;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.model.data.DBDValueHandler;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.impl.data.ProxyValueHandler;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.utils.CommonUtils;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Transforms numeric attribute value into string in a specified radix
 */
public class RadixAttributeTransformer implements DBDAttributeTransformer {

    public static final String PROP_RADIX = "radix";
    public static final String PROP_BITS = "bits";
    public static final String PROP_PREFIX = "prefix";

    @Override
    public void transformAttribute(@NotNull DBCSession session, @NotNull DBDAttributeBinding attribute, @NotNull List<Object[]> rows, @NotNull Map<String, String> options) throws DBException {
        int radix = 16;
        int bits = 32;
        boolean showPrefix = false;
        if (options.containsKey(PROP_RADIX)) {
            radix = CommonUtils.toInt(options.get(PROP_RADIX), radix);
        }
        if (options.containsKey(PROP_BITS)) {
            bits = CommonUtils.toInt(options.get(PROP_BITS), bits);
        }
        if (options.containsKey(PROP_PREFIX)) {
            showPrefix = CommonUtils.getBoolean(options.get(PROP_PREFIX), showPrefix);
        }

        attribute.setValueHandler(new RadixValueHandler(attribute.getValueHandler(), radix, bits, showPrefix));
    }

    private class RadixValueHandler extends ProxyValueHandler {

        private int radix;
        private int bits;
        private boolean showPrefix;

        public RadixValueHandler(DBDValueHandler target, int radix, int bits, boolean showPrefix) {
            super(target);
            this.radix = radix;
            this.bits = bits;
            this.showPrefix = showPrefix;
        }

        @NotNull
        @Override
        public String getValueDisplayString(@NotNull DBSTypedObject column, @Nullable Object value, @NotNull DBDDisplayFormat format) {
            if (value instanceof Number) {
                final long longValue = ((Number) value).longValue();
                final String strValue = Long.toString(longValue, radix).toUpperCase(Locale.ENGLISH);
                if (showPrefix) {
                    if (radix == 16) {
                        return "0x" + strValue;
                    } else if (radix == 8) {
                        return "0" + strValue;
                    } else if (radix == 2) {
                        return "0b" + strValue;
                    }
                }
                return strValue;
            }
            return DBUtils.getDefaultValueDisplayString(value, format);
        }
    }
}

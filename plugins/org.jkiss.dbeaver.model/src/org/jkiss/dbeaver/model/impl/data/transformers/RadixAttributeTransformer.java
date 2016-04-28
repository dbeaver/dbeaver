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
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.data.DBDAttributeTransformer;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.model.data.DBDValueHandler;
import org.jkiss.dbeaver.model.exec.DBCException;
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

    private static final Log log = Log.getLog(RadixAttributeTransformer.class);

    public static final String PROP_RADIX = "radix";
    public static final String PROP_BITS = "bits";
    public static final String PROP_PREFIX = "prefix";

    public static final String PREFIX_HEX = "0x";
    public static final String PREFIX_OCT = "0";
    public static final String PREFIX_BIN = "0b";

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
        attribute.setPresentationAttribute(
            new TransformerPresentationAttribute(attribute, "StringNumber", -1, DBPDataKind.STRING));
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
                        return PREFIX_HEX + strValue;
                    } else if (radix == 8) {
                        return PREFIX_OCT + strValue;
                    } else if (radix == 2) {
                        return PREFIX_BIN + strValue;
                    }
                }
                return strValue;
            }
            return DBUtils.getDefaultValueDisplayString(value, format);
        }

        @Nullable
        @Override
        public Object getValueFromObject(@NotNull DBCSession session, @NotNull DBSTypedObject type, @Nullable Object object, boolean copy) throws DBCException {
            if (object instanceof String) {
                String strValue = (String) object;
                if (showPrefix) {
                    if (radix == 16 && strValue.startsWith(PREFIX_HEX)) {
                        strValue = strValue.substring(2);
                    } else if (radix == 8 && strValue.startsWith(PREFIX_OCT)) {
                        strValue = strValue.substring(1);
                    } else if (radix == 2 && strValue.startsWith(PREFIX_BIN)) {
                        strValue = strValue.substring(2);
                    }
                }
                try {
                    return Long.parseLong(strValue, radix);
                } catch (NumberFormatException e) {
                    log.debug(e);
                }
            }
            return super.getValueFromObject(session, type, object, copy);
        }
    }
}

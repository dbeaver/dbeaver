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
package org.jkiss.dbeaver.model;

import org.jkiss.dbeaver.model.data.DBDBinaryFormatter;
import org.jkiss.dbeaver.model.impl.data.formatters.BinaryFormatterBase64;
import org.jkiss.dbeaver.model.impl.data.formatters.BinaryFormatterHex;
import org.jkiss.dbeaver.model.impl.data.formatters.BinaryFormatterString;

/**
 * General model constants
 */
public class DBConstants {

    public static final int METADATA_FETCH_SIZE = 1000;
    public static final long DISCONNECT_TIMEOUT = 2000;

    public static final String DATA_SOURCE_PROPERTY_USER = "user"; //NON-NLS-1
    public static final String DATA_SOURCE_PROPERTY_PASSWORD = "password"; //NON-NLS-1

    public static final String NULL_VALUE_LABEL = "[NULL]"; //NON-NLS-1

    public static final String PROP_ID_NAME = "name"; //NON-NLS-1
    public static final String PROP_ID_DESCRIPTION = "description"; //NON-NLS-1
    public static final String PROP_ID_TYPE_NAME = "typeName"; //NON-NLS-1
    public static final String PROP_ID_MAX_LENGTH = "maxLength"; //NON-NLS-1
    public static final String PROP_ID_NOT_NULL = "notNull"; //NON-NLS-1
    public static final String PARAM_INIT_ON_TEST = "initOnTest"; //NON-NLS-1

    // Internal properties prefix. This is a legacy properties marker (used to divide driver properties from provider properties)
    // Left for backward compatibility. Do not use it for new provider property names
    public static final String INTERNAL_PROP_PREFIX = "@dbeaver-"; //NON-NLS-1

    // Used for default driver property values redefine
    public static final String DEFAULT_DRIVER_PROP_PREFIX = INTERNAL_PROP_PREFIX + "default-"; //NON-NLS-1

    public static final String[] DEFAULT_DATATYPE_NAMES = {
        "varchar", //NON-NLS-1
        "varchar2", //NON-NLS-1
        "string",  //NON-NLS-1
        "char", //NON-NLS-1
        "integer", //NON-NLS-1
        "number" //NON-NLS-1
    };

    public static final String BOOLEAN_PROP_YES = "yes";
    public static final String BOOLEAN_PROP_NO = "no";

    public static final DBDBinaryFormatter[] BINARY_FORMATS = {
        new BinaryFormatterString(),
        new BinaryFormatterHex(),
        new BinaryFormatterBase64(),
    };
    public static final String TYPE_NAME_UUID = "UUID";
    public static final String TYPE_NAME_UUID2 = "uuid";

    public static final String DEFAULT_TIME_FORMAT = "HH:mm:ss";
    public static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd";
    public static final String DEFAULT_TIMESTAMP_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS";

    public static final String DEFAULT_ISO_TIMESTAMP_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'";
}

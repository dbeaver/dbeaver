/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model;

/**
 * General model constants
 */
public class DBConstants {

    public static final int METADATA_FETCH_SIZE = 1000;

    public static final String DATA_SOURCE_PROPERTY_USER = "user"; //NON-NLS-1
    public static final String DATA_SOURCE_PROPERTY_PASSWORD = "password"; //NON-NLS-1

    public static final String NULL_VALUE_LABEL = "[NULL]"; //NON-NLS-1

    public static final String PROP_ID_NAME = "name"; //NON-NLS-1
    public static final String PROP_ID_DESCRIPTION = "description"; //NON-NLS-1
    public static final String PROP_ID_TYPE_NAME = "typeName"; //NON-NLS-1
    public static final String PROP_ID_MAX_LENGTH = "maxLength"; //NON-NLS-1
    public static final String PROP_ID_NOT_NULL = "notNull"; //NON-NLS-1

    public static final String INTERNAL_PROP_PREFIX = "@dbeaver-"; //NON-NLS-1

    public static final String FORMAT_CSV = "CSV"; //NON-NLS-1
    public static final String FORMAT_SQL = "SQL"; //NON-NLS-1

    public static final String[] DEFAULT_DATATYPE_NAMES = {
        "varchar", //NON-NLS-1
        "varchar2", //NON-NLS-1
        "string",  //NON-NLS-1
        "char", //NON-NLS-1
        "integer", //NON-NLS-1
        "number" //NON-NLS-1
    };
}

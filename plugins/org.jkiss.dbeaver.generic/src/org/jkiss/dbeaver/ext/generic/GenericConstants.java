/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.generic;

import org.jkiss.dbeaver.ext.generic.model.MetaDataNameConverter;

/**
 * Generic provider constants
 */
public class GenericConstants {

    public static final String PARAM_QUERY_GET_ACTIVE_DB = "query-get-active-db";
    public static final String PARAM_QUERY_SET_ACTIVE_DB = "query-set-active-db";
    public static final String PARAM_META_CASE = "meta-case";
    // URL parameter for DB shutdown. Added to support Derby DB shutdown process
    public static final String PARAM_SHUTDOWN_URL_PARAM = "shutdown-url-param";

    public static enum MetaCase implements MetaDataNameConverter {
        NONE {
            public String convert(String name)
            {
                return name;
            }},
        UPPER{
            public String convert(String name)
            {
                return name.toUpperCase();
            }},
        LOWER{
            public String convert(String name)
            {
                return name.toLowerCase();
            }}
    }

}

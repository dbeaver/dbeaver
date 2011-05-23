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
    public static final String PARAM_ACTIVE_ENTITY_TYPE = "active-entity-type";

    public static final String ENTITY_TYPE_CATALOG = "catalog";
    public static final String ENTITY_TYPE_SCHEMA = "schema";

    // URL parameter for DB shutdown. Added to support Derby DB shutdown process
    public static final String PARAM_SHUTDOWN_URL_PARAM = "shutdown-url-param";
    public static final String TYPE_MODIFIER_IDENTITY = " IDENTITY";

    public static final String TERM_CATALOG = "catalog";
    public static final String TERM_SCHEMA = "schema";
    public static final String TERM_PROCEDURE = "procedure";

}

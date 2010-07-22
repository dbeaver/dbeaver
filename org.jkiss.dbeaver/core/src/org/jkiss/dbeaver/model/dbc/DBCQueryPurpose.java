/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.dbc;

/**
 * Statement purpose.
 *
 * Each query which executed withing application have some purpose.
 * Some of queries are instantiated by user, some are executed internally to obtain metadata, etc.
 * This enum defines different query purposes.
 *
 * Note: for sure, we can't detect ALL executed queries. Some of them are executed by drivers internally,
 * some are executed by plugins and not reported to query manager.
 */
public enum DBCQueryPurpose {

    MANUAL,     // User query
    SCRIPT,     // User script query
    DDL,        // DDL query, caused by some metadata modifications
    META,       // Metadata query, processed by data source providers internally
    OTHER       // Other types of queries

}

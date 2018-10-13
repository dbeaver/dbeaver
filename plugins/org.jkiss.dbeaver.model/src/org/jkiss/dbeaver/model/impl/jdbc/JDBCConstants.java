/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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

package org.jkiss.dbeaver.model.impl.jdbc;

/**
 * JDBCConstants
 */
public class JDBCConstants
{

    public static final String TABLE_CAT = "TABLE_CAT"; //$NON-NLS-1$
    public static final String TABLE_SCHEM = "TABLE_SCHEM"; //$NON-NLS-1$
    public static final String TABLE_CATALOG = "TABLE_CATALOG"; //$NON-NLS-1$ 
    public static final String TABLE_QUALIFIER = "TABLE_QUALIFIER"; //$NON-NLS-1$ // ODBC column for catalog names
    public static final String TABLE_OWNER = "TABLE_OWNER"; //$NON-NLS-1$ // ODBC column for schema names

    public static final String TABLE_NAME = "TABLE_NAME"; //$NON-NLS-1$
    public static final String TABLE_TYPE = "TABLE_TYPE"; //$NON-NLS-1$
    public static final String REMARKS = "REMARKS"; //$NON-NLS-1$
    public static final String TYPE_CAT = "TYPE_CAT"; //$NON-NLS-1$
    public static final String TYPE_SCHEM = "TYPE_SCHEM"; //$NON-NLS-1$
    public static final String TYPE_NAME = "TYPE_NAME"; //$NON-NLS-1$
    public static final String SELF_REFERENCING_COL_NAME = "SELF_REFERENCING_COL_NAME"; //$NON-NLS-1$
    public static final String REF_GENERATION = "REF_GENERATION"; //$NON-NLS-1$

    public static final String PROCEDURE_CAT = "PROCEDURE_CAT"; //$NON-NLS-1$
    public static final String PROCEDURE_SCHEM = "PROCEDURE_SCHEM"; //$NON-NLS-1$
    public static final String PROCEDURE_NAME = "PROCEDURE_NAME"; //$NON-NLS-1$
    public static final String PROCEDURE_TYPE = "PROCEDURE_TYPE"; //$NON-NLS-1$
    public static final String FUNCTION_NAME = "FUNCTION_NAME"; //$NON-NLS-1$
    public static final String SPECIFIC_NAME = "SPECIFIC_NAME"; //$NON-NLS-1$
    public static final String FUNCTION_TYPE = "FUNCTION_TYPE"; //$NON-NLS-1$

    public static final String DATA_TYPE = "DATA_TYPE"; //$NON-NLS-1$
    public static final String LENGTH = "LENGTH"; //$NON-NLS-1$
    public static final String SCALE = "SCALE"; //$NON-NLS-1$
    public static final String PRECISION = "PRECISION"; //$NON-NLS-1$
    public static final String RADIX = "RADIX"; //$NON-NLS-1$
    public static final String LITERAL_PREFIX = "LITERAL_PREFIX"; //$NON-NLS-1$
    public static final String LITERAL_SUFFIX = "LITERAL_SUFFIX"; //$NON-NLS-1$
    public static final String CREATE_PARAMS = "CREATE_PARAMS"; //$NON-NLS-1$
    public static final String NULLABLE = "NULLABLE"; //$NON-NLS-1$
    public static final String CASE_SENSITIVE = "CASE_SENSITIVE"; //$NON-NLS-1$
    public static final String SEARCHABLE = "SEARCHABLE"; //$NON-NLS-1$
    public static final String UNSIGNED_ATTRIBUTE = "UNSIGNED_ATTRIBUTE"; //$NON-NLS-1$
    public static final String FIXED_PREC_SCALE = "FIXED_PREC_SCALE"; //$NON-NLS-1$
    public static final String AUTO_INCREMENT = "AUTO_INCREMENT"; //$NON-NLS-1$
    public static final String LOCAL_TYPE_NAME = "LOCAL_TYPE_NAME"; //$NON-NLS-1$
    public static final String MINIMUM_SCALE = "MINIMUM_SCALE"; //$NON-NLS-1$
    public static final String MAXIMUM_SCALE = "MAXIMUM_SCALE"; //$NON-NLS-1$
    public static final String SQL_DATA_TYPE = "SQL_DATA_TYPE"; //$NON-NLS-1$
    public static final String SQL_DATETIME_SUB = "SQL_DATETIME_SUB"; //$NON-NLS-1$
    public static final String NUM_PREC_RADIX = "NUM_PREC_RADIX"; //$NON-NLS-1$

    public static final String COLUMN_NAME = "COLUMN_NAME"; //$NON-NLS-1$
    public static final String COLUMN_TYPE = "COLUMN_TYPE"; //$NON-NLS-1$
    public static final String COLUMN_SIZE = "COLUMN_SIZE"; //$NON-NLS-1$
    public static final String COLUMN_DEF = "COLUMN_DEF"; //$NON-NLS-1$
    public static final String BUFFER_LENGTH = "BUFFER_LENGTH"; //$NON-NLS-1$
    public static final String DECIMAL_DIGITS = "DECIMAL_DIGITS"; //$NON-NLS-1$
    public static final String CHAR_OCTET_LENGTH = "CHAR_OCTET_LENGTH"; //$NON-NLS-1$
    public static final String ORDINAL_POSITION = "ORDINAL_POSITION"; //$NON-NLS-1$
    public static final String IS_NULLABLE = "IS_NULLABLE"; //$NON-NLS-1$
    public static final String SCOPE_CATLOG = "SCOPE_CATLOG"; //$NON-NLS-1$
    public static final String SCOPE_SCHEMA = "SCOPE_SCHEMA"; //$NON-NLS-1$
    public static final String SCOPE_TABLE = "SCOPE_TABLE"; //$NON-NLS-1$
    public static final String SOURCE_DATA_TYPE = "SOURCE_DATA_TYPE"; //$NON-NLS-1$

    public static final String NON_UNIQUE = "NON_UNIQUE"; //$NON-NLS-1$
    public static final String INDEX_QUALIFIER = "INDEX_QUALIFIER"; //$NON-NLS-1$
    public static final String INDEX_CARDINALITY = "CARDINALITY"; //$NON-NLS-1$
    public static final String INDEX_NAME = "INDEX_NAME"; //$NON-NLS-1$
    public static final String TYPE = "TYPE"; //$NON-NLS-1$
    public static final String ASC_OR_DESC = "ASC_OR_DESC"; //$NON-NLS-1$
    public static final String CARDINALITY = "CARDINALITY"; //$NON-NLS-1$
    public static final String PAGES = "PAGES"; //$NON-NLS-1$
    public static final String FILTER_CONDITION = "FILTER_CONDITION"; //$NON-NLS-1$
    public static final String IS_AUTOINCREMENT = "IS_AUTOINCREMENT"; //$NON-NLS-1$
    public static final String IS_GENERATEDCOLUMN = "IS_GENERATEDCOLUMN"; //$NON-NLS-1$

    public static final String KEY_SEQ = "KEY_SEQ"; //$NON-NLS-1$
    public static final String PK_NAME = "PK_NAME"; //$NON-NLS-1$
    public static final String FK_NAME = "FK_NAME"; //$NON-NLS-1$

    public static final String PKTABLE_CAT = "PKTABLE_CAT"; //$NON-NLS-1$
    public static final String PKTABLE_SCHEM = "PKTABLE_SCHEM"; //$NON-NLS-1$
    public static final String PKTABLE_NAME = "PKTABLE_NAME"; //$NON-NLS-1$
    public static final String PKCOLUMN_NAME = "PKCOLUMN_NAME"; //$NON-NLS-1$
    public static final String FKTABLE_CAT = "FKTABLE_CAT"; //$NON-NLS-1$
    public static final String FKTABLE_SCHEM = "FKTABLE_SCHEM"; //$NON-NLS-1$
    public static final String FKTABLE_NAME = "FKTABLE_NAME"; //$NON-NLS-1$
    public static final String FKCOLUMN_NAME = "FKCOLUMN_NAME"; //$NON-NLS-1$
    public static final String UPDATE_RULE = "UPDATE_RULE"; //$NON-NLS-1$
    public static final String DELETE_RULE = "DELETE_RULE"; //$NON-NLS-1$
    public static final String DEFERRABILITY = "DEFERRABILITY"; //$NON-NLS-1$

    public static final String REF_GENERATION_SYSTEM = "SYSTEM"; //$NON-NLS-1$
    public static final String REF_GENERATION_USER = "USER"; //$NON-NLS-1$
    public static final String REF_GENERATION_DERIVED = "DERIVED"; //$NON-NLS-1$

    public static final String APPLICATION_NAME_CLIENT_PROPERTY = "ApplicationName";

    public static final String PROP_HOST = "host"; //$NON-NLS-1$
    public static final String PROP_PORT = "port"; //$NON-NLS-1$
    public static final String PROP_DATABASE = "database"; //$NON-NLS-1$
    public static final String PROP_SERVER = "server"; //$NON-NLS-1$
    public static final String PROP_FOLDER = "folder"; //$NON-NLS-1$
    public static final String PROP_FILE = "file"; //$NON-NLS-1$
    public static final String PROP_USER = "user"; //$NON-NLS-1$
    public static final String PROP_PASSWORD = "password"; //$NON-NLS-1$
}

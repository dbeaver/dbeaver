/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
package org.jkiss.dbeaver.model;

import org.jkiss.dbeaver.model.data.DBDBinaryFormatter;
import org.jkiss.dbeaver.model.impl.data.formatters.BinaryFormatterBase64;
import org.jkiss.dbeaver.model.impl.data.formatters.BinaryFormatterHex;
import org.jkiss.dbeaver.model.impl.data.formatters.BinaryFormatterString;

import java.util.Locale;

/**
 * General model constants
 */
public class DBConstants {

    /////////////////////////////////
    // Standard properties

    public static final String PROP_ID_NAME = "name"; //NON-NLS-1
    public static final String PROP_ID_DESCRIPTION = "description"; //NON-NLS-1
    public static final String PROP_ID_DATA_TYPE = "dataType"; //NON-NLS-1
    public static final String PROP_ID_REQUIRED = "required"; //NON-NLS-1
    public static final String PROP_ID_DEFAULT_VALUE = "defaultValue"; //NON-NLS-1
    public static final String PROP_ID_TYPE_NAME = "typeName"; //NON-NLS-1
    public static final String PROP_ID_MAX_LENGTH = "maxLength"; //NON-NLS-1
    public static final String PROP_ID_NOT_NULL = "notNull"; //NON-NLS-1
    public static final String PROP_ID_ENABLED = "enabled"; //NON-NLS-1
    public static final String PARAM_INIT_ON_TEST = "initOnTest"; //NON-NLS-1
    public static final String PARAM_OBJECT_DEFINITION_TEXT = "objectDefinitionText"; //NON-NLS-1
    public static final String PARAM_EXTENDED_DEFINITION_TEXT = "extendedDefinitionText"; //NON-NLS-1

    /////////////////////////////////
    // Variables

    public static final String VAR_CONTEXT_NAME = "context.name";
    public static final String VAR_CONTEXT_ID = "context.id";

    /////////////////////////////////
    // Misc

    public static final int METADATA_FETCH_SIZE = 1000;

    public static final String DATA_SOURCE_PROPERTY_USER = "user"; //NON-NLS-1
    public static final String DATA_SOURCE_PROPERTY_PASSWORD = "password"; //NON-NLS-1

    public static final String NULL_VALUE_LABEL = "[NULL]"; //NON-NLS-1

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

    public static final String TYPE_NAME_CHAR = "CHAR";
    public static final String TYPE_NAME_CHAR2 = TYPE_NAME_CHAR.toLowerCase(Locale.ENGLISH);
    public static final String TYPE_NAME_UUID = "UUID";
    public static final String TYPE_NAME_UUID2 = TYPE_NAME_UUID.toLowerCase(Locale.ENGLISH);
    public static final String TYPE_NAME_XML = "XML";
    public static final String TYPE_NAME_XML2 = TYPE_NAME_XML.toLowerCase(Locale.ENGLISH);
    public static final String TYPE_NAME_JSON = "JSON";
    public static final String TYPE_NAME_JSON2 = TYPE_NAME_JSON.toLowerCase(Locale.ENGLISH);

    public static final String DEFAULT_TIME_FORMAT = "HH:mm:ss";
    public static final String DEFAULT_TIME_TZ_FORMAT = "HH:mm:ss Z";
    public static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd";
    public static final String DEFAULT_TIMESTAMP_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS";
    public static final String DEFAULT_TIMESTAMP_TZ_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS Z";

    public static final String DEFAULT_ISO_TIMESTAMP_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'";

    public static final String LEGACY_DRIVERS_FOLDER = ".dbeaver-drivers";
    public static final String DEFAULT_DRIVERS_FOLDER = "drivers";

    public static final String MODEL_BUNDLE_ID = "org.jkiss.dbeaver.model";

    public static final String WORKSPACE_PROPS_FILE = "dbeaver-workspace.properties"; //$NON-NLS-1$

    public static final String LABEL_NA = "N/A";

    public static final String DEBUG_LOG_FILE_NAME = "dbeaver-debug.log";

    /////////////////////////////////
    // Property categories and features

    public static final String CAT_MAIN = "Main";
    public static final String CAT_STATISTICS = "Statistics";

    public static final String PROP_FEATURE_EXPENSIVE = "expensive";
    public static final String PROP_FEATURE_NUMERIC = "numeric";
    public static final String PROP_FEATURE_DATETME = "datetme";
    public static final String PROP_FEATURE_HIDDEN = "hidden";
    public static final String PROP_FEATURE_REMOTE = "remote";
    public static final String PROP_FEATURE_OPTIONAL = "optional";
    public static final String PROP_FEATURE_SPECIFIC = "specific";
    public static final String PROP_FEATURE_REQUIRED = "required";
    public static final String PROP_FEATURE_PASSWORD = "password";
    public static final String PROP_FEATURE_NAME = "name";
    public static final String PROP_FEATURE_MULTILINE = "multiline";
    public static final String PROP_FEATURE_EDIT_POSSIBLE = "editPossible";
    public static final String PROP_FEATURE_LINK_POSSIBLE = "linkPossible";
    public static final String PROP_FEATURE_HREF = "href";
    public static final String PROP_FEATURE_VIEWABLE = "viewable";

}

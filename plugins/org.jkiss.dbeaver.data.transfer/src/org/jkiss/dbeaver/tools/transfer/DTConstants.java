/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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
package org.jkiss.dbeaver.tools.transfer;

import org.jkiss.utils.StandardConstants;

/**
 * DataTransfer constants
 */
public class DTConstants {

    public static final String TASK_IMPORT = "dataImport";
    public static final String TASK_EXPORT = "dataExport";

    public static final String DEFAULT_TABLE_NAME_EXPORT = "export";

    public static final String PREF_FALLBACK_OUTPUT_DIRECTORY = "fallbackOutputDirectory";
    public static final String DEFAULT_FALLBACK_OUTPUT_DIRECTORY = System.getProperty(StandardConstants.ENV_TMP_DIR);

    public static final String PRODUCT_FEATURE_ADVANCED_DATA_TRANSFER = "database/data/transfer/advanced";

}

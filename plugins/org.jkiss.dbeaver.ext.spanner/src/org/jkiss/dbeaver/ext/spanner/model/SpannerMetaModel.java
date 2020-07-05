/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.spanner.model;

import org.jkiss.dbeaver.ext.generic.model.meta.GenericMetaModel;

import java.util.regex.Pattern;

/**
 * Spanner meta model
 */
public class SpannerMetaModel extends GenericMetaModel {

    public SpannerMetaModel() {
    }
    
    // The default schema in Cloud Spanner is an empty string. This ensures that this
    // default schema will be shown as 'DEFAULT' in DBeaver, and enables auto complete
    // for tables and views in both the DEFAULT schema, as well as the INFORMATION_SCHEMA
    // and SPANNER_SYS schemas.
    public boolean supportsNullSchemas() {
        return true;
    }

}

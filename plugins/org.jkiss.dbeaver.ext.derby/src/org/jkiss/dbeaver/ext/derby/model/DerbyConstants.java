/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.derby.model;

import org.jkiss.api.CompositeObjectId;

public class DerbyConstants {
    public static final String PROVIDER_ID = "generic";
    public static final String DRIVER_DERBY_ID = "derby";
    public static final CompositeObjectId DRIVER_DERBY_REFERENCE = new CompositeObjectId(PROVIDER_ID, DRIVER_DERBY_ID);
}
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

package org.jkiss.dbeaver.tools.project;

/**
 * Import/Export constants
 */
class ExportConstants {

    public static final String ARCHIVE_FILE_EXT = ".dbp"; //NON-NLS-1

    public static final String DIR_PROJECTS = "projects"; //NON-NLS-1
    public static final String DIR_DRIVERS = "drivers"; //NON-NLS-1

    public static final String META_FILENAME = "meta.xml"; //NON-NLS-1

    public static final String TAG_ARCHIVE = "archive"; //NON-NLS-1
    public static final String TAG_SOURCE = "source";
    public static final String TAG_PROJECTS = "projects"; //NON-NLS-1
    public static final String TAG_PROJECT = "project"; //NON-NLS-1
    public static final String TAG_RESOURCE = "resource"; //NON-NLS-1
    public static final String TAG_ATTRIBUTE = "attribute"; //NON-NLS-1
    public static final String TAG_LIBRARIES = "libraries"; //NON-NLS-1

    public static final String ATTR_VERSION = "version"; //NON-NLS-1
    public static final String ATTR_HOST = "host";
    public static final String ATTR_ADDRESS = "address";
    public static final String ATTR_TIME = "time";
    public static final String ATTR_QUALIFIER = "qualifier"; //NON-NLS-1
    public static final String ATTR_NAME = "name"; //NON-NLS-1
    public static final String ATTR_VALUE = "value"; //NON-NLS-1
    public static final String ATTR_DIRECTORY = "directory"; //NON-NLS-1
    public static final String ATTR_DESCRIPTION = "description"; //NON-NLS-1
    public static final String ATTR_CHARSET = "charset"; //NON-NLS-1
    public static final String ATTR_PATH = "path"; //NON-NLS-1
    public static final String ATTR_FILE = "file"; //NON-NLS-1

    public static final int ARCHIVE_VERSION_1 = 1;
    public static final int ARCHIVE_VERSION_CURRENT = ARCHIVE_VERSION_1;

}

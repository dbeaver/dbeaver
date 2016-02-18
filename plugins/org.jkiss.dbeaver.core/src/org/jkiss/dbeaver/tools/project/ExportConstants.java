/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
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

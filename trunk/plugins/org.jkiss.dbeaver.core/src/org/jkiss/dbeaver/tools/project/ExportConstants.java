/*
 * Copyright (C) 2010-2015 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
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

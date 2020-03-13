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
package org.jkiss.utils;

/**
 * Standard Java constants
 */
public abstract class StandardConstants {

    public static final String ENV_LINE_SEPARATOR = "line.separator";
    public static final String ENV_PATH_SEPARATOR = "path.separator";

    public static final String ENV_TMP_DIR = "java.io.tmpdir";
    public static final String ENV_FILE_ENCODING = "file.encoding";
    public static final String ENV_CONSOLE_ENCODING = "console.encoding";

    public static final String ENV_USER_HOME = "user.home";
    public static final String ENV_USER_NAME = "user.name";
    public static final String ENV_USER_TIMEZONE = "user.timezone";
    public static final String ENV_OS_NAME = "os.name";
    public static final String ENV_OS_VERSION = "os.version";
    public static final String ENV_OS_ARCH = "os.arch";

    public static final String ENV_JAVA_VERSION = "java.version";
    public static final String ENV_JAVA_VENDOR = "java.vendor";
    public static final String ENV_JAVA_ARCH = "sun.arch.data.model";
    public static final String ENV_JAVA_CLASSPATH = "java.class.path";

    public static final int MIN_PORT_VALUE = 0;
    public static final int MAX_PORT_VALUE = 65535;
}

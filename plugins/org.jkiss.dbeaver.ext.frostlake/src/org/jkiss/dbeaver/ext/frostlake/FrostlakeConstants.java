/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.frostlake;

/**
 * Frostlake connection settings and URL prefixes.
 */
public class FrostlakeConstants {

    /** jdbc:frostlake://host:port/db — talks to a running DatabaseHttpServer. */
    public static final String URL_PREFIX_SERVER = "jdbc:frostlake://";
    /** jdbc:frostlake:direct:<name> — in-process, one shared engine per name, nothing persisted. */
    public static final String URL_PREFIX_DIRECT = "jdbc:frostlake:direct:";
    /** jdbc:frostlake:file:<dir> — in-process and persistent, pinned to a directory. */
    public static final String URL_PREFIX_FILE = "jdbc:frostlake:file:";

    public static final int DEFAULT_PORT = 18082;

    private FrostlakeConstants() {
        // constants only
    }
}

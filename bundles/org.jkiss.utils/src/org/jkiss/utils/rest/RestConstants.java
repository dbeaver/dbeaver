/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.utils.rest;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class RestConstants {
    public static final int SC_OK = 200;
    public static final int SC_FORBIDDEN = 403;
    public static final int SC_UNSUPPORTED = 405;
    public static final int SC_NOT_FOUND = 404;
    public static final int SC_SERVER_ERROR = 500;

    static final Gson DEFAULT_GSON = new GsonBuilder()
        .setLenient()
        .disableHtmlEscaping()
        .serializeNulls()
        .create();
}

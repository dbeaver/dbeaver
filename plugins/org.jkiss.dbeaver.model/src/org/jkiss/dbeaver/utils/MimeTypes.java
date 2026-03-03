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

package org.jkiss.dbeaver.utils;

import org.jkiss.utils.HttpConstants;

public class MimeTypes {

    public static final String TEXT = "text";
    public static final String TEXT_PLAIN = HttpConstants.CONTENT_TYPE_TEXT_PLAIN;
    public static final String TEXT_HTML = "text/html";
    public static final String TEXT_XML = "text/xml";
    public static final String TEXT_CSS = "text/css";
    public static final String TEXT_JSON = "text/json";

    public static final String APPLICATION_JSON = HttpConstants.CONTENT_TYPE_JSON;
    public final static String OCTET_STREAM = HttpConstants.CONTENT_TYPE_OCTET_STREAM;
    public static final String MULTIPART_ANY = "multipart/*";
    public static final String MULTIPART_RELATED = "multipart/related";

}

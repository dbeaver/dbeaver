/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.dbeaver.db.cdata.registry;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

public final class CDataLicenseProbe {
    static final String LICENSE_PREFIX = "CDATA_LICENSE=";
    static final String NODE_PREFIX = "CDATA_NODE=";
    static final String ERROR_PREFIX = "CDATA_ERROR=";

    private CDataLicenseProbe() {
    }

    public static void main(String[] args) {
        if (args.length != 1) {
            System.exit(2);
            return;
        }
        try {
            Class<?> driverClass = Class.forName(args[0]);
            Object information = driverClass.getMethod("getInformation", String.class).invoke(null, "");
            if (!(information instanceof Map<?, ?> informationMap)) {
                System.exit(2);
                return;
            }
            System.out.println(LICENSE_PREFIX + encode(informationMap.get("License")));
            System.out.println(NODE_PREFIX + encode(informationMap.get("NodeId")));
        } catch (Throwable e) {
            Throwable cause = e.getCause() == null ? e : e.getCause();
            String message = cause.getMessage();
            String error = cause.getClass().getName() + (message == null || message.isBlank() ? "" : ": " + message);
            if (error.length() > 500) {
                error = error.substring(0, 500);
            }
            System.out.println(ERROR_PREFIX + encode(error));
            System.exit(2);
        }
    }

    private static String encode(Object value) {
        String text = value == null ? "" : String.valueOf(value);
        return Base64.getEncoder().encodeToString(text.getBytes(StandardCharsets.UTF_8));
    }
}

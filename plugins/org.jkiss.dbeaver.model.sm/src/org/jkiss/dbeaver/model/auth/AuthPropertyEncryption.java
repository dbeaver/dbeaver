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
package org.jkiss.dbeaver.model.auth;

import org.jkiss.utils.SecurityUtils;

/**
 * Auth provider property encryption
 */
public enum AuthPropertyEncryption {
    // Non-secure property
    none {
        @Override
        public String encrypt(String salt, String value) {
            return value;
        }
    },
    // Secure property, value passed to provided as-is
    plain {
        @Override
        public String encrypt(String salt, String value) {
            return value;
        }
    },
    // Secure property, value passed as MD5 hash
    hash {
        @Override
        public String encrypt(String salt, String value) {
            return SecurityUtils.makeDigest(salt, value);
        }
    };

    public abstract String encrypt(String salt, String value);
}

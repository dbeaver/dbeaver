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

package org.jkiss.dbeaver.registry;

import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.WinReg;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.utils.RuntimeUtils;

import java.util.Collections;
import java.util.Map;

/**
 * LocalSystemRegistry
 */
public class LocalSystemRegistry {

    public interface Registry {
        boolean registryKeyExists(String root, String key);

        boolean registryValueExists(String root, String key, String value);

        String[] registryGetKeys(String root, String keyPath);

        Map<String, Object> registryGetValues(String root, String keyPath);

        String registryGetStringValue(String root, String key, String value);
    }

    private static final Log log = Log.getLog(BasePolicyDataProvider.class);
    private static Registry instance;

    public static Registry getInstance() {
        if (instance == null) {
            if (RuntimeUtils.isWindows()) {
                instance = new WindowsRegistry();
            } else {
                instance = new VoidRegistry();
            }
        }
        return instance;
    }

    private static class WindowsRegistry implements Registry {
        @Override
        public boolean registryKeyExists(String root, String key) {
            return Advapi32Util.registryKeyExists(getRootHkey(root), key);
        }

        @Override
        public boolean registryValueExists(String root, String key, String value) {
            return Advapi32Util.registryValueExists(getRootHkey(root), key, value);
        }

        @Override
        public String[] registryGetKeys(String root, String keyPath) {
            return Advapi32Util.registryGetKeys(getRootHkey(root), keyPath);
        }

        @Override
        public Map<String, Object> registryGetValues(String root, String keyPath) {
            return Advapi32Util.registryGetValues(getRootHkey(root), keyPath);
        }

        @Override
        public String registryGetStringValue(String root, String key, String value) {
            return Advapi32Util.registryGetStringValue(getRootHkey(root), key, value);
        }

        private WinReg.HKEY getRootHkey(String root) {
            try {
                return (WinReg.HKEY) WinReg.class.getField(root).get(null);
            } catch (Exception e) {
                log.debug(e);
                return new WinReg.HKEY();
            }
        }

    }

    private static class VoidRegistry implements Registry {
        @Override
        public boolean registryKeyExists(String root, String key) {
            return false;
        }

        @Override
        public boolean registryValueExists(String root, String key, String value) {
            return false;
        }

        @Override
        public String[] registryGetKeys(String root, String keyPath) {
            return new String[0];
        }

        @Override
        public Map<String, Object> registryGetValues(String root, String keyPath) {
            return Collections.emptyMap();
        }

        @Override
        public String registryGetStringValue(String root, String key, String value) {
            return null;
        }
    }

}

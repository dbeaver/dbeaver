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

package org.jkiss.dbeaver.utils;

import org.eclipse.core.runtime.Platform;
import org.jkiss.dbeaver.runtime.IVariableResolver;
import org.jkiss.utils.StandardConstants;

import java.io.File;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Locale;

/**
 * SystemVariablesResolver
 */
public class SystemVariablesResolver implements IVariableResolver {

    public static SystemVariablesResolver INSTANCE = new SystemVariablesResolver();

    public static final String VAR_APP_NAME = "application.name";
    public static final String VAR_APP_VERSION = "application.version";
    public static final String VAR_APP_PATH = "application.path";
    public static final String VAR_WORKSPACE = "workspace";
    public static final String VAR_HOME = "home";
    public static final String VAR_DBEAVER_HOME = "dbeaver_home";
    public static final String VAR_LOCAL_IP = "local.ip";

    @Override
    public String get(String name) {
        name = name.toLowerCase(Locale.ENGLISH);
        switch (name) {
            case VAR_APP_NAME:
                return GeneralUtils.getProductName();
            case VAR_APP_VERSION:
                return GeneralUtils.getProductVersion().toString();
            case VAR_HOME:
                return getUserHome();
            case VAR_WORKSPACE:
                return getWorkspacePath();
            case VAR_DBEAVER_HOME:
            case VAR_APP_PATH:
                return getInstallPath();
            case VAR_LOCAL_IP:
                try {
                    return InetAddress.getLocalHost().getHostAddress();
                } catch (UnknownHostException e) {
                    return "127.0.0.1";
                }
        }
        return null;
    }

    public static String getInstallPath() {
        return getPlainPath(Platform.getInstallLocation().getURL());
    }

    public static String getWorkspacePath() {
        return getPlainPath(Platform.getInstanceLocation().getURL());
    }

    public static String getUserHome() {
        return System.getProperty(StandardConstants.ENV_USER_HOME);
    }

    private static String getPlainPath(URL url) {
        try {
            File file = new File(url.toURI());
            return file.getAbsolutePath();
        } catch (Exception e) {
            return url.toString();
        }
    }

}

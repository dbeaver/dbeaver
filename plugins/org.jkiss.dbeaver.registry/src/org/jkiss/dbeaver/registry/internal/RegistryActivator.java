/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
package org.jkiss.dbeaver.registry.internal;

import org.eclipse.core.runtime.Plugin;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.dbeaver.utils.SystemVariablesResolver;
import org.jkiss.utils.CommonUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

public class RegistryActivator extends Plugin {

    public RegistryActivator() {
    }

    @Override
    public void start(@NotNull BundleContext context) throws Exception {
        super.start(context);


        setJnaNativePath(context);
    }

    private static void setJnaNativePath(@NotNull BundleContext context) {
        {
            String installPath = SystemVariablesResolver.getInstallPath();
            Path pluginsPath = Path.of(installPath).resolve("plugins");
            Bundle jnaBundle = Arrays.stream(context.getBundles()).filter(b -> "com.sun.jna".equals(b.getSymbolicName()))
                .findFirst().orElse(null);
            if (jnaBundle != null) {
                String location = jnaBundle.getLocation();
                location = CommonUtils.removeTrailingSlash(location);
                int divPos = location.lastIndexOf("/");
                if (divPos != -1) {
                    String bundleFolderName = location.substring(divPos + 1);
                    Path jnaBundlePath = pluginsPath.resolve(bundleFolderName).resolve("com/sun/jna");
                    if (Files.exists(jnaBundlePath)) {
                        String osName = null;
                        if (RuntimeUtils.isMacOS()) {
                            osName = "darwin";
                        } else if (RuntimeUtils.isLinux()) {
                            osName = "linux";
                        } else if (RuntimeUtils.isWindows()) {
                            osName = "win32";
                        }
                        String osArch = null;
                        if (RuntimeUtils.isOSArchAMD64()) {
                            osArch = "x86-64";
                        } else if (RuntimeUtils.isOSArchAArch64()) {
                            osArch = "aarch64";
                        }
                        if (osName != null && osArch != null) {
                            System.setProperty(
                                "jna.boot.library.path",
                                jnaBundlePath.resolve(osName + "-" + osArch).toAbsolutePath().toString());
                        }
                    }
                }
            }
        }
    }

    @Override
    public void stop(@NotNull BundleContext context) throws Exception {
        super.stop(context);
    }

}

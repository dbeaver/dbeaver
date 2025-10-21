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
package org.jkiss.dbeaver.ui.registry;

import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Shell;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.registry.confirmation.ConfirmationDescriptor;
import org.jkiss.dbeaver.registry.confirmation.ConfirmationRegistry;
import org.jkiss.dbeaver.ui.dialogs.ConfirmationDialog;
import org.jkiss.dbeaver.ui.internal.UIActivator;
import org.jkiss.dbeaver.utils.RuntimeUtils;

import java.util.Locale;
import java.util.ResourceBundle;

// TODO change naming
public class DesktopConfirmationRegistry {

    private static final Log log = Log.getLog(DesktopConfirmationRegistry.class);

    public static int confirmAction(@Nullable Shell shell, @NotNull String id, int type, int imageType, @NotNull Object... args) {
        ConfirmationDescriptor descriptor = ConfirmationRegistry.getInstance().getConfirmation(id);

        String toggleMessage = descriptor.getToggleMessage();
        if ("default".equals(descriptor.getToggleMessage())) {
            ResourceBundle resourceBundle = RuntimeUtils.getBundleLocalization(
                UIActivator.getDefault().getBundle(),
                Locale.getDefault().getLanguage()
            );
            try {
                toggleMessage = resourceBundle.getString("confirm.general.toggleMessage");
            } catch (Exception e) {
                log.debug(e);
            }
        }

        return ConfirmationDialog.open(
            type,
            imageType == -1 ? type : imageType,
            shell,
            NLS.bind(descriptor.getTitle(), args),
            NLS.bind(descriptor.getMessage(), args),
            toggleMessage != null ? NLS.bind(toggleMessage, args) : null,
            false,
            ConfirmationDialog.PREF_KEY_PREFIX + id
        );
    }
}

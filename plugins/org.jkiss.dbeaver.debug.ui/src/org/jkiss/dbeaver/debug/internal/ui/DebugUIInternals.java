/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
 * Copyright (C) 2017 Alexander Fedorov (alexander.fedorov@jkiss.org)
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
package org.jkiss.dbeaver.debug.internal.ui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.expressions.Expression;
import org.eclipse.core.expressions.IEvaluationContext;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.actions.LaunchConfigurationAction;
import org.eclipse.debug.internal.ui.actions.LaunchShortcutAction;
import org.eclipse.debug.internal.ui.launchConfigurations.LaunchConfigurationManager;
import org.eclipse.debug.internal.ui.launchConfigurations.LaunchShortcutExtension;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.activities.WorkbenchActivityHelper;
import org.jkiss.dbeaver.debug.core.DebugCore;

@SuppressWarnings("restriction")
public class DebugUIInternals {

    public static ILaunchConfiguration isSharedConfig(Object receiver)
    {
        LaunchConfigurationManager launchConfigurationManager = DebugUIPlugin.getDefault()
                .getLaunchConfigurationManager();
        return launchConfigurationManager.isSharedConfig(receiver);
    }

    public static IAction createConfigurationAction(ILaunchConfiguration config, String mode, int accelerator)
    {
        if (DebugCore.canLaunch(config, mode)) {
            String configName = config.getName();
            ImageDescriptor imageDescriptor = DebugUITools.getDefaultImageDescriptor(config);
            IAction action = new LaunchConfigurationAction(config, mode, configName, imageDescriptor, accelerator);
            return action;
        }
        return null;
    }

    public static Map<IAction, String> createShortcutActions(Object[] selected, String mode,
            int accelerator)
    {
        Map<IAction, String> result = new LinkedHashMap<IAction, String>();
        if (selected == null) {
            return result;
        }
        List<Object> selection = Arrays.asList(selected);
        IEvaluationContext context = DebugUIPlugin.createEvaluationContext(selection);
        context.setAllowPluginActivation(true);
        context.addVariable("selection", selection); //$NON-NLS-1$
        List<LaunchShortcutExtension> allShortCuts = DebugUIPlugin.getDefault().getLaunchConfigurationManager()
                .getLaunchShortcuts();
        List<LaunchShortcutExtension> filteredShortCuts = new ArrayList<LaunchShortcutExtension>();
        Iterator<LaunchShortcutExtension> iter = allShortCuts.iterator();

        while (iter.hasNext()) {
            LaunchShortcutExtension ext = iter.next();
            if (WorkbenchActivityHelper.filterItem(ext)) {
                continue;
            }
            try {
                Expression expr = ext.getContextualLaunchEnablementExpression();
                if (ext.evalEnablementExpression(context, expr)) {
                    filteredShortCuts.add(ext);
                }
            } catch (CoreException e) {
                IStatus status = new Status(IStatus.ERROR, DebugUIPlugin.getUniqueIdentifier(), "Launch shortcut '" //$NON-NLS-1$
                        + ext.getId() + "' enablement expression caused exception. Shortcut was removed.", e); //$NON-NLS-1$
                DebugUIPlugin.log(status);
                iter.remove();
            }
        }

        for (LaunchShortcutExtension ext : filteredShortCuts) {
            for (String supported : ext.getModes()) {
                if (supported.equals(mode)) {
                    LaunchShortcutAction action = new LaunchShortcutAction(supported, ext);
                    action.setActionDefinitionId(ext.getId() + "." + supported); //$NON-NLS-1$
                    String helpContextId = ext.getHelpContextId();
                    if (helpContextId != null) {
                        PlatformUI.getWorkbench().getHelpSystem().setHelp(action, helpContextId);
                    }
                    StringBuffer label = new StringBuffer();
                    if (accelerator >= 0 && accelerator < 10) {
                        // add the numerical accelerator
                        label.append('&');
                        label.append(accelerator);
                        label.append(' ');
                    }
                    String contextLabel = ext.getContextLabel(supported);
                    // replace default action label with context label if
                    // specified.
                    label.append((contextLabel != null) ? contextLabel : action.getText());
                    action.setText(label.toString());
                    String category = ext.getCategory();
                    result.put(action, category);
                    accelerator++;
                }
            }
        }
        return result;
    }

}

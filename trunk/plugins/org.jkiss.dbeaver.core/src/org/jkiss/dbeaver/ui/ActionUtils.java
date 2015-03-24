/*
 * Copyright (C) 2010-2015 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.jkiss.dbeaver.ui;

import org.eclipse.core.commands.ParameterizedCommand;
import org.eclipse.jface.bindings.Binding;
import org.jkiss.dbeaver.core.Log;
import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.common.NotDefinedException;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.ContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.bindings.TriggerSequence;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.*;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.keys.IBindingService;
import org.eclipse.ui.menus.CommandContributionItem;
import org.eclipse.ui.menus.CommandContributionItemParameter;
import org.eclipse.ui.services.IEvaluationService;
import org.eclipse.ui.services.IServiceLocator;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;

/**
 * NavigatorUtils
 */
public class ActionUtils
{
    static final Log log = Log.getLog(ActionUtils.class);

    public static CommandContributionItem makeCommandContribution(IServiceLocator serviceLocator, String commandId)
    {
        return makeCommandContribution(serviceLocator, commandId, CommandContributionItem.STYLE_PUSH);
    }

    public static CommandContributionItem makeCommandContribution(IServiceLocator serviceLocator, String commandId, int style)
    {
        return new CommandContributionItem(new CommandContributionItemParameter(
            serviceLocator,
            null,
            commandId,
            style));
    }

    public static CommandContributionItem makeCommandContribution(IServiceLocator serviceLocator, String commandId, int style, ImageDescriptor icon)
    {
        CommandContributionItemParameter parameters = new CommandContributionItemParameter(
            serviceLocator,
            null,
            commandId,
            style);
        parameters.icon = icon;
        return new CommandContributionItem(parameters);
    }

    public static CommandContributionItem makeCommandContribution(IServiceLocator serviceLocator, String commandId, String name, ImageDescriptor imageDescriptor)
    {
        return makeCommandContribution(serviceLocator, commandId, name, imageDescriptor, null, false);
    }

    public static ContributionItem makeActionContribution(
        IAction action,
        boolean showText)
    {
        ActionContributionItem item = new ActionContributionItem(action);
        if (showText) {
            item.setMode(ActionContributionItem.MODE_FORCE_TEXT);
        }
        return item;
    }

    public static CommandContributionItem makeCommandContribution(
        IServiceLocator serviceLocator,
        String commandId,
        String name,
        @Nullable ImageDescriptor imageDescriptor,
        @Nullable String toolTip,
        boolean showText)
    {
        final CommandContributionItemParameter contributionParameters = new CommandContributionItemParameter(
            serviceLocator,
            null,
            commandId,
            null,
            imageDescriptor,
            null,
            null,
            name,
            null,
            toolTip,
            CommandContributionItem.STYLE_PUSH,
            null,
            false);
        if (showText) {
            contributionParameters.mode = CommandContributionItem.MODE_FORCE_TEXT;
        }
        return new CommandContributionItem(contributionParameters);
    }

    public static boolean isCommandEnabled(String commandId, IWorkbenchPart part)
    {
        if (commandId != null && part != null) {
            try {
                //Command cmd = new Command();
                ICommandService commandService = (ICommandService)part.getSite().getService(ICommandService.class);
                if (commandService != null) {
                    Command command = commandService.getCommand(commandId);
                    return command != null && command.isEnabled();
                }
            } catch (Exception e) {
                log.error("Could not execute command '" + commandId + "'", e);
            }
        }
        return false;
    }

    @Nullable
    public static String findCommandDescription(String commandId, IServiceLocator serviceLocator, boolean shortcutOnly)
    {
        String commandName = null;
        String shortcut = null;
        ICommandService commandService = (ICommandService)serviceLocator.getService(ICommandService.class);
        if (commandService != null) {
            Command command = commandService.getCommand(commandId);
            if (command != null && command.isDefined()) {
                try {
                    commandName = command.getName();
                } catch (NotDefinedException e) {
                    log.debug(e);
                }
            }
        }
        IBindingService bindingService = (IBindingService)serviceLocator.getService(IBindingService.class);
        if (bindingService != null) {
            TriggerSequence sequence = null;
            for (Binding b : bindingService.getBindings()) {
                ParameterizedCommand parameterizedCommand = b.getParameterizedCommand();
                if (parameterizedCommand != null && commandId.equals(parameterizedCommand.getId())) {
                    sequence = b.getTriggerSequence();
                }
            }
            if (sequence == null) {
                sequence = bindingService.getBestActiveBindingFor(commandId);
            }
            if (sequence != null) {
                shortcut = sequence.format();
            }
        }
        if (shortcutOnly) {
            return shortcut == null ? "?" : shortcut;
        }
        if (shortcut == null) {
            return commandName;
        }
        if (commandName == null) {
            return shortcut;
        }
        return commandName + " (" + shortcut + ")";
    }

    public static void runCommand(String commandId, IServiceLocator serviceLocator)
    {
        if (commandId != null) {
            try {
                //Command cmd = new Command();
                ICommandService commandService = (ICommandService)serviceLocator.getService(ICommandService.class);
                if (commandService != null) {
                    Command command = commandService.getCommand(commandId);
                    if (command != null && command.isEnabled()) {
                        IHandlerService handlerService = (IHandlerService) serviceLocator.getService(IHandlerService.class);
                        handlerService.executeCommand(commandId, null);
                    }
                }
            } catch (Exception e) {
                log.error("Could not execute command '" + commandId + "'", e);
            }
        }
    }

    public static IAction makeAction(
        @NotNull final IActionDelegate actionDelegate,
        @Nullable IWorkbenchSite site,
        @Nullable ISelection selection,
        @Nullable String text,
        @Nullable ImageDescriptor image,
        @Nullable String toolTip)
    {
        Action actionImpl = new Action() {
            @Override
            public void run() {
                actionDelegate.run(this);
            }
        };
        if (text != null) {
            actionImpl.setText(text);
        }
        if (image != null) {
            actionImpl.setImageDescriptor(image);
        }
        if (toolTip != null) {
            actionImpl.setToolTipText(toolTip);
        }

        actionDelegate.selectionChanged(actionImpl, selection);

        if (site != null) {
            if (actionDelegate instanceof IObjectActionDelegate && site instanceof IWorkbenchPartSite) {
                ((IObjectActionDelegate)actionDelegate).setActivePart(actionImpl, ((IWorkbenchPartSite) site).getPart());
            } else if (actionDelegate instanceof IWorkbenchWindowActionDelegate) {
                ((IWorkbenchWindowActionDelegate)actionDelegate).init(site.getWorkbenchWindow());
            }
        }

        return actionImpl;
    }

    public static void evaluatePropertyState(String propertyName)
    {
        IEvaluationService service = (IEvaluationService) PlatformUI.getWorkbench().getService(IEvaluationService.class);
        if (service != null) {
            try {
                service.requestEvaluation(propertyName);
            } catch (Exception e) {
                log.warn("Error evaluating property [" + propertyName + "]");
            }
        }
    }

}

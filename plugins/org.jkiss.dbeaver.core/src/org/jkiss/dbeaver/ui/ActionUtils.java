/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.commands.Command;
import org.eclipse.jface.action.*;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.*;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.menus.CommandContributionItem;
import org.eclipse.ui.menus.CommandContributionItemParameter;
import org.eclipse.ui.services.IServiceLocator;

/**
 * NavigatorUtils
 */
public class ActionUtils
{
    static final Log log = LogFactory.getLog(ActionUtils.class);

    public static CommandContributionItem makeCommandContribution(IServiceLocator serviceLocator, String commandId)
    {
        return new CommandContributionItem(new CommandContributionItemParameter(
            serviceLocator,
            null,
            commandId,
            CommandContributionItem.STYLE_PUSH));
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
        ImageDescriptor imageDescriptor,
        String toolTip,
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
        if (commandId != null) {
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

    public static IAction makeAction(final IActionDelegate actionDelegate, IWorkbenchPart part, ISelection selection, String text, ImageDescriptor image, String toolTip)
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

        if (part != null) {
            if (actionDelegate instanceof IObjectActionDelegate) {
                ((IObjectActionDelegate)actionDelegate).setActivePart(actionImpl, part);
            } else if (actionDelegate instanceof IWorkbenchWindowActionDelegate) {
                ((IWorkbenchWindowActionDelegate)actionDelegate).init(part.getSite().getWorkbenchWindow());
            }
        }

        return actionImpl;
    }

}

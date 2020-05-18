/*******************************************************************************
 * Copyright (c) 2004, 2011 Tasktop Technologies and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Tasktop Technologies - initial API and implementation
 *******************************************************************************/
package org.jkiss.dbeaver.ui.notifications;

import org.eclipse.jface.action.LegacyActionTools;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Rob Elves
 * @author Mik Kersten
 */
public class NotificationPopup extends AbstractWorkbenchNotificationPopup {

    private static final int NUM_NOTIFICATIONS_TO_DISPLAY = 4;

    public static final Color HYPERLINK_WIDGET_COLOR = new Color(Display.getDefault(), 12, 81, 172);

    private List<AbstractNotification> notifications;

    public NotificationPopup(Shell parent) {
        super(parent.getDisplay());
    }

    @Override
    protected void createContentArea(Composite parent) {
        int count = 0;
        for (final AbstractNotification notification : notifications) {
            Composite notificationComposite = new Composite(parent, SWT.NO_FOCUS);
            GridLayout gridLayout = new GridLayout(2, false);
            GridDataFactory.fillDefaults().grab(true, false).align(SWT.FILL, SWT.TOP).applyTo(notificationComposite);
            notificationComposite.setLayout(gridLayout);
            notificationComposite.setBackground(parent.getBackground());

            if (count < NUM_NOTIFICATIONS_TO_DISPLAY) {
                final Label notificationLabelIcon = new Label(notificationComposite, SWT.NO_FOCUS);
                notificationLabelIcon.setBackground(parent.getBackground());
                if (notification instanceof AbstractUiNotification) {
                    notificationLabelIcon.setImage(((AbstractUiNotification) notification).getNotificationKindImage());
                }

                final ScalingHyperlink itemLink = new ScalingHyperlink(notificationComposite, SWT.BEGINNING
                    | SWT.NO_FOCUS);
                GridDataFactory.fillDefaults().grab(true, false).align(SWT.FILL, SWT.TOP).applyTo(itemLink);
                itemLink.setForeground(HYPERLINK_WIDGET_COLOR);
                itemLink.registerMouseTrackListener();
                itemLink.setText(LegacyActionTools.escapeMnemonics(notification.getLabel()));
                if (notification instanceof AbstractUiNotification) {
                    itemLink.setImage(((AbstractUiNotification) notification).getNotificationImage());
                }
                itemLink.setBackground(parent.getBackground());
                itemLink.addHyperlinkListener(new HyperlinkAdapter() {
                    @Override
                    public void linkActivated(HyperlinkEvent e) {
                        if (notification instanceof AbstractUiNotification) {
                            ((AbstractUiNotification) notification).open();
                        }
                        IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
                        if (window != null) {
                            Shell windowShell = window.getShell();
                            if (windowShell != null) {
                                if (windowShell.getMinimized()) {
                                    windowShell.setMinimized(false);
                                }

                                windowShell.open();
                                windowShell.forceActive();
                            }
                        }
                    }
                });

                String descriptionText = null;
                if (notification.getDescription() != null) {
                    descriptionText = notification.getDescription();
                }
                if (descriptionText != null && !descriptionText.trim().equals("")) { //$NON-NLS-1$
                    Label descriptionLabel = new Label(notificationComposite, SWT.NO_FOCUS);
                    descriptionLabel.setText(LegacyActionTools.escapeMnemonics(descriptionText));
                    descriptionLabel.setBackground(parent.getBackground());
                    GridDataFactory.fillDefaults()
                        .span(2, SWT.DEFAULT)
                        .grab(true, false)
                        .align(SWT.FILL, SWT.TOP)
                        .applyTo(descriptionLabel);
                }
            } else {
                int numNotificationsRemain = notifications.size() - count;
                ScalingHyperlink remainingLink = new ScalingHyperlink(notificationComposite, SWT.NO_FOCUS);
                remainingLink.setForeground(HYPERLINK_WIDGET_COLOR);
                remainingLink.registerMouseTrackListener();
                remainingLink.setBackground(parent.getBackground());

                remainingLink.setText(NLS.bind("{0} more", numNotificationsRemain)); //$NON-NLS-1$
                GridDataFactory.fillDefaults().span(2, SWT.DEFAULT).applyTo(remainingLink);
                remainingLink.addHyperlinkListener(new HyperlinkAdapter() {
                    @Override
                    public void linkActivated(HyperlinkEvent e) {
                        // FIXME
                        //						TasksUiUtil.openTasksViewInActivePerspective().setFocus();
                        IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
                        if (window != null) {
                            Shell windowShell = window.getShell();
                            if (windowShell != null) {
                                windowShell.setMaximized(true);
                                windowShell.open();
                            }
                        }
                    }
                });
                break;
            }
            count++;
        }
    }

    @Override
    protected void createTitleArea(Composite parent) {
        super.createTitleArea(parent);
    }

    public List<AbstractNotification> getNotifications() {
        return new ArrayList<>(notifications);
    }

    public void setContents(List<AbstractNotification> notifications) {
        this.notifications = notifications;
    }

}
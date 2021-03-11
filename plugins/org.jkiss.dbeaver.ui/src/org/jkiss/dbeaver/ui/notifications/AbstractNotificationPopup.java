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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.resource.DeviceResourceException;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.jface.resource.ResourceManager;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UIStyles;
import org.jkiss.dbeaver.utils.GeneralUtils;

/**
 * A popup window with a title bar and message area for displaying notifications.
 *
 * @author Benjamin Pasero
 * @author Mik Kersten
 * @author Steffen Pingel
 * @since 3.7
 */
public abstract class AbstractNotificationPopup extends Window {

    private static final int TITLE_HEIGHT = 24;

    private static final String LABEL_NOTIFICATION = "Notification";

    private static final String LABEL_JOB_CLOSE = "Close";

    private static final int MAX_WIDTH = 400;

    private static final int MIN_HEIGHT = 100;

    private static final long DEFAULT_DELAY_CLOSE = 8 * 1000;

    private static final int PADDING_EDGE = 5;

    private long delayClose = DEFAULT_DELAY_CLOSE;

    protected LocalResourceManager resources;

    private final Display display;

    private Shell shell;

    private Region lastUsedRegion;

    private Image lastUsedBgImage;

    private final Job closeJob = new Job(LABEL_JOB_CLOSE) {

        @Override
        protected IStatus run(IProgressMonitor monitor) {
            if (!display.isDisposed()) {
                display.asyncExec(new Runnable() {
                    public void run() {
                        Shell shell = AbstractNotificationPopup.this.getShell();
                        if (shell == null || shell.isDisposed()) {
                            return;
                        }

                        if (isMouseOver(shell)) {
                            scheduleAutoClose();
                            return;
                        }

                        AbstractNotificationPopup.this.closeFade();
                    }

                });
            }
            if (monitor.isCanceled()) {
                return Status.CANCEL_STATUS;
            }

            return Status.OK_STATUS;
        }
    };

    private final boolean respectDisplayBounds = true;

    private final boolean respectMonitorBounds = true;

    private AnimationUtil.FadeJob fadeJob;

    private boolean fadingEnabled;

    @NotNull
    private final Color titleForegroundColor;
    @NotNull
    private final Color borderColor;
    @NotNull
    private final Color backgroundColor;

    public AbstractNotificationPopup(Display display) {
        this(display, SWT.NO_TRIM | SWT.ON_TOP | SWT.NO_FOCUS | SWT.TOOL);
    }

    public AbstractNotificationPopup(Display display, int style) {
        super(new Shell(display));
        setShellStyle(style);

        this.display = display;
        resources = new LocalResourceManager(JFaceResources.getResources());

        titleForegroundColor = getColor(resources, display.getSystemColor(SWT.COLOR_WIDGET_DARK_SHADOW).getRGB());
        //FIXME: on dark theme, the borders aren't distinguishable from the background for whatever reason
        borderColor = UIStyles.isDarkTheme() ? display.getSystemColor(SWT.COLOR_GRAY) : display.getSystemColor(SWT.COLOR_DARK_GRAY);
        backgroundColor = UIStyles.getDefaultWidgetBackground();

        closeJob.setSystem(true);
    }

    private static Color getColor(@NotNull ResourceManager manager, RGB rgb) {
        try {
            return manager.createColor(rgb);
        } catch (DeviceResourceException e) {
            return manager.getDevice().getSystemColor(SWT.COLOR_BLACK);
        }
    }

    public boolean isFadingEnabled() {
        return fadingEnabled;
    }

    public void setFadingEnabled(boolean fadingEnabled) {
        this.fadingEnabled = fadingEnabled;
    }

    /**
     * Override to return a customized name. Default is to return the name of the product, specified by the -name (e.g.
     * "Eclipse SDK") command line parameter that's associated with the product ID (e.g. "org.eclipse.sdk.ide"). Strips
     * the trailing "SDK" for any name, since this part of the label is considered visual noise.
     *
     * @return the name to be used in the title of the popup.
     */
    protected String getPopupShellTitle() {
        String productName = GeneralUtils.getProductName();
        if (productName != null) {
            return productName + " " + LABEL_NOTIFICATION; //$NON-NLS-1$
        } else {
            return LABEL_NOTIFICATION;
        }
    }

    protected Image getPopupShellImage(int maximumHeight) {
        return null;
    }

    /**
     * Override to populate with notifications.
     *
     * @param parent
     */
    protected void createContentArea(Composite parent) {
        // empty by default
    }

    /**
     * Override to customize the title bar
     */
    protected void createTitleArea(Composite parent) {
        ((GridData) parent.getLayoutData()).heightHint = TITLE_HEIGHT;

        Label titleImageLabel = new Label(parent, SWT.NONE);
        titleImageLabel.setImage(getPopupShellImage(TITLE_HEIGHT));

        Label titleTextLabel = new Label(parent, SWT.NONE);
        titleTextLabel.setText(getPopupShellTitle());
        titleTextLabel.setFont(JFaceResources.getFontRegistry().getBold("org.eclipse.jface.defaultfont"));
        titleTextLabel.setForeground(getTitleForeground());
        titleTextLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));
        titleTextLabel.setCursor(parent.getDisplay().getSystemCursor(SWT.CURSOR_HAND));

        final Label button = new Label(parent, SWT.NONE);
        button.setImage(DBeaverIcons.getImage(UIIcon.NOTIFICATION_CLOSE));
        button.addMouseTrackListener(new MouseTrackAdapter() {
            @Override
            public void mouseEnter(MouseEvent e) {
                button.setImage(DBeaverIcons.getImage(UIIcon.NOTIFICATION_CLOSE_HOVER));
            }

            @Override
            public void mouseExit(MouseEvent e) {
                button.setImage(DBeaverIcons.getImage(UIIcon.NOTIFICATION_CLOSE));
            }
        });
        button.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseUp(MouseEvent e) {
                close();
                setReturnCode(CANCEL);
            }

        });
    }

    protected Color getTitleForeground() {
        return titleForegroundColor;
    }

    @Override
    protected void configureShell(Shell newShell) {
        super.configureShell(newShell);

        shell = newShell;
        newShell.setBackground(borderColor);
    }

    @Override
    public void create() {
        super.create();
        addRegion(shell);
    }

    private void addRegion(Shell shell) {
        Region region = new Region();
        Point s = shell.getSize();

		/* Add entire Shell */
        region.add(0, 0, s.x, s.y);

		/* Subtract Top-Left Corner */
        region.subtract(0, 0, 5, 1);
        region.subtract(0, 1, 3, 1);
        region.subtract(0, 2, 2, 1);
        region.subtract(0, 3, 1, 1);
        region.subtract(0, 4, 1, 1);

		/* Subtract Top-Right Corner */
        region.subtract(s.x - 5, 0, 5, 1);
        region.subtract(s.x - 3, 1, 3, 1);
        region.subtract(s.x - 2, 2, 2, 1);
        region.subtract(s.x - 1, 3, 1, 1);
        region.subtract(s.x - 1, 4, 1, 1);

		/* Subtract Bottom-Left Corner */
        region.subtract(0, s.y, 5, 1);
        region.subtract(0, s.y - 1, 3, 1);
        region.subtract(0, s.y - 2, 2, 1);
        region.subtract(0, s.y - 3, 1, 1);
        region.subtract(0, s.y - 4, 1, 1);

		/* Subtract Bottom-Right Corner */
        region.subtract(s.x - 5, s.y - 0, 5, 1);
        region.subtract(s.x - 3, s.y - 1, 3, 1);
        region.subtract(s.x - 2, s.y - 2, 2, 1);
        region.subtract(s.x - 1, s.y - 3, 1, 1);
        region.subtract(s.x - 1, s.y - 4, 1, 1);

		/* Dispose old first */
        if (shell.getRegion() != null) {
            shell.getRegion().dispose();
        }

		/* Apply Region */
        shell.setRegion(region);

		/* Remember to dispose later */
        lastUsedRegion = region;
    }

    private boolean isMouseOver(Shell shell) {
        if (display.isDisposed()) {
            return false;
        }
        return shell.getBounds().contains(display.getCursorLocation());
    }

    @Override
    public int open() {
        if (shell == null || shell.isDisposed()) {
            shell = null;
            create();
        }

        constrainShellSize();
        shell.setLocation(fixupDisplayBounds(shell.getSize(), shell.getLocation()));

        if (isFadingEnabled()) {
            shell.setAlpha(0);
        }
        shell.setVisible(true);
        fadeJob = AnimationUtil.fadeIn(shell, (shell, alpha) -> {
            if (shell.isDisposed()) {
                return;
            }

            if (alpha == 255) {
                scheduleAutoClose();
            }
        });

        return Window.OK;
    }

    protected void scheduleAutoClose() {
        if (delayClose > 0) {
            closeJob.schedule(delayClose);
        }
    }

    @Override
    protected Control createContents(Composite parent) {
        ((GridLayout) parent.getLayout()).marginWidth = 1;
        ((GridLayout) parent.getLayout()).marginHeight = 1;

		/* Outer Composite holding the controls */
        final Composite outerCircle = new Composite(parent, SWT.NO_FOCUS);
        outerCircle.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        outerCircle.setBackgroundMode(SWT.INHERIT_FORCE);

        outerCircle.addControlListener(new ControlAdapter() {

            @Override
            public void controlResized(ControlEvent e) {
                Rectangle clArea = outerCircle.getClientArea();
                lastUsedBgImage = new Image(outerCircle.getDisplay(), clArea.width, clArea.height);
                GC gc = new GC(lastUsedBgImage);

                gc.setBackground(backgroundColor);
                gc.fillRectangle(clArea);

				/* Fix Region Shape */
                fixRegion(gc, clArea);

                gc.dispose();

                Image oldBGImage = outerCircle.getBackgroundImage();
                outerCircle.setBackgroundImage(lastUsedBgImage);

                if (oldBGImage != null) {
                    oldBGImage.dispose();
                }
            }

            private void fixRegion(GC gc, Rectangle clArea) {
                // FIXME:
                // Notification borders are currently black on dark theme (which is a bug).
                // If we proceed to fix the region, popup's corners are colored with border color
                // (and the rest of the border is not). This needs to be deleted when the borders are fixed.
                if (UIStyles.isDarkTheme()) {
                    return;
                }

                gc.setForeground(borderColor);

				/* Fill Top Left */
                gc.drawPoint(2, 0);
                gc.drawPoint(3, 0);
                gc.drawPoint(1, 1);
                gc.drawPoint(0, 2);
                gc.drawPoint(0, 3);

				/* Fill Top Right */
                gc.drawPoint(clArea.width - 4, 0);
                gc.drawPoint(clArea.width - 3, 0);
                gc.drawPoint(clArea.width - 2, 1);
                gc.drawPoint(clArea.width - 1, 2);
                gc.drawPoint(clArea.width - 1, 3);

				/* Fill Bottom Left */
                gc.drawPoint(2, clArea.height - 0);
                gc.drawPoint(3, clArea.height - 0);
                gc.drawPoint(1, clArea.height - 1);
                gc.drawPoint(0, clArea.height - 2);
                gc.drawPoint(0, clArea.height - 3);

				/* Fill Bottom Right */
                gc.drawPoint(clArea.width - 4, clArea.height - 0);
                gc.drawPoint(clArea.width - 3, clArea.height - 0);
                gc.drawPoint(clArea.width - 2, clArea.height - 1);
                gc.drawPoint(clArea.width - 1, clArea.height - 2);
                gc.drawPoint(clArea.width - 1, clArea.height - 3);
            }
        });

        GridLayout layout = new GridLayout(1, false);
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        layout.verticalSpacing = 0;

        outerCircle.setLayout(layout);

		/* Title area containing label and close button */
        final Composite titleCircle = new Composite(outerCircle, SWT.NO_FOCUS);
        titleCircle.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        titleCircle.setBackgroundMode(SWT.INHERIT_FORCE);

        layout = new GridLayout(4, false);
        layout.marginWidth = 3;
        layout.marginHeight = 0;
        layout.verticalSpacing = 5;
        layout.horizontalSpacing = 3;

        titleCircle.setLayout(layout);

		/* Create Title Area */
        createTitleArea(titleCircle);

		/* Outer composite to hold content controlls */
        Composite outerContentCircle = new Composite(outerCircle, SWT.NONE);
        outerContentCircle.setBackgroundMode(SWT.INHERIT_FORCE);

        layout = new GridLayout(1, false);
        layout.marginWidth = 0;
        layout.marginHeight = 0;

        outerContentCircle.setLayout(layout);
        outerContentCircle.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        outerContentCircle.setBackground(outerCircle.getBackground());

		/* Middle composite to show a 1px black line around the content controls */
        Composite middleContentCircle = new Composite(outerContentCircle, SWT.NO_FOCUS);
        middleContentCircle.setBackgroundMode(SWT.INHERIT_FORCE);

        layout = new GridLayout(1, false);
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        layout.marginTop = 1;

        middleContentCircle.setLayout(layout);
        middleContentCircle.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        middleContentCircle.setBackground(borderColor);

		/* Inner composite containing the content controls */
        Composite innerContent = new Composite(middleContentCircle, SWT.NO_FOCUS);
        innerContent.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        innerContent.setBackgroundMode(SWT.INHERIT_FORCE);

        layout = new GridLayout(1, false);
        layout.marginWidth = 0;
        layout.marginHeight = 5;
        layout.marginLeft = 5;
        layout.marginRight = 5;
        innerContent.setLayout(layout);

        innerContent.setBackground(shell.getDisplay().getSystemColor(SWT.COLOR_WHITE));

		/* Content Area */
        createContentArea(innerContent);

        setNullBackground(outerCircle);

        return outerCircle;
    }

    private void setNullBackground(final Composite outerCircle) {
        for (Control c : outerCircle.getChildren()) {
            c.setBackground(null);
            if (c instanceof Composite) {
                setNullBackground((Composite) c);
            }
        }
    }

    @Override
    protected void initializeBounds() {
        Rectangle clArea = getPrimaryClientArea();
        Point initialSize = shell.computeSize(SWT.DEFAULT, SWT.DEFAULT);
        int height = Math.max(initialSize.y, MIN_HEIGHT);
        int width = Math.min(initialSize.x, MAX_WIDTH);

        Point size = new Point(width, height);
        shell.setLocation(clArea.width + clArea.x - size.x - PADDING_EDGE, clArea.height + clArea.y - size.y
            - PADDING_EDGE);
        shell.setSize(size);
    }

    private Rectangle getPrimaryClientArea() {
        Monitor primaryMonitor = shell.getDisplay().getPrimaryMonitor();
        return (primaryMonitor != null) ? primaryMonitor.getClientArea() : shell.getDisplay().getClientArea();
    }

    public void closeFade() {
        if (fadeJob != null) {
            fadeJob.cancelAndWait(false);
        }
        fadeJob = AnimationUtil.fadeOut(getShell(), (shell, alpha) -> {
            if (!shell.isDisposed()) {
                if (alpha == 0) {
                    shell.close();
                } else if (isMouseOver(shell)) {
                    if (fadeJob != null) {
                        fadeJob.cancelAndWait(false);
                    }
                    fadeJob = AnimationUtil.fastFadeIn(shell, (shell1, alpha1) -> {
                        if (shell1.isDisposed()) {
                            return;
                        }

                        if (alpha1 == 255) {
                            scheduleAutoClose();
                        }
                    });
                }
            }
        });
    }

    @Override
    public boolean close() {
        resources.dispose();
        if (lastUsedRegion != null) {
            lastUsedRegion.dispose();
        }
        if (lastUsedBgImage != null && !lastUsedBgImage.isDisposed()) {
            lastUsedBgImage.dispose();
        }
        return super.close();
    }

    public long getDelayClose() {
        return delayClose;
    }

    public void setDelayClose(long delayClose) {
        this.delayClose = delayClose;
    }

    private Point fixupDisplayBounds(Point tipSize, Point location) {
        if (respectDisplayBounds) {
            Rectangle bounds;
            Point rightBounds = new Point(tipSize.x + location.x, tipSize.y + location.y);

            if (respectMonitorBounds) {
                bounds = shell.getDisplay().getPrimaryMonitor().getBounds();
            } else {
                bounds = getPrimaryClientArea();
            }

            if (!(bounds.contains(location) && bounds.contains(rightBounds))) {
                if (rightBounds.x > bounds.x + bounds.width) {
                    location.x -= rightBounds.x - (bounds.x + bounds.width);
                }

                if (rightBounds.y > bounds.y + bounds.height) {
                    location.y -= rightBounds.y - (bounds.y + bounds.height);
                }

                if (location.x < bounds.x) {
                    location.x = bounds.x;
                }

                if (location.y < bounds.y) {
                    location.y = bounds.y;
                }
            }
        }

        return location;
    }

}

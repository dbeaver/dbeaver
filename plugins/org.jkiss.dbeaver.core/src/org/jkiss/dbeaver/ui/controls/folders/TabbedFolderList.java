/*******************************************************************************
 * Copyright (c) 2001, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Mariot Chauvin <mariot.chauvin@obeo.fr> - bug 259553
 *     Amit Joglekar <joglekar@us.ibm.com> - Support for dynamic images (bug 385795)
 *
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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

package org.jkiss.dbeaver.ui.controls.folders;

import org.eclipse.jface.resource.ColorRegistry;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.source.ISharedTextColors;
import org.eclipse.swt.SWT;
import org.eclipse.swt.accessibility.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIUtils;

import java.util.IdentityHashMap;
import java.util.Map;


/**
 * Shows the list of tabs in the tabbed property sheet page.
 *
 * @author Anthony Hunter
 * @author Serge Rider
 */
public class TabbedFolderList extends Composite {

    private static final ListElement[] ELEMENTS_EMPTY = new ListElement[0];

    protected static final int NONE = -1;

    protected static final int INDENT_LEFT = 7;
    protected static final int INDENT_RIGHT = 10;
    public static final String LABEL_NA = "N/A";
    public static final int SECTION_DIV_HEIGHT = 7;
    private final boolean section;

    private boolean focus = false;

    private ListElement[] elements;

    private int selectedElementIndex = NONE;
    private int topVisibleIndex = NONE;
    private int bottomVisibleIndex = NONE;

    private TopNavigationElement topNavigationElement;
    private BottomNavigationElement bottomNavigationElement;

    private int widestLabelIndex = NONE;
    private int tabsThatFitInComposite = NONE;

    Color widgetForeground;
    Color widgetBackground;
    Color widgetNormalShadow;
    Color widgetDarkShadow;
    private Color listBackground;
    private Color hoverGradientStart;
    private Color hoverGradientEnd;
    private Color elementBackground;
    private Color indentedDefaultBackground;
    private Color indentedHoverBackground;
    private Color navigationElementShadowStroke;
    private Color bottomNavigationElementShadowStroke1;
    private Color bottomNavigationElementShadowStroke2;

    private final Map<Image, Image> grayedImages = new IdentityHashMap<>();

    /**
     * One of the tabs in the tabbed property list.
     */
    public class ListElement extends Canvas {

        private TabbedFolderInfo tab;
        private int index;
        private boolean selected;
        private boolean hover;

        /**
         * Constructor for ListElement.
         *
         * @param parent the parent Composite.
         * @param tab    the tab item for the element.
         * @param index  the index in the list.
         */
        public ListElement(Composite parent, final TabbedFolderInfo tab, int index) {
            super(parent, SWT.NO_FOCUS);
            this.tab = tab;
            hover = false;
            selected = false;
            this.index = index;

            addPaintListener(new PaintListener() {

                public void paintControl(PaintEvent e) {
                    paint(e);
                }
            });
            addMouseListener(new MouseAdapter() {

                public void mouseUp(MouseEvent e) {
                    if (!selected) {
                        select(getIndex(ListElement.this));
                        /*
						 * We set focus to the tabbed property composite so that
						 * focus is moved to the appropriate widget in the
						 * section.
						 */
                        Composite tabbedPropertyComposite = getParent();
                        tabbedPropertyComposite.setFocus();
                    }
                }
            });
            addMouseMoveListener(new MouseMoveListener() {

                public void mouseMove(MouseEvent e) {
                    String tooltip = tab.getTooltip();
                    if (tooltip != null) {
                        setToolTipText(tooltip);
                    }
                    if (!hover) {
                        hover = true;
                        redraw();
                    }
                }
            });
            addMouseTrackListener(new MouseTrackAdapter() {

                public void mouseExit(MouseEvent e) {
                    hover = false;
                    redraw();
                }
            });
        }

        /**
         * Set selected value for this element.
         *
         * @param selected the selected value.
         */
        public void setSelected(boolean selected) {
            this.selected = selected;
            redraw();
        }


        /**
         * Paint the element.
         *
         * @param e the paint event.
         */
        private void paint(PaintEvent e) {
			/*
			 * draw the top two lines of the tab, same for selected, hover and
			 * default
			 */
            Rectangle bounds = getBounds();
            e.gc.setForeground(widgetNormalShadow);
            e.gc.drawLine(0, 0, bounds.width - 1, 0);
            e.gc.setForeground(listBackground);
            e.gc.drawLine(0, 1, bounds.width - 1, 1);

			/* draw the fill in the tab */
            if (selected) {
                e.gc.setBackground(listBackground);
                e.gc.fillRectangle(0, 2, bounds.width, bounds.height - 1);
            } else if (hover && tab.isIndented()) {
                e.gc.setBackground(indentedHoverBackground);
                e.gc.fillRectangle(0, 2, bounds.width - 1, bounds.height - 1);
            } else if (hover) {
                e.gc.setForeground(hoverGradientStart);
                e.gc.setBackground(hoverGradientEnd);
                e.gc.fillGradientRectangle(0, 2, bounds.width - 1, bounds.height - 1, true);
            } else if (tab.isIndented()) {
                e.gc.setBackground(indentedDefaultBackground);
                e.gc.fillRectangle(0, 2, bounds.width - 1, bounds.height - 1);
            } else {
                e.gc.setBackground(elementBackground);
                e.gc.fillRectangle(0, 2, bounds.width - 1, bounds.height - 1);
                //e.gc.setBackground(defaultGradientEnd);
                //e.gc.fillGradientRectangle(0, 2, bounds.width - 1, bounds.height - 1, true);
            }

            if (!selected) {
                e.gc.setForeground(widgetNormalShadow);
                e.gc.drawLine(bounds.width - 1, 1, bounds.width - 1,
                    bounds.height + 1);
            }

			/*
			 * Add INDENT_LEFT pixels to the left as a margin.
			 */
            int textIndent = INDENT_LEFT;
            FontMetrics fm = e.gc.getFontMetrics();
            int height = fm.getHeight();
            int textMiddle = (bounds.height - height) / 2;

            if (tab.getImage() != null) {
				/* draw the icon for the selected tab */
                if (tab.isIndented()) {
                    textIndent = textIndent + INDENT_LEFT;
                } else {
                    textIndent = textIndent - 3;
                }
                Image image = DBeaverIcons.getImage(tab.getImage());
                if (selected || hover) {
                    e.gc.drawImage(image, textIndent, textMiddle - 1);
                } else {
                    e.gc.drawImage(getGrayedImage(image), textIndent, textMiddle - 1);
                }
                textIndent = textIndent + image.getBounds().width + 4;
            } else if (tab.isIndented()) {
                textIndent = textIndent + INDENT_LEFT;
            }

			/* draw the text */
            e.gc.setForeground(widgetForeground);
            if (selected) {
				/* selected tab is bold font */
                e.gc.setFont(JFaceResources.getFontRegistry().getBold(JFaceResources.DEFAULT_FONT));
            }
            e.gc.drawText(tab.getText(), textIndent, textMiddle, true);
            if (((TabbedFolderList) getParent()).focus && selected) {
				/* draw a line if the tab has focus */
                Point point = e.gc.textExtent(tab.getText());
                e.gc.drawLine(textIndent, bounds.height - 4, textIndent + point.x, bounds.height - 4);
            }

			/* draw the bottom line on the tab for selected and default */
            if (!hover) {
                e.gc.setForeground(listBackground);
                e.gc.drawLine(0, bounds.height - 1, bounds.width - 2, bounds.height - 1);
            }
        }

        /**
         * Get the tab item.
         *
         * @return the tab item.
         */
        public TabbedFolderInfo getInfo() {
            return tab;
        }

        public String toString() {
            return tab.getText();
        }
    }

    private Image getGrayedImage(Image image) {
        Image disabledImage = grayedImages.get(image);
        if (disabledImage == null) {
            disabledImage = new Image(image.getDevice(), image, SWT.IMAGE_GRAY);
            grayedImages.put(image, disabledImage);
        }

        return disabledImage;
    }

    /**
     * The top navigation element in the tabbed property list. It looks like a
     * scroll button when scrolling is needed or is just a spacer when no
     * scrolling is required.
     */
    public class TopNavigationElement extends Canvas {

        /**
         * Constructor for TopNavigationElement.
         *
         * @param parent the parent Composite.
         */
        public TopNavigationElement(Composite parent) {
            super(parent, SWT.NO_FOCUS);
            addPaintListener(new PaintListener() {

                public void paintControl(PaintEvent e) {
                    paint(e);
                }
            });
            addMouseListener(new MouseAdapter() {

                public void mouseUp(MouseEvent e) {
                    if (isUpScrollRequired()) {
                        bottomVisibleIndex--;
                        if (topVisibleIndex != 0) {
                            topVisibleIndex--;
                        }
                        layoutTabs();
                        topNavigationElement.redraw();
                        bottomNavigationElement.redraw();
                    }
                }
            });
        }

        /**
         * Paint the element.
         *
         * @param e the paint event.
         */
        private void paint(PaintEvent e) {
            e.gc.setBackground(widgetBackground);
            e.gc.setForeground(widgetForeground);
            Rectangle bounds = getBounds();

            if (elements.length != 0) {
                e.gc.fillRectangle(0, 0, bounds.width, bounds.height);
                e.gc.setForeground(widgetNormalShadow);
                e.gc.drawLine(bounds.width - 1, 0, bounds.width - 1,
                    bounds.height - 1);
            } else {
                e.gc.setBackground(listBackground);
                e.gc.fillRectangle(0, 0, bounds.width, bounds.height);
                int textIndent = INDENT_LEFT;
                FontMetrics fm = e.gc.getFontMetrics();
                int height = fm.getHeight();
                int textMiddle = (bounds.height - height) / 2;
                e.gc.setForeground(widgetForeground);
                e.gc.drawText(LABEL_NA, textIndent, textMiddle);
            }

            if (isUpScrollRequired()) {
                e.gc.setForeground(widgetDarkShadow);
                int middle = bounds.width / 2;
                e.gc.drawLine(middle + 1, 3, middle + 5, 7);
                e.gc.drawLine(middle, 3, middle - 4, 7);
                e.gc.drawLine(middle - 3, 7, middle + 4, 7);

                e.gc.setForeground(listBackground);
                e.gc.drawLine(middle, 4, middle + 1, 4);
                e.gc.drawLine(middle - 1, 5, middle + 2, 5);
                e.gc.drawLine(middle - 2, 6, middle + 3, 6);

                e.gc.setForeground(widgetNormalShadow);
                e.gc.drawLine(0, 0, bounds.width - 2, 0);
                e.gc.setForeground(navigationElementShadowStroke);
                e.gc.drawLine(0, 1, bounds.width - 2, 1);
                e.gc.drawLine(0, bounds.height - 1, bounds.width - 2,
                    bounds.height - 1);
            }
        }
    }

    /**
     * The top navigation element in the tabbed property list. It looks like a
     * scroll button when scrolling is needed or is just a spacer when no
     * scrolling is required.
     */
    public class BottomNavigationElement extends Canvas {

        /**
         * Constructor for BottomNavigationElement.
         *
         * @param parent the parent Composite.
         */
        public BottomNavigationElement(Composite parent) {
            super(parent, SWT.NO_FOCUS);
            addPaintListener(new PaintListener() {

                public void paintControl(PaintEvent e) {
                    paint(e);
                }
            });
            addMouseListener(new MouseAdapter() {

                public void mouseUp(MouseEvent e) {
                    if (isDownScrollRequired()) {
                        topVisibleIndex++;
                        if (bottomVisibleIndex != elements.length - 1) {
                            bottomVisibleIndex++;
                        }
                        layoutTabs();
                        topNavigationElement.redraw();
                        bottomNavigationElement.redraw();
                    }
                }
            });
        }

        /**
         * Paint the element.
         *
         * @param e the paint event.
         */
        private void paint(PaintEvent e) {
            e.gc.setBackground(widgetBackground);
            e.gc.setForeground(widgetForeground);
            Rectangle bounds = getBounds();

            if (elements.length != 0) {
                e.gc.fillRectangle(0, 0, bounds.width, bounds.height);
                e.gc.setForeground(widgetNormalShadow);
                if (!section || isDownScrollRequired()) {
                    e.gc.drawLine(bounds.width - 1, 0, bounds.width - 1, bounds.height - 1);
                } else {
                    e.gc.drawLine(bounds.width - 1, 0, bounds.width - 1, bounds.height - SECTION_DIV_HEIGHT);
                    e.gc.drawPoint(bounds.width - 1, bounds.height - 1);
                }
                e.gc.drawLine(0, 0, bounds.width - 1, 0);

                e.gc.setForeground(bottomNavigationElementShadowStroke1);
                e.gc.drawLine(0, 1, bounds.width - 2, 1);
                e.gc.setForeground(bottomNavigationElementShadowStroke2);
                e.gc.drawLine(0, 2, bounds.width - 2, 2);
            } else {
                e.gc.setBackground(listBackground);
                e.gc.fillRectangle(0, 0, bounds.width, bounds.height);
            }

            if (isDownScrollRequired()) {
                e.gc.setForeground(widgetDarkShadow);
                int middle = bounds.width / 2;
                int bottom = bounds.height - 3;
                e.gc.drawLine(middle + 1, bottom, middle + 5, bottom - 4);
                e.gc.drawLine(middle, bottom, middle - 4, bottom - 4);
                e.gc.drawLine(middle - 3, bottom - 4, middle + 4, bottom - 4);

                e.gc.setForeground(listBackground);
                e.gc.drawLine(middle, bottom - 1, middle + 1, bottom - 1);
                e.gc.drawLine(middle - 1, bottom - 2, middle + 2, bottom - 2);
                e.gc.drawLine(middle - 2, bottom - 3, middle + 3, bottom - 3);

                e.gc.setForeground(widgetNormalShadow);
                e.gc.drawLine(0, bottom - 7, bounds.width - 2, bottom - 7);
                e.gc.setForeground(navigationElementShadowStroke);
                e.gc.drawLine(0, bottom + 2, bounds.width - 2, bottom + 2);
                e.gc.drawLine(0, bottom - 6, bounds.width - 2, bottom - 6);
            }
        }
    }

    public TabbedFolderList(Composite parent, boolean section) {
        super(parent, SWT.NO_FOCUS);
        this.section = section;
        removeAll();
        setLayout(new FormLayout());
        initColours();
        initAccessible();
        topNavigationElement = new TopNavigationElement(this);
        bottomNavigationElement = new BottomNavigationElement(this);

        this.addFocusListener(new FocusListener() {

            public void focusGained(FocusEvent e) {
                focus = true;
                int i = getSelectionIndex();
                if (i >= 0) {
                    elements[i].redraw();
                }
            }

            public void focusLost(FocusEvent e) {
                focus = false;
                int i = getSelectionIndex();
                if (i >= 0) {
                    elements[i].redraw();
                }
            }
        });
        this.addControlListener(new ControlAdapter() {

            public void controlResized(ControlEvent e) {
                computeTopAndBottomTab();
            }
        });
        this.addTraverseListener(new TraverseListener() {
            public void keyTraversed(TraverseEvent e) {
                handleTraverse(e);
            }
        });
        addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(DisposeEvent e) {
                for (Image di : grayedImages.values()) {
                    UIUtils.dispose(di);
                }
                grayedImages.clear();
            }
        });
    }

    /**
     * Calculate the number of tabs that will fit in the tab list composite.
     */
    protected void computeTabsThatFitInComposite() {
        tabsThatFitInComposite = Math
            .round((getSize().y - 22) / getTabHeight());
        if (tabsThatFitInComposite <= 0) {
            tabsThatFitInComposite = 1;
        }
    }

    /**
     * Returns the number of elements in this list viewer.
     *
     * @return number of elements
     */
    public int getNumberOfElements() {
        return elements.length;
    }

    /**
     * Returns the element with the given index from this list viewer. Returns
     * <code>null</code> if the index is out of range.
     *
     * @param index the zero-based index
     * @return the element at the given index, or <code>null</code> if the
     * index is out of range
     */
    public ListElement getElementAt(int index) {
        if (index >= 0 && index < elements.length) {
            return elements[index];
        }
        return null;
    }

    public TabbedFolderInfo[] getElements() {
        TabbedFolderInfo[] tabs = new TabbedFolderInfo[elements.length];
        for (int i = 0; i < elements.length; i++) {
            tabs[i] = elements[i].getInfo();
        }
        return tabs;
    }


    /**
     * Returns the zero-relative index of the item which is currently selected
     * in the receiver, or -1 if no item is selected.
     *
     * @return the index of the selected item
     */
    public int getSelectionIndex() {
        return selectedElementIndex;
    }

    /**
     * Removes all elements from this list.
     */
    public void removeAll() {
        if (elements != null) {
            for (ListElement element : elements) {
                element.dispose();
            }
        }
        elements = ELEMENTS_EMPTY;
        selectedElementIndex = NONE;
        widestLabelIndex = NONE;
        topVisibleIndex = NONE;
        bottomVisibleIndex = NONE;
    }

    /**
     * Sets the new list elements.
     */
    public void setFolders(TabbedFolderInfo[] children) {
        if (elements != ELEMENTS_EMPTY) {
            removeAll();
        }
        elements = new ListElement[children.length];
        if (children.length == 0) {
            widestLabelIndex = NONE;
        } else {
            widestLabelIndex = 0;
            for (int i = 0; i < children.length; i++) {
                elements[i] = new ListElement(this, children[i], i);
                elements[i].setVisible(false);
                elements[i].setLayoutData(null);

                if (i != widestLabelIndex) {
                    int width = getTabWidth(children[i]);
                    if (width > getTabWidth(children[widestLabelIndex])) {
                        widestLabelIndex = i;
                    }
                }
            }
        }
        int maxTabWidth = getTabWidth(children[widestLabelIndex]);
        Object layoutData = getLayoutData();
        if (layoutData instanceof GridData) {
            ((GridData) layoutData).widthHint = maxTabWidth + INDENT_LEFT + INDENT_RIGHT;
        }
        computeTopAndBottomTab();
    }

    private int getTabWidth(TabbedFolderInfo folderInfo) {
        int width = getTextDimension(folderInfo.getText()).x;
		/*
		 * To anticipate for the icon placement we should always keep the
		 * space available after the label. So when the active tab includes
		 * an icon the width of the tab doesn't change.
		 */
        if (folderInfo.getImage() != null) {
            Image image = DBeaverIcons.getImage(folderInfo.getImage());
            width = width + image.getBounds().width + 4;
        }
        if (folderInfo.isIndented()) {
            width = width + INDENT_LEFT;
        }
        return width;
    }

    /**
     * Selects one of the elements in the list.
     *
     * @param index the index of the element to select.
     */
    public void select(int index) {
        if (index >= 0 && index < elements.length) {
            int lastSelected = getSelectionIndex();
            if (index == lastSelected) {
                return;
            }
            elements[index].setSelected(true);
            selectedElementIndex = index;
            if (lastSelected != NONE) {
                elements[lastSelected].setSelected(false);
                if (getSelectionIndex() != elements.length - 1) {
					/*
					 * redraw the next tab to fix the border by calling
					 * setSelected()
					 */
                    elements[getSelectionIndex() + 1].setSelected(false);
                }
            }
            topNavigationElement.redraw();
            bottomNavigationElement.redraw();

            if (selectedElementIndex < topVisibleIndex || selectedElementIndex > bottomVisibleIndex) {
                computeTopAndBottomTab();
            }
        }
        notifyListeners(SWT.Selection, new Event());
        elements[index].getInfo().getContents().setFocus();
    }

    /**
     * Deselects all the elements in the list.
     */
    public void deselectAll() {
        if (getSelectionIndex() != NONE) {
            elements[getSelectionIndex()].setSelected(false);
            selectedElementIndex = NONE;
        }
    }

    private int getIndex(ListElement element) {
        return element.index;
    }

    public Point computeSize(int wHint, int hHint, boolean changed) {
        Point result = super.computeSize(wHint, hHint, changed);
        Object layoutData = getLayoutData();
        if (layoutData instanceof GridData && ((GridData) layoutData).widthHint != -1) {
            result.x = ((GridData) layoutData).widthHint;
        } else if (widestLabelIndex == -1) {
            result.x = getTextDimension(LABEL_NA).x + INDENT_LEFT;
        } else {
			/*
			 * Add INDENT_LEFT pixels to the left of the longest tab as a margin.
			 */
            int width = getTabWidth(elements[widestLabelIndex].getInfo()) + INDENT_LEFT;
			/*
			 * Add INDENT_RIGHT pixels to the right of the longest tab as a margin.
			 */
            result.x = width + INDENT_RIGHT;
        }
        return result;
    }

    /**
     * Get the dimensions of the provided string.
     *
     * @param text the string.
     * @return the dimensions of the provided string.
     */
    private Point getTextDimension(String text) {
        GC gc = new GC(this);
        gc.setFont(JFaceResources.getFontRegistry().getBold(
            JFaceResources.DEFAULT_FONT));
        Point point = gc.textExtent(text);
        point.x++;
        gc.dispose();
        return point;
    }

    /**
     * Initialize the colours used in the list.
     */
    private void initColours() {
        Display display = Display.getCurrent();
        ISharedTextColors sharedColors = DBeaverUI.getSharedTextColors();

        ColorRegistry colorRegistry = UIUtils.getColorRegistry();

        listBackground = display.getSystemColor(SWT.COLOR_LIST_BACKGROUND);
        widgetBackground = getBackground();//display.getSystemColor(SWT.COLOR_WIDGET_BACKGROUND);
        widgetDarkShadow = display.getSystemColor(SWT.COLOR_WIDGET_DARK_SHADOW);
        widgetForeground = getForeground();//display.getSystemColor(SWT.COLOR_WIDGET_FOREGROUND);
        widgetNormalShadow = display.getSystemColor(SWT.COLOR_WIDGET_NORMAL_SHADOW);

        RGB white = display.getSystemColor(SWT.COLOR_WHITE).getRGB();
        RGB black = display.getSystemColor(SWT.COLOR_BLACK).getRGB();

		/*
		 * gradient in the default tab: start colour WIDGET_NORMAL_SHADOW 100% +
		 * white 20% + INFO_BACKGROUND 60% end colour WIDGET_NORMAL_SHADOW 100% +
		 * INFO_BACKGROUND 40%
		 */
        /*
        defaultGradientStart = sharedColors.getColor(
            UIUtils.blend(infoBackground,
                UIUtils.blend(white, widgetNormalShadow.getRGB(), 20), 60)
        );
        defaultGradientEnd = sharedColors.getColor(UIUtils.blend(infoBackground, widgetNormalShadow.getRGB(), 40));
        */
        if (widgetNormalShadow.hashCode() < widgetBackground.hashCode()) {
            // Foreground darker than background - make element background darker
            elementBackground = sharedColors.getColor(UIUtils.blend(black, widgetBackground.getRGB(), 15));
        } else {
            // Make element background lighter
            elementBackground = sharedColors.getColor(UIUtils.blend(white, widgetBackground.getRGB(), 15));
        }

        navigationElementShadowStroke = sharedColors.getColor(UIUtils.blend(white, widgetNormalShadow.getRGB(), 55));
        bottomNavigationElementShadowStroke1 = sharedColors.getColor(UIUtils.blend(black, widgetBackground.getRGB(), 10));
        bottomNavigationElementShadowStroke2 = sharedColors.getColor(UIUtils.blend(black, widgetBackground.getRGB(), 5));

		/*
		 * gradient in the hover tab: start colour WIDGET_BACKGROUND 100% +
		 * white 20% end colour WIDGET_BACKGROUND 100% + WIDGET_NORMAL_SHADOW
		 * 10%
		 */
        hoverGradientStart = sharedColors.getColor(UIUtils.blend(white, widgetBackground.getRGB(), 20));
        hoverGradientEnd = sharedColors.getColor(UIUtils.blend(widgetNormalShadow.getRGB(), widgetBackground.getRGB(), 10));

        indentedDefaultBackground = sharedColors.getColor(UIUtils.blend(white, widgetBackground.getRGB(), 10));
        indentedHoverBackground = sharedColors.getColor(UIUtils.blend(white, widgetBackground.getRGB(), 75));
    }

    @Override
    public void setBackground(Color color) {
        super.setBackground(color);
        DBeaverUI.asyncExec(new Runnable() {
            @Override
            public void run() {
                initColours();
                for (ListElement e : elements) {
                    e.redraw();
                }
                topNavigationElement.redraw();
                bottomNavigationElement.redraw();
            }
        });
    }

    /**
     * Get the height of a tab. The height of the tab is the height of the text
     * plus buffer.
     *
     * @return the height of a tab.
     */
    int getTabHeight() {
        int tabHeight = getTextDimension("").y + INDENT_LEFT; //$NON-NLS-1$
        if (tabsThatFitInComposite == 1) {
			/*
			 * if only one tab will fix, reduce the size of the tab height so
			 * that the navigation elements fit.
			 */
            int ret = getBounds().height - 20;
            return (ret > tabHeight) ? tabHeight
                : (ret < 5) ? 5
                : ret;
        }
        return tabHeight;
    }

    /**
     * Determine if a downward scrolling is required.
     *
     * @return true if downward scrolling is required.
     */
    private boolean isDownScrollRequired() {
        return elements.length > tabsThatFitInComposite
            && bottomVisibleIndex != elements.length - 1;
    }

    /**
     * Determine if an upward scrolling is required.
     *
     * @return true if upward scrolling is required.
     */
    private boolean isUpScrollRequired() {
        return elements.length > tabsThatFitInComposite && topVisibleIndex != 0;
    }

    /**
     * Based on available space, figure out the top and bottom tabs in the list.
     */
    private void computeTopAndBottomTab() {
        computeTabsThatFitInComposite();
        if (elements.length == 0) {
			/*
			 * no tabs to display.
			 */
            topVisibleIndex = 0;
            bottomVisibleIndex = 0;
        } else if (tabsThatFitInComposite >= elements.length) {
			/*
			 * all the tabs fit.
			 */
            topVisibleIndex = 0;
            bottomVisibleIndex = elements.length - 1;
        } else if (getSelectionIndex() == NONE) {
			/*
			 * there is no selected tab yet, assume that tab one would
			 * be selected for now.
			 */
            topVisibleIndex = 0;
            bottomVisibleIndex = tabsThatFitInComposite - 1;
        } else if (getSelectionIndex() + tabsThatFitInComposite > elements.length) {
			/*
			 * the selected tab is near the bottom.
			 */
            bottomVisibleIndex = elements.length - 1;
            topVisibleIndex = bottomVisibleIndex - tabsThatFitInComposite + 1;
        } else {
			/*
			 * the selected tab is near the top.
			 */
            topVisibleIndex = selectedElementIndex;
            bottomVisibleIndex = selectedElementIndex + tabsThatFitInComposite
                - 1;
        }
        layoutTabs();
    }

    /**
     * Layout the tabs.
     */
    private void layoutTabs() {
        if (tabsThatFitInComposite == NONE || elements.length == 0) {
            FormData formData = new FormData();
            formData.left = new FormAttachment(0, 0);
            formData.right = new FormAttachment(100, 0);
            formData.top = new FormAttachment(0, 0);
            formData.height = getTabHeight();
            topNavigationElement.setLayoutData(formData);

            formData = new FormData();
            formData.left = new FormAttachment(0, 0);
            formData.right = new FormAttachment(100, 0);
            formData.top = new FormAttachment(topNavigationElement, 0);
            formData.bottom = new FormAttachment(100, 0);
            bottomNavigationElement.setLayoutData(formData);
        } else {

            FormData formData = new FormData();
            formData.left = new FormAttachment(0, 0);
            formData.right = new FormAttachment(100, 0);
            formData.top = new FormAttachment(0, 0);
            formData.height = 10;
            topNavigationElement.setLayoutData(formData);

			/*
			 * use nextElement to attach the layout to the previous canvas
			 * widget in the list.
			 */
            Canvas nextElement = topNavigationElement;

            for (int i = 0; i < elements.length; i++) {
                if (i < topVisibleIndex || i > bottomVisibleIndex) {
					/*
					 * this tab is not visible
					 */
                    elements[i].setLayoutData(null);
                    elements[i].setVisible(false);
                } else {
					/*
					 * this tab is visible.
					 */
                    formData = new FormData();
                    formData.height = getTabHeight();
                    formData.left = new FormAttachment(0, 0);
                    formData.right = new FormAttachment(100, 0);
                    formData.top = new FormAttachment(nextElement, 0);
                    nextElement = elements[i];
                    elements[i].setLayoutData(formData);
                    elements[i].setVisible(true);
                }
            }
            formData = new FormData();
            formData.left = new FormAttachment(0, 0);
            formData.right = new FormAttachment(100, 0);
            formData.top = new FormAttachment(nextElement, 0);
            formData.bottom = new FormAttachment(100, 0);
            formData.height = 10;
            bottomNavigationElement.setLayoutData(formData);
        }

        // layout so that we have enough space for the new labels
        Composite grandparent = getParent().getParent();
        grandparent.layout(true);
        layout(true);
    }

    /**
     * Initialize the accessibility adapter.
     */
    private void initAccessible() {
        final Accessible accessible = getAccessible();
        accessible.addAccessibleListener(new AccessibleAdapter() {

            public void getName(AccessibleEvent e) {
                if (getSelectionIndex() != NONE) {
                    e.result = elements[getSelectionIndex()].getInfo().getText();
                }
            }

            public void getHelp(AccessibleEvent e) {
                if (getSelectionIndex() != NONE) {
                    e.result = elements[getSelectionIndex()].getInfo().getText();
                }
            }
        });

        accessible.addAccessibleControlListener(new AccessibleControlAdapter() {

            public void getChildAtPoint(AccessibleControlEvent e) {
                Point pt = toControl(new Point(e.x, e.y));
                e.childID = (getBounds().contains(pt)) ? ACC.CHILDID_SELF : ACC.CHILDID_NONE;
            }

            public void getLocation(AccessibleControlEvent e) {
                if (getSelectionIndex() != NONE) {
                    Rectangle location = elements[getSelectionIndex()].getBounds();
                    Point pt = toDisplay(new Point(location.x, location.y));
                    e.x = pt.x;
                    e.y = pt.y;
                    e.width = location.width;
                    e.height = location.height;
                }
            }

            public void getChildCount(AccessibleControlEvent e) {
                e.detail = 0;
            }

            public void getRole(AccessibleControlEvent e) {
                e.detail = ACC.ROLE_TABITEM;
            }

            public void getState(AccessibleControlEvent e) {
                e.detail = ACC.STATE_SELECTABLE | ACC.STATE_SELECTED | ACC.STATE_FOCUSED | ACC.STATE_FOCUSABLE;
            }
        });

        addListener(SWT.Selection, new Listener() {

            public void handleEvent(Event event) {
                if (isFocusControl()) {
                    accessible.setFocus(ACC.CHILDID_SELF);
                }
            }
        });

        addListener(SWT.FocusIn, new Listener() {

            public void handleEvent(Event event) {
                accessible.setFocus(ACC.CHILDID_SELF);
            }
        });
    }

    public void addSelectionListener(SelectionListener listener) {
        checkWidget ();
        TypedListener typedListener = new TypedListener (listener);
        addListener (SWT.Selection,typedListener);
        addListener (SWT.DefaultSelection,typedListener);
    }

    public void handleTraverse(TraverseEvent e) {
        if (e.detail == SWT.TRAVERSE_PAGE_PREVIOUS || e.detail == SWT.TRAVERSE_PAGE_NEXT) {
            int nMax = elements.length - 1;
            int nCurrent = getSelectionIndex();
            if (e.detail == SWT.TRAVERSE_PAGE_PREVIOUS) {
                nCurrent -= 1;
                nCurrent = Math.max(0, nCurrent);
            } else {
                nCurrent += 1;
                nCurrent = Math.min(nCurrent, nMax);
            }
            select(nCurrent);
            redraw();
            e.doit = false;
        } else {
            e.doit = true;
        }
    }

}

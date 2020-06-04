/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.controls.finder;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.IToolTipProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.Canvas;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ui.css.CSSUtils;
import org.jkiss.utils.CommonUtils;

/**
 * AdvancedListItem
 */
public class AdvancedListItem extends Canvas {

    private static final Log log = Log.getLog(AdvancedListItem.class);
    public static final int BORDER_MARGIN = 5;

    private final AdvancedList list;
    private final ILabelProvider labelProvider;
    private boolean isHover;
    final TextLayout textLayout;

    public AdvancedListItem(AdvancedList list, Object item, ILabelProvider labelProvider) {
        super(list.getContainer(), SWT.NONE);
        this.labelProvider = labelProvider;
        this.list = list;
        this.list.addItem(this);
        this.setData(item);
        this.textLayout = new TextLayout(list.getDisplay());
        this.textLayout.setText(labelProvider.getText(item));

        if (labelProvider instanceof IToolTipProvider) {
            String toolTipText = ((IToolTipProvider) labelProvider).getToolTipText(item);
            if (!CommonUtils.isEmpty(toolTipText)) {
                setToolTipText(toolTipText);
            }
        }

        CSSUtils.setCSSClass(this, "Composite");
        this.setBackground(list.getContainer().getBackground());

        this.addMouseTrackListener(new MouseTrackAdapter() {
            @Override
            public void mouseEnter(MouseEvent e) {
                isHover = true;
                redraw();
            }

            @Override
            public void mouseExit(MouseEvent e) {
                isHover = false;
                redraw();
            }

            @Override
            public void mouseHover(MouseEvent e) {
                //redraw();
            }
        });
        this.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseDoubleClick(MouseEvent e) {
                getList().notifyDefaultSelection();
            }

            @Override
            public void mouseDown(MouseEvent e) {
                getList().setSelection(AdvancedListItem.this);
                setFocus();
            }
        });
        this.addPaintListener(this::painItem);
        this.addDisposeListener(e -> list.removeItem(this));

        this.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (getList().getSelectedItem() == null) {
                    getList().setSelection(AdvancedListItem.this);
                }
            }
        });
        this.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                switch (e.keyCode) {
                    case SWT.ARROW_LEFT:
                    case SWT.ARROW_RIGHT:
                    case SWT.ARROW_UP:
                    case SWT.ARROW_DOWN:
                    case SWT.CR:
                        if (getList().getSelectedItem() != null) {
                            getList().navigateByKey(e);
                        }
                        break;
                }
            }
        });

    }

    private void painItem(PaintEvent e) {
        Point itemSize = getSize();
        if (itemSize.x <= 0 || itemSize.y <= 0) {
            return;
        }
        boolean isSelected = getList().getSelectedItem() == this;

        GC gc = e.gc;
        if (isSelected) {
            gc.setBackground(getList().getSelectionBackgroundColor());
            gc.setForeground(getList().getSelectionForegroundColor());
        } else if (isHover) {
            gc.setBackground(getList().getHoverBackgroundColor());
            gc.setForeground(getList().getForegroundColor());
        } else {
            gc.setBackground(getList().getContainer().getBackground());
            gc.setForeground(getList().getForegroundColor());
        }

        if (isSelected || isHover) {
            gc.fillRoundRectangle(0, 0, itemSize.x, itemSize.y, 5, 5);
        }

        Image icon = labelProvider.getImage(getData());
        Rectangle iconBounds = icon.getBounds();
        Point imageSize = getList().getImageSize();

        int imgPosX = (itemSize.x - imageSize.x) / 2;
        int imgPosY = BORDER_MARGIN;//(itemBounds.height - iconBounds.height) / 2 ;

        gc.setAntialias(SWT.ON);
        gc.setInterpolation(SWT.HIGH);
        gc.drawImage(icon, 0, 0, iconBounds.width, iconBounds.height,
            imgPosX - e.x, imgPosY, imageSize.x, imageSize.y);

        this.textLayout.setWidth(itemSize.x - BORDER_MARGIN * 2);
        this.textLayout.setAlignment(SWT.CENTER);
        this.textLayout.draw(gc, BORDER_MARGIN, imageSize.y + BORDER_MARGIN);
        /*String text = labelProvider.getText(getData());
        String theText = text;
        int divPos = theText.indexOf('(');
        if (divPos != -1 && text.endsWith(")")) {
            // Draw in two lines
            String mainTitle = text.substring(0, divPos);
            String subTitle = text.substring(divPos, text.length());
            drawItemText(e, itemSize, gc, mainTitle, 0);
            drawItemText(e, itemSize, gc, subTitle, getList().getTextSize().y + 1);
        } else {
            drawItemText(e, itemSize, gc, text, BORDER_MARGIN * 2);
        }*/
    }

    private void drawItemText(PaintEvent e, Point itemSize, GC gc, String theText, int topIndent) {
        Point textSize = gc.stringExtent(theText);
        if (textSize.x > itemSize.x - BORDER_MARGIN * 2) textSize.x = itemSize.x - BORDER_MARGIN * 2;

        gc.setClipping(BORDER_MARGIN, itemSize.y - BORDER_MARGIN * 4 - textSize.y + topIndent, itemSize.x - BORDER_MARGIN * 2, textSize.y * 2);

        gc.drawText(theText, (itemSize.x - textSize.x) / 2 - e.x, itemSize.y - textSize.y - BORDER_MARGIN * 4 + topIndent);
    }

    private AdvancedList getList() {
        return list;
    }

    public boolean isHover() {
        return isHover;
    }

    @Override
    public Point computeSize(int wHint, int hHint, boolean changed) {
        Point imageSize = getList().getImageSize();
        int itemLength = imageSize.x + BORDER_MARGIN * 4 + getList().getTextSize().y;
        return new Point(itemLength, itemLength + BORDER_MARGIN * 2);
        //return super.computeSize(wHint, hHint, changed);//getList().getImageSize();
    }

    private Image resize(Image image, int width, int height) {
        Image scaled = new Image(getDisplay().getDefault(), width, height);
        GC gc = new GC(scaled);
        gc.setAntialias(SWT.ON);
        gc.setInterpolation(SWT.HIGH);
        gc.drawImage(image, 0,  0,
            image.getBounds().width, image.getBounds().height,
            0, 0, width, height);
        gc.dispose();
        return scaled;
    }

}
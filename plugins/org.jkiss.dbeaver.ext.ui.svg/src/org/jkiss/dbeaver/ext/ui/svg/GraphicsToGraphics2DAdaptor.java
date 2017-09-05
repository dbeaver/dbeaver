/******************************************************************************
 * Copyright (c) 2004, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    IBM Corporation - initial API and implementation 
 *    SAP AG - copied class from GMF runtime since Graphiti can not have a dependency to GMF
 *    		   uses GraphitiUiInternal.getWorkbenchService() instead of 
 *    		   GMF's DisplayUtils, does not implement the interface
 *    		   DrawableRenderedImage, since it is not needed.
 *    RaM - Remove/hide initial viewport/clipping area, so all GEF diagrams fit
 *    			
 ****************************************************************************/

package org.jkiss.dbeaver.ext.ui.svg;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Composite;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.Stack;

import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.SWTGraphics;
import org.eclipse.draw2d.TextUtilities;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.PointList;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.LineAttributes;
import org.eclipse.swt.graphics.Path;
import org.eclipse.swt.graphics.PathData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.TextLayout;
import org.eclipse.swt.widgets.Display;

/**
 * Objects of this class can be used with draw2d to render to a Graphics2D object.
 * 
 * @author jschofie / sshaw
 * 
 *         This class is originally from GMF and was adapted for Graphiti needs.
 */
public class GraphicsToGraphics2DAdaptor extends Graphics
{
    private static class State
    {
        /**
         * translateX
         */
        public int translateX = 0;

        /**
         * translateY
         */
        public int translateY = 0;

        /** Indicates whether clipping is active or not */
        boolean clippingActive = false;

        /**
         * clipping rectangle x coordinate
         */
        public int clipX = 0;
        /**
         * clipping rectangle y coordinate
         */
        public int clipY = 0;
        /**
         * clipping rectangle width
         */
        public int clipW = 0;
        /**
         * clipping rectangle height
         */
        public int clipH = 0;

        /** Font value **/
        /**
         * cached font
         */
        public Font font;

        /**
         * cached xor mode value
         */
        public boolean XorMode = false;
        /**
         * cached foreground color
         */
        public Color fgColor;
        /**
         * cached background color
         */
        public Color bgColor;

        /**
         * cached alpha value
         */
        public int alpha;

        /**
         * Line attributes value
         */
        public LineAttributes lineAttributes = new LineAttributes(1);

        int graphicHints;

        /**
         * Copy the values from a given state to this state
         * 
         * @param state the state to copy from
         */
        public void copyFrom(State state)
        {
            translateX = state.translateX;
            translateY = state.translateY;

            clippingActive = state.clippingActive;

            clipX = state.clipX;
            clipY = state.clipY;
            clipW = state.clipW;
            clipH = state.clipH;

            font = state.font;
            fgColor = state.fgColor;
            bgColor = state.bgColor;
            XorMode = state.XorMode;
            alpha = state.alpha;
            graphicHints = state.graphicHints;

            lineAttributes = SWTGraphics.clone(state.lineAttributes);
        }
    }

    static final int ADVANCED_GRAPHICS_MASK;
    static final int ADVANCED_SHIFT;
    static final int FILL_RULE_MASK;
    static final int FILL_RULE_SHIFT;
    static final int FILL_RULE_WHOLE_NUMBER = -1;

    /*
     * It's consistent with SWTGraphics flags in case some other flags from SWTGraphics need to be here
     */
    static
    {
        FILL_RULE_SHIFT = 14;
        ADVANCED_SHIFT = 15;
        FILL_RULE_MASK = 1 << FILL_RULE_SHIFT; // If changed to more than 1-bit,
                                               // check references!
        ADVANCED_GRAPHICS_MASK = 1 << ADVANCED_SHIFT;
    }

    private SWTGraphics swtGraphics;
    private Graphics2D graphics2D;
    private BasicStroke stroke;
    private Stack<State> states = new Stack<State>();
    private final State currentState = new State();
    private final State appliedState = new State();

    /**
     * Some strings, Asian string in particular, are painted differently between SWT and AWT. SWT falls back to some
     * default locale font if Asian string cannot be painted with the current font - this is done via the platform. AWT,
     * unlike platform biased SWT, does not. Hence, Asian string widths are very different between SWT and AWT. To
     * workaround the issue, if the flag below is set to <code>true</code> then once SWT and AWT string width are not
     * equal, a bitmap of the SWT string will be painted. Otherwise the string is always painted with AWT Graphics 2D
     * string rendering.
     */
    protected boolean paintNotCompatibleStringsAsBitmaps = true;

    private static final TextUtilities TEXT_UTILITIES = new TextUtilities();

    /** Indicates that there is no active clipping area */
    private static final Rectangle NOCLIPPING = new Rectangle();

    private Rectangle relativeClipRegion = NOCLIPPING;

    private Image image;

    /**
     * x coordinate for graphics translation
     */
    private int transX = 0;
    /**
     * y coordinate for graphics translation
     */
    private int transY = 0;

    /**
     * Alternate Constructor that takes an swt Rectangle
     * 
     * @param graphics the <code>Graphics2D</code> object that this object is delegating calls to.
     */
    public GraphicsToGraphics2DAdaptor(Graphics2D graphics)
    {
        // Create the SWT Graphics Object
        createSWTGraphics();

        // Initialize the SVG Graphics Object
        initSVGGraphics(graphics);

        // Initialize the States
        init();
    }

    /**
     * This is a helper method used to create the SWT Graphics object
     */
    private void createSWTGraphics()
    {
        // we need this temp Rect just to instantiate an swt image in order to
        // keep state, the size of this Rect is of no consequence and we just set it
        // to such a small size in order to minimize memory allocation
        org.eclipse.swt.graphics.Rectangle tempRect = new org.eclipse.swt.graphics.Rectangle(0, 0, 10, 10);
        image = new Image(Display.getCurrent(), tempRect);
        GC gc = new GC(image);
        swtGraphics = new SWTGraphics(gc);
    }

    /**
     * Create the SVG graphics object and initializes it with the current line style and width
     */
    private void initSVGGraphics(Graphics2D graphics)
    {
        this.graphics2D = graphics;

        relativeClipRegion = NOCLIPPING;

        // Initialize the line style and width
        stroke = new BasicStroke(swtGraphics.getLineWidth(), BasicStroke.CAP_SQUARE, BasicStroke.JOIN_ROUND, 0, null, 0);
        LineAttributes lineAttributes = new LineAttributes(1);
        swtGraphics.getLineAttributes(lineAttributes);
        setLineAttributes(lineAttributes);
        setFillRule(swtGraphics.getFillRule());
        setAdvanced(swtGraphics.getAdvanced());
        getGraphics2D().setStroke(stroke);
    }

    /**
     * This method should only be called by the constructor. Initializes state information for the currentState
     */
    private void init()
    {
        // Initialize drawing styles
        setForegroundColor(getForegroundColor());
        setBackgroundColor(getBackgroundColor());
        setXORMode(getXORMode());

        // Initialize Font
        setFont(getFont());
        currentState.font = appliedState.font = getFont();

        // Initialize translations
        currentState.translateX = appliedState.translateX = transX;
        currentState.translateY = appliedState.translateY = transY;

        // Initialize Clip Regions
        if (relativeClipRegion.equals(NOCLIPPING))
        {
            currentState.clippingActive = appliedState.clippingActive = false;
        }
        else
        {
            currentState.clipX = appliedState.clipX = relativeClipRegion.x;
            currentState.clipY = appliedState.clipY = relativeClipRegion.y;
            currentState.clipW = appliedState.clipW = relativeClipRegion.width;
            currentState.clipH = appliedState.clipH = relativeClipRegion.height;
        }

        currentState.alpha = appliedState.alpha = getAlpha();
    }

    /**
     * Verifies that the applied state is up to date with the current state and updates the applied state accordingly.
     */
    protected void checkState()
    {
        if (appliedState.font != currentState.font)
        {
            appliedState.font = currentState.font;

            setFont(currentState.font);
        }

        if (appliedState.clipX != currentState.clipX || appliedState.clipY != currentState.clipY
                || appliedState.clipW != currentState.clipW || appliedState.clipH != currentState.clipH)
        {

            appliedState.clipX = currentState.clipX;
            appliedState.clipY = currentState.clipY;
            appliedState.clipW = currentState.clipW;
            appliedState.clipH = currentState.clipH;

            // Adjust the clip for SVG
            getGraphics2D().setClip(currentState.clipX - 1, currentState.clipY - 1, currentState.clipW + 2,
                    currentState.clipH + 2);
        }

        if (appliedState.alpha != currentState.alpha)
        {
            appliedState.alpha = currentState.alpha;

            setAlpha(currentState.alpha);
        }

        appliedState.graphicHints = currentState.graphicHints;
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.draw2d.Graphics#clipRect(org.eclipse.draw2d.geometry.Rectangle )
     */
    @Override
    public void clipRect(Rectangle rect)
    {
        if (relativeClipRegion.equals(NOCLIPPING))
        {
            setClipAbsolute(true, rect.x + transX, rect.y + transY, rect.width, rect.height);
        }
        else
        {
            relativeClipRegion.intersect(rect);
            setClipAbsolute(true, relativeClipRegion.x + transX, relativeClipRegion.y + transY,
                    relativeClipRegion.width, relativeClipRegion.height);
        }
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.draw2d.Graphics#dispose()
     */
    @Override
    public void dispose()
    {
        swtGraphics.dispose();

        if (image != null)
        {
            image.dispose();
        }

        states.clear();
    }

    /**
     * This method is used to convert an SWT Color to an AWT Color.
     * 
     * @param toConvert SWT Color to convert
     * @return AWT Color
     */
    protected java.awt.Color getColor(Color toConvert)
    {
        return new java.awt.Color(toConvert.getRed(), toConvert.getGreen(), toConvert.getBlue());
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.draw2d.Graphics#drawArc(int, int, int, int, int, int)
     */
    @Override
    public void drawArc(int x, int y, int width, int height, int startAngle, int endAngle)
    {
        Arc2D arc = new Arc2D.Float(x + transX, y + transY, width - 1, height, startAngle, endAngle, Arc2D.OPEN);

        checkState();
        getGraphics2D().setPaint(getColor(swtGraphics.getForegroundColor()));
        getGraphics2D().setStroke(createStroke());
        getGraphics2D().draw(arc);
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.draw2d.Graphics#fillArc(int, int, int, int, int, int)
     */
    @Override
    public void fillArc(int x, int y, int w, int h, int offset, int length)
    {
        Arc2D arc = new Arc2D.Float(x + transX, y + transY, w, h, offset, length, Arc2D.OPEN);

        checkState();
        getGraphics2D().setPaint(getColor(swtGraphics.getBackgroundColor()));
        getGraphics2D().fill(arc);
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.draw2d.Graphics#drawFocus(int, int, int, int)
     */
    @Override
    public void drawFocus(int x, int y, int w, int h)
    {
        drawRectangle(x, y, w, h);
    }

    @Override
    public void drawTextLayout(TextLayout layout, int x, int y, int selectionStart, int selectionEnd,
            Color selectionForeground, Color selectionBackground)
    {
        checkState();
        if (!layout.getBounds().isEmpty())
        {
            Image image = new Image(Display.getCurrent(), layout.getBounds().width, layout.getBounds().height);
            GC gc = new GC(image);
            cloneGC(gc);
            layout.draw(gc, 0, 0, selectionStart, selectionEnd, selectionForeground, selectionBackground);

            ImageData imageData = image.getImageData();
            imageData.transparentPixel = imageData.palette.getPixel(getBackgroundColor().getRGB());

            gc.dispose();
            image.dispose();

            getGraphics2D().drawImage(ImageConverter.convertFromImageData(imageData), x + transX, y + transY, null);
        }
    }

    private void cloneGC(GC gc)
    {
        gc.setAdvanced(getAdvanced());
        gc.setAlpha(getAlpha());
        gc.setAntialias(getAntialias());
        gc.setFillRule(getFillRule());
        gc.setFont(getFont());
        gc.setInterpolation(getInterpolation());
        gc.setLineAttributes(getLineAttributes());
        gc.setTextAntialias(getTextAntialias());
        gc.setBackground(getBackgroundColor());
        gc.setForeground(getForegroundColor());
    }

    @Override
    public int getInterpolation()
    {
        return swtGraphics.getInterpolation();
    }

    @Override
    public LineAttributes getLineAttributes()
    {
        LineAttributes la = new LineAttributes(1);
        swtGraphics.getLineAttributes(la);
        return la;
    }

    @Override
    public int getTextAntialias()
    {
        return swtGraphics.getTextAntialias();
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.draw2d.Graphics#drawImage(org.eclipse.swt.graphics.Image, int, int)
     */
    @Override
    public void drawImage(Image srcImage, int xpos, int ypos)
    {
        // Translate the Coordinates
        xpos += transX;
        ypos += transY;

        // Convert the SWT Image into an AWT BufferedImage
        BufferedImage toDraw = ImageConverter.convert(srcImage);

        checkState();
        getGraphics2D().drawImage(toDraw, new AffineTransform(1f, 0f, 0f, 1f, xpos, ypos), null);
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.draw2d.Graphics#drawImage(org.eclipse.swt.graphics.Image, int, int, int, int, int, int, int,
     * int)
     */
    @Override
    public void drawImage(Image srcImage, int x1, int y1, int w1, int h1, int x2, int y2, int w2, int h2)
    {
        x2 += transX;
        y2 += transY;

        BufferedImage toDraw = ImageConverter.convert(srcImage);
        checkState();
        getGraphics2D().drawImage(toDraw, x2, y2, w2, h2, null);
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.draw2d.Graphics#drawLine(int, int, int, int)
     */
    @Override
    public void drawLine(int x1, int y1, int x2, int y2)
    {
        Line2D line = new Line2D.Float(x1 + transX, y1 + transY, x2 + transX, y2 + transY);

        checkState();
        getGraphics2D().setPaint(getColor(swtGraphics.getForegroundColor()));
        getGraphics2D().setStroke(createStroke());
        getGraphics2D().draw(line);
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.draw2d.Graphics#drawOval(int, int, int, int)
     */
    @Override
    public void drawOval(int x, int y, int w, int h)
    {
        Ellipse2D ellipse = new Ellipse2D.Float(x + transX, y + transY, w, h);

        checkState();
        getGraphics2D().setPaint(getColor(swtGraphics.getForegroundColor()));
        getGraphics2D().setStroke(createStroke());
        getGraphics2D().draw(ellipse);
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.draw2d.Graphics#fillOval(int, int, int, int)
     */
    @Override
    public void fillOval(int x, int y, int w, int h)
    {
        Ellipse2D ellipse = new Ellipse2D.Float(x + transX, y + transY, w - 1, h - 1);

        checkState();
        getGraphics2D().setPaint(getColor(swtGraphics.getBackgroundColor()));
        getGraphics2D().fill(ellipse);
    }

    private Polygon createPolygon(PointList pointList)
    {
        Polygon toCreate = new Polygon();

        for (int i = 0; i < pointList.size(); i++)
        {
            Point pt = pointList.getPoint(i);

            toCreate.addPoint(pt.x + transX, pt.y + transY);
        }

        return toCreate;
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.draw2d.Graphics#drawPolygon(org.eclipse.draw2d.geometry.PointList )
     */
    @Override
    public void drawPolygon(PointList pointList)
    {
        checkState();
        getGraphics2D().setPaint(getColor(swtGraphics.getForegroundColor()));
        getGraphics2D().setStroke(createStroke());
        getGraphics2D().draw(createPolygon(pointList));
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.draw2d.Graphics#fillPolygon(org.eclipse.draw2d.geometry.PointList )
     */
    @Override
    public void fillPolygon(PointList pointList)
    {
        checkState();
        getGraphics2D().setPaint(getColor(swtGraphics.getBackgroundColor()));
        getGraphics2D().fill(createPolygon(pointList));
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.draw2d.Graphics#drawPolyline(org.eclipse.draw2d.geometry. PointList)
     */
    @Override
    public void drawPolyline(PointList pointList)
    {
        // Draw polylines as a series of lines
        for (int x = 1; x < pointList.size(); x++)
        {

            Point p1 = pointList.getPoint(x - 1);
            Point p2 = pointList.getPoint(x);

            drawLine(p1.x, p1.y, p2.x, p2.y);
        }
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.draw2d.Graphics#drawRectangle(int, int, int, int)
     */
    @Override
    public void drawRectangle(int x, int y, int w, int h)
    {
        Rectangle2D rect = new Rectangle2D.Float(x + transX, y + transY, w, h);

        checkState();
        getGraphics2D().setPaint(getColor(swtGraphics.getForegroundColor()));
        getGraphics2D().setStroke(createStroke());
        getGraphics2D().draw(rect);
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.draw2d.Graphics#fillRectangle(int, int, int, int)
     */
    @Override
    public void fillRectangle(int x, int y, int width, int height)
    {
        Rectangle2D rect = new Rectangle2D.Float(x + transX, y + transY, width, height);

        checkState();
        getGraphics2D().setPaint(getColor(swtGraphics.getBackgroundColor()));
        getGraphics2D().fill(rect);
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.draw2d.Graphics#drawRoundRectangle(org.eclipse.draw2d.geometry .Rectangle, int, int)
     */
    @Override
    public void drawRoundRectangle(Rectangle rect, int arcWidth, int arcHeight)
    {
        RoundRectangle2D roundRect = new RoundRectangle2D.Float(rect.x + transX, rect.y + transY, rect.width,
                rect.height, arcWidth, arcHeight);

        checkState();
        getGraphics2D().setPaint(getColor(swtGraphics.getForegroundColor()));
        getGraphics2D().setStroke(createStroke());
        getGraphics2D().draw(roundRect);
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.draw2d.Graphics#fillRoundRectangle(org.eclipse.draw2d.geometry .Rectangle, int, int)
     */
    @Override
    public void fillRoundRectangle(Rectangle rect, int arcWidth, int arcHeight)
    {
        RoundRectangle2D roundRect = new RoundRectangle2D.Float(rect.x + transX, rect.y + transY, rect.width,
                rect.height, arcWidth, arcHeight);

        checkState();
        getGraphics2D().setPaint(getColor(swtGraphics.getBackgroundColor()));
        getGraphics2D().fill(roundRect);
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.draw2d.Graphics#drawText(java.lang.String, int, int)
     */
    @Override
    public void drawText(String s, int x, int y)
    {
        drawString(s, x, y);
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.draw2d.Graphics#drawString(java.lang.String, int, int)
     */
    @Override
    public void drawString(String s, int x, int y)
    {

        if (s == null)
            return;

        java.awt.FontMetrics metrics = getGraphics2D().getFontMetrics();
        int stringLength = metrics.stringWidth(s);
        Dimension swtStringSize = TEXT_UTILITIES.getStringExtents(s, swtGraphics.getFont());

        float xpos = x + transX;
        float ypos = y + transY;
        int lineWidth;

        if (paintNotCompatibleStringsAsBitmaps && Math.abs(swtStringSize.width - stringLength) > 2)
        {
            // create SWT bitmap of the string then
            Image image = new Image(Display.getCurrent(), swtStringSize.width, swtStringSize.height);
            GC gc = new GC(image);
            gc.setForeground(getForegroundColor());
            gc.setBackground(getBackgroundColor());
            gc.setAntialias(getAntialias());
            gc.setFont(getFont());
            gc.drawString(s, 0, 0);
            gc.dispose();
            ImageData data = image.getImageData();
            image.dispose();
            RGB backgroundRGB = getBackgroundColor().getRGB();
            for (int i = 0; i < data.width; i++)
            {
                for (int j = 0; j < data.height; j++)
                {
                    if (data.palette.getRGB(data.getPixel(i, j)).equals(backgroundRGB))
                    {
                        data.setAlpha(i, j, 0);
                    }
                    else
                    {
                        data.setAlpha(i, j, 255);
                    }
                }
            }
            getGraphics2D().drawImage(ImageConverter.convertFromImageData(data),
                    new AffineTransform(1f, 0f, 0f, 1f, xpos, ypos), null);
            stringLength = swtStringSize.width;
        }
        else
        {

            ypos += metrics.getAscent();

            checkState();
            getGraphics2D().setPaint(getColor(swtGraphics.getForegroundColor()));
            getGraphics2D().drawString(s, xpos, ypos);
        }

        if (isFontUnderlined(getFont()))
        {
            int baseline = y + metrics.getAscent();
            lineWidth = getLineWidth();

            setLineWidth(1);
            drawLine(x, baseline, x + stringLength, baseline);
            setLineWidth(lineWidth);
        }

        if (isFontStrikeout(getFont()))
        {
            int strikeline = y + (metrics.getHeight() / 2);
            lineWidth = getLineWidth();

            setLineWidth(1);
            drawLine(x, strikeline, x + stringLength, strikeline);
            setLineWidth(lineWidth);
        }
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.draw2d.Graphics#fillString(java.lang.String, int, int)
     */
    @Override
    public void fillString(String s, int x, int y)
    {
        // Not implemented
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.draw2d.Graphics#fillText(java.lang.String, int, int)
     */
    @Override
    public void fillText(String s, int x, int y)
    {
        // Not implemented
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.draw2d.Graphics#getBackgroundColor()
     */
    @Override
    public Color getBackgroundColor()
    {
        return swtGraphics.getBackgroundColor();
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.draw2d.Graphics#getClip(org.eclipse.draw2d.geometry.Rectangle )
     */
    @Override
    public Rectangle getClip(Rectangle rect)
    {
        if (!relativeClipRegion.equals(NOCLIPPING))
        {
            rect.setBounds(relativeClipRegion);
        }
        else
        {
            rect.setBounds(0, 0, Integer.MAX_VALUE, Integer.MAX_VALUE);
        }
        return rect;
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.draw2d.Graphics#getFont()
     */
    @Override
    public Font getFont()
    {
        return swtGraphics.getFont();
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.draw2d.Graphics#getFontMetrics()
     */
    @Override
    public FontMetrics getFontMetrics()
    {
        return swtGraphics.getFontMetrics();
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.draw2d.Graphics#getForegroundColor()
     */
    @Override
    public Color getForegroundColor()
    {
        return swtGraphics.getForegroundColor();
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.draw2d.Graphics#getLineStyle()
     */
    @Override
    public int getLineStyle()
    {
        return swtGraphics.getLineStyle();
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.draw2d.Graphics#getLineWidth()
     */
    @Override
    public int getLineWidth()
    {
        return swtGraphics.getLineWidth();
    }

    @Override
    public float getLineWidthFloat()
    {
        return swtGraphics.getLineWidthFloat();
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.draw2d.Graphics#getXORMode()
     */
    @Override
    public boolean getXORMode()
    {
        return swtGraphics.getXORMode();
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.draw2d.Graphics#popState()
     */
    @Override
    public void popState()
    {
        swtGraphics.popState();

        restoreState(states.pop());
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.draw2d.Graphics#pushState()
     */
    @Override
    public void pushState()
    {
        swtGraphics.pushState();

        // Make a copy of the current state and push it onto the stack
        State toPush = new State();
        toPush.copyFrom(currentState);
        states.push(toPush);
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.draw2d.Graphics#restoreState()
     */
    @Override
    public void restoreState()
    {
        swtGraphics.restoreState();

        restoreState(states.peek());
    }

    private void restoreState(State state)
    {

        setBackgroundColor(state.bgColor);
        setForegroundColor(state.fgColor);
        setLineAttributes(state.lineAttributes);
        setXORMode(state.XorMode);

        setClipAbsolute(state.clippingActive, state.clipX, state.clipY, state.clipW, state.clipH);

        transX = currentState.translateX = state.translateX;
        transY = currentState.translateY = state.translateY;

        if (state.clippingActive)
        {
            if (relativeClipRegion.equals(NOCLIPPING))
            {
                relativeClipRegion = new Rectangle();
            }
            relativeClipRegion.x = state.clipX - transX;
            relativeClipRegion.y = state.clipY - transY;
            relativeClipRegion.width = state.clipW;
            relativeClipRegion.height = state.clipH;
        }
        else
        {
            relativeClipRegion = NOCLIPPING;
        }

        currentState.font = state.font;
        currentState.alpha = state.alpha;

    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.draw2d.Graphics#scale(double)
     */
    @Override
    public void scale(double amount)
    {
        swtGraphics.scale(amount);
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.draw2d.Graphics#setBackgroundColor(org.eclipse.swt.graphics .Color)
     */
    @Override
    public void setBackgroundColor(Color rgb)
    {
        currentState.bgColor = rgb;
        swtGraphics.setBackgroundColor(rgb);
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.draw2d.Graphics#setClip(org.eclipse.draw2d.geometry.Rectangle )
     */
    @Override
    public void setClip(Rectangle rect)
    {

        if (relativeClipRegion.equals(NOCLIPPING))
        {
            relativeClipRegion = new Rectangle();
        }
        relativeClipRegion.x = rect.x;
        relativeClipRegion.y = rect.y;
        relativeClipRegion.width = rect.width;
        relativeClipRegion.height = rect.height;

        setClipAbsolute(true, rect.x + transX, rect.y + transY, rect.width, rect.height);
    }

    /**
     * Sets the current clip values
     * 
     * @param x the x value
     * @param y the y value
     * @param width the width value
     * @param height the height value
     */
    private void setClipAbsolute(boolean active, int x, int y, int width, int height)
    {
        currentState.clippingActive = active;
        if (active)
        {
            currentState.clipX = x;
            currentState.clipY = y;
            currentState.clipW = width;
            currentState.clipH = height;
        }
    }

    private boolean isFontUnderlined(Font f)
    {
        return false;
    }

    private boolean isFontStrikeout(Font f)
    {
        return false;
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.draw2d.Graphics#setFont(org.eclipse.swt.graphics.Font)
     */
    @Override
    public void setFont(Font f)
    {

        swtGraphics.setFont(f);
        currentState.font = f;

        FontData[] fontInfo = f.getFontData();

        if (fontInfo[0] != null)
        {

            int height = fontInfo[0].getHeight();

            float fsize = height * (float) Display.getCurrent().getDPI().x / 72.0f;
            height = Math.round(fsize);

            int style = fontInfo[0].getStyle();
            boolean bItalic = (style & SWT.ITALIC) == SWT.ITALIC;
            boolean bBold = (style & SWT.BOLD) == SWT.BOLD;
            String faceName = fontInfo[0].getName();
            int escapement = 0;

            boolean bUnderline = isFontUnderlined(f);
            boolean bStrikeout = isFontStrikeout(f);

            GdiFont font = new GdiFont(height, bItalic, bUnderline, bStrikeout, bBold, faceName, escapement);

            getGraphics2D().setFont(font.getFont());
        }
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.draw2d.Graphics#setForegroundColor(org.eclipse.swt.graphics .Color)
     */
    @Override
    public void setForegroundColor(Color rgb)
    {
        currentState.fgColor = rgb;
        swtGraphics.setForegroundColor(rgb);
    }

    /**
     * Sets the dash pattern when the custom line style is in use. Because this feature is rarely used, the dash pattern
     * may not be preserved when calling {@link #pushState()} and {@link #popState()}.
     * 
     * @param dash the pixel pattern
     * 
     */
    @Override
    public void setLineDash(int[] dash)
    {
        float dashFlt[] = new float[dash.length];
        for (int i = 0; i < dash.length; i++)
        {
            dashFlt[i] = dash[i];
        }
        setLineDash(dashFlt);
    }

    @Override
    public void setLineDash(float[] dash)
    {
        currentState.lineAttributes.dash = dash;
        setLineStyle(SWTGraphics.LINE_CUSTOM);
        swtGraphics.setLineDash(dash);
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.draw2d.Graphics#setLineStyle(int)
     */
    @Override
    public void setLineStyle(int style)
    {
        currentState.lineAttributes.style = style;
        swtGraphics.setLineStyle(style);
    }

    /**
     * ignored
     */
    @Override
    public void setLineMiterLimit(float miterLimit)
    {
        // do nothing
    }

    /**
     * ignored
     */
    @Override
    public void setLineCap(int cap)
    {
        // do nothing
    }

    /**
     * ignored
     */
    @Override
    public void setLineJoin(int join)
    {
        // do nothing
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.draw2d.Graphics#setLineWidth(int)
     */
    @Override
    public void setLineWidth(int width)
    {
        setLineWidthFloat(width);
    }

    @Override
    public void setLineWidthFloat(float width)
    {
        currentState.lineAttributes.width = width;
        swtGraphics.setLineWidthFloat(width);
    }

    @Override
    public void setLineAttributes(LineAttributes lineAttributes)
    {
        SWTGraphics.copyLineAttributes(currentState.lineAttributes, lineAttributes);
        swtGraphics.setLineAttributes(lineAttributes);
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.draw2d.Graphics#setXORMode(boolean)
     */
    @Override
    public void setXORMode(boolean xorMode)
    {
        currentState.XorMode = xorMode;
        swtGraphics.setXORMode(xorMode);
    }

    /**
     * Sets the current translation values
     * 
     * @param x the x translation value
     * @param y the y translation value
     */
    private void setTranslation(int x, int y)
    {
        transX = currentState.translateX = x;
        transY = currentState.translateY = y;
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.draw2d.Graphics#translate(int, int)
     */
    @Override
    public void translate(int dx, int dy)
    {
        swtGraphics.translate(dx, dy);

        setTranslation(transX + dx, transY + dy);
        if (!relativeClipRegion.equals(NOCLIPPING))
        {
            relativeClipRegion.x -= dx;
            relativeClipRegion.y -= dy;
        }
    }

    /**
     * @return the <code>Graphics2D</code> that this is delegating to.
     */
    protected Graphics2D getGraphics2D()
    {
        return graphics2D;
    }

    /**
     * @return Returns the swtGraphics.
     */
    private SWTGraphics getSWTGraphics()
    {
        return swtGraphics;
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.draw2d.Graphics#fillGradient(int, int, int, int, boolean)
     */
    @Override
    public void fillGradient(int x, int y, int w, int h, boolean vertical)
    {
        GradientPaint gradient;

        checkState();

        // Gradients in SWT start with Foreground Color and end at Background
        java.awt.Color start = getColor(getSWTGraphics().getForegroundColor());
        java.awt.Color stop = getColor(getSWTGraphics().getBackgroundColor());

        // Create the Gradient based on horizontal or vertical
        if (vertical)
        {
            gradient = new GradientPaint(x + transX, y + transY, start, x + transX, y + h + transY, stop);
        }
        else
        {
            gradient = new GradientPaint(x + transX, y + transY, start, x + w + transX, y + transY, stop);
        }

        Paint oldPaint = getGraphics2D().getPaint();
        getGraphics2D().setPaint(gradient);
        getGraphics2D().fill(new Rectangle2D.Double(x + transX, y + transY, w, h));
        getGraphics2D().setPaint(oldPaint);
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.draw2d.Graphics#drawPath(org.eclipse.swt.graphics.Path)
     */
    @Override
    public void drawPath(Path path)
    {
        checkState();
        getGraphics2D().setPaint(getColor(swtGraphics.getForegroundColor()));
        getGraphics2D().setStroke(createStroke());
        getGraphics2D().draw(createPathAWT(path));
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.draw2d.Graphics#fillPath(org.eclipse.swt.graphics.Path)
     */
    @Override
    public void fillPath(Path path)
    {
        checkState();
        getGraphics2D().setPaint(getColor(swtGraphics.getBackgroundColor()));
        getGraphics2D().fill(createPathAWT(path));
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.draw2d.Graphics#setClip(org.eclipse.swt.graphics.Path)
     */
    @Override
    public void setClip(Path path)
    {
        if (((appliedState.graphicHints ^ currentState.graphicHints) & FILL_RULE_MASK) != 0)
        {
            // If there is a pending change to the fill rule, apply it first.
            // As long as the FILL_RULE is stored in a single bit, just toggling
            // it works.
            appliedState.graphicHints ^= FILL_RULE_MASK;
        }
        getGraphics2D().setClip(createPathAWT(path));
        // Note not tested
        appliedState.clippingActive = currentState.clippingActive = false;
        appliedState.clipX = currentState.clipX = 0;
        appliedState.clipY = currentState.clipY = 0;
        appliedState.clipW = currentState.clipW = 0;
        appliedState.clipH = currentState.clipH = 0;
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.draw2d.Graphics#getFillRule()
     */
    @Override
    public int getFillRule()
    {
        return ((currentState.graphicHints & FILL_RULE_MASK) >> FILL_RULE_SHIFT) - FILL_RULE_WHOLE_NUMBER;
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.draw2d.Graphics#setFillRule(int)
     */
    @Override
    public void setFillRule(int rule)
    {
        currentState.graphicHints &= ~FILL_RULE_MASK;
        currentState.graphicHints |= (rule + FILL_RULE_WHOLE_NUMBER) << FILL_RULE_SHIFT;
    }

    private GeneralPath createPathAWT(Path path)
    {
        GeneralPath pathAWT = new GeneralPath();
        PathData pathData = path.getPathData();
        int idx = 0;
        for (int i = 0; i < pathData.types.length; i++)
        {
            switch (pathData.types[i])
            {
            case SWT.PATH_MOVE_TO:
                pathAWT.moveTo(pathData.points[idx++] + transX, pathData.points[idx++] + transY);
                break;
            case SWT.PATH_LINE_TO:
                pathAWT.lineTo(pathData.points[idx++] + transX, pathData.points[idx++] + transY);
                break;
            case SWT.PATH_CUBIC_TO:
                pathAWT.curveTo(pathData.points[idx++] + transX, pathData.points[idx++] + transY,
                        pathData.points[idx++] + transX, pathData.points[idx++] + transY, pathData.points[idx++]
                                + transX, pathData.points[idx++] + transY);
                break;
            case SWT.PATH_QUAD_TO:
                pathAWT.quadTo(pathData.points[idx++] + transX, pathData.points[idx++] + transY, pathData.points[idx++]
                        + transX, pathData.points[idx++] + transY);
                break;
            case SWT.PATH_CLOSE:
                pathAWT.closePath();
                break;
            default:
                dispose();
                SWT.error(SWT.ERROR_INVALID_ARGUMENT);
            }
        }
        int swtWindingRule = ((appliedState.graphicHints & FILL_RULE_MASK) >> FILL_RULE_SHIFT) - FILL_RULE_WHOLE_NUMBER;
        if (swtWindingRule == SWT.FILL_WINDING)
        {
            pathAWT.setWindingRule(GeneralPath.WIND_NON_ZERO);
        }
        else if (swtWindingRule == SWT.FILL_EVEN_ODD)
        {
            pathAWT.setWindingRule(GeneralPath.WIND_EVEN_ODD);
        }
        else
        {
            SWT.error(SWT.ERROR_INVALID_ARGUMENT);
        }
        return pathAWT;
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.gmf.runtime.draw2d.ui.render.awt.internal.DrawableRenderedImage #allowDelayRender()
     */
    public boolean shouldAllowDelayRender()
    {
        return false;
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.gmf.runtime.draw2d.ui.render.awt.internal.DrawableRenderedImage #getMaximumRenderSize()
     */
    public Dimension getMaximumRenderSize()
    {
        return null;
    }

    /**
     * Accessor method to return the translation offset for the graphics object
     * 
     * @return <code>Point</code> x coordinate for graphics translation
     */
    protected Point getTranslationOffset()
    {
        return new Point(transX, transY);
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.draw2d.Graphics#getAntialias()
     */
    @Override
    public int getAntialias()
    {
        Object antiAlias = getGraphics2D().getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        if (antiAlias != null)
        {
            if (antiAlias.equals(RenderingHints.VALUE_ANTIALIAS_ON))
                return SWT.ON;
            else if (antiAlias.equals(RenderingHints.VALUE_ANTIALIAS_OFF))
                return SWT.OFF;
            else if (antiAlias.equals(RenderingHints.VALUE_ANTIALIAS_DEFAULT))
                return SWT.DEFAULT;
        }

        return SWT.DEFAULT;
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.draw2d.Graphics#setAntialias(int)
     */
    @Override
    public void setAntialias(int value)
    {
        if (value == SWT.ON)
        {
            getGraphics2D().setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        }
        else if (value == SWT.OFF)
        {
            getGraphics2D().setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        }
        setAdvanced(true);
    }

    @Override
    public int getAlpha()
    {
        return swtGraphics.getAlpha();
    }

    @Override
    public void setAlpha(int alpha)
    {
        swtGraphics.setAlpha(alpha);
        currentState.alpha = alpha;

        Composite composite = getGraphics2D().getComposite();
        if (composite instanceof AlphaComposite)
        {
            AlphaComposite newComposite = AlphaComposite.getInstance(((AlphaComposite) composite).getRule(),
                    (float) alpha / (float) 255);
            getGraphics2D().setComposite(newComposite);
        }
    }

    protected BasicStroke getStroke()
    {
        return stroke;
    }

    protected void setStroke(BasicStroke stroke)
    {
        this.stroke = stroke;
        getGraphics2D().setStroke(stroke);
    }

    /**
     * Sets and retirns AWT Stroke based on the value of <code>LineAttributes</code> within the current state object
     * 
     * @return the new AWT stroke
     */
    private Stroke createStroke()
    {
        float factor = currentState.lineAttributes.width > 0 ? currentState.lineAttributes.width : 3;
        float awt_dash[];
        int awt_cap;
        int awt_join;

        switch (currentState.lineAttributes.style)
        {
        case SWTGraphics.LINE_DASH:
            awt_dash = new float[] { factor * 6, factor * 3 };
            break;
        case SWTGraphics.LINE_DASHDOT:
            awt_dash = new float[] { factor * 3, factor, factor, factor };
            break;
        case SWTGraphics.LINE_DASHDOTDOT:
            awt_dash = new float[] { factor * 3, factor, factor, factor, factor, factor };
            break;
        case SWTGraphics.LINE_DOT:
            awt_dash = new float[] { factor, factor };
            break;
        case SWTGraphics.LINE_CUSTOM:
            awt_dash = currentState.lineAttributes.dash;
            break;
        default:
            awt_dash = null;
        }

        switch (currentState.lineAttributes.cap)
        {
        case SWT.CAP_FLAT:
            awt_cap = BasicStroke.CAP_BUTT;
            break;
        case SWT.CAP_ROUND:
            awt_cap = BasicStroke.CAP_ROUND;
            break;
        case SWT.CAP_SQUARE:
            awt_cap = BasicStroke.CAP_SQUARE;
            break;
        default:
            awt_cap = BasicStroke.CAP_BUTT;
        }

        switch (currentState.lineAttributes.join)
        {
        case SWT.JOIN_BEVEL:
            awt_join = BasicStroke.JOIN_BEVEL;
            break;
        case SWT.JOIN_MITER:
            awt_join = BasicStroke.JOIN_MITER;
            break;
        case SWT.JOIN_ROUND:
            awt_join = BasicStroke.JOIN_ROUND;
        default:
            awt_join = BasicStroke.JOIN_MITER;
        }

        /*
         * SWT paints line width == 0 as if it is == 1, so AWT is synced up with that below.
         */
        stroke = new BasicStroke(currentState.lineAttributes.width != 0 ? currentState.lineAttributes.width : 1,
                awt_cap, awt_join, currentState.lineAttributes.miterLimit, awt_dash,
                currentState.lineAttributes.dashOffset);
        return stroke;
    }

    @Override
    public boolean getAdvanced()
    {
        return (currentState.graphicHints & ADVANCED_GRAPHICS_MASK) != 0;
    }

    @Override
    public void setAdvanced(boolean value)
    {
        if (value)
        {
            currentState.graphicHints |= ADVANCED_GRAPHICS_MASK;
        }
        else
        {
            currentState.graphicHints &= ~ADVANCED_GRAPHICS_MASK;
        }
    }

    @Override
    public void clipPath(Path path)
    {
        if (((appliedState.graphicHints ^ currentState.graphicHints) & FILL_RULE_MASK) != 0)
        {
            // If there is a pending change to the fill rule, apply it first.
            // As long as the FILL_RULE is stored in a single bit, just toggling
            // it works.
            appliedState.graphicHints ^= FILL_RULE_MASK;
        }
        setClip(path);
        // Note not tested
        if (!relativeClipRegion.equals(NOCLIPPING))
        {
            getGraphics2D().clipRect(relativeClipRegion.x + transX, relativeClipRegion.y + transY,
                    relativeClipRegion.width, relativeClipRegion.height);
        }
        java.awt.Rectangle bounds = getGraphics2D().getClip().getBounds();
        relativeClipRegion = new Rectangle(bounds.x, bounds.y, bounds.width, bounds.height);
    }
}

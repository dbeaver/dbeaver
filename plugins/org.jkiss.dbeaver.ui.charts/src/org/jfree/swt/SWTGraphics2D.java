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

package org.jfree.swt;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.*;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.*;
import java.awt.RenderingHints.Key;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.*;
import java.awt.image.*;
import java.awt.image.renderable.RenderableImage;
import java.text.AttributedCharacterIterator;
import java.util.List;
import java.util.*;

/**
 * An implementation of the {@code Graphics2D} API targeting an SWT graphics
 * context.
 */
public class SWTGraphics2D extends Graphics2D {

    /** The SWT graphic composite */
    private GC gc;

    /**
     * The rendering hints.  For now, these are not used, but at least the
     * basic mechanism is present.
     */
    private RenderingHints hints;

    /** The user clip. */
    private Shape clip;

    /** Save the initial clip for when the user clip is reset to null. */
    private org.eclipse.swt.graphics.Rectangle swtInitialClip;

    private Font awtFont;

    /** The AWT color that has been set. */
    private Color awtColor;

    /** The AWT paint that has been set. */
    private Paint awtPaint;

    /** The current transform (protect this, only hand out copies). */
    private AffineTransform transform;

    /** 
     * A reference to the compositing rule to apply. This is necessary
     * due to the poor compositing interface of the SWT toolkit. 
     */
    private java.awt.Composite composite;

    /**
     * The device configuration (this is lazily instantiated in the
     * getDeviceConfiguration() method).
     */
    private GraphicsConfiguration deviceConfiguration;

    /** A HashMap to store the SWT color resources. */
    private Map colorsPool = new HashMap();

    /** A HashMap to store the SWT font resources. */
    private Map fontsPool = new HashMap();

    /** A pool for storing SWT pattern resources. */
    private Map<GradientPaint, Pattern> patternsPool = new HashMap<>();

    /** A HashMap to store the SWT transform resources. */
    private Map transformsPool = new HashMap();

    /** A List to store the SWT resources. */
    private List resourcePool = new ArrayList();

    /**
     * Creates a new instance.
     *
     * @param gc  the graphics context.
     */
    public SWTGraphics2D(GC gc) {
        super();
        this.gc = gc;
        this.hints = new RenderingHints(null);
        this.transform = new AffineTransform();
        this.composite = AlphaComposite.getInstance(AlphaComposite.SRC, 1.0f);
        this.clip = null;
        this.swtInitialClip = gc.getClipping();
        setStroke(new BasicStroke());
    }

    /**
     * Creates a new graphics object that is a copy of this graphics object.
     *
     * @return A new graphics object.
     */
    public Graphics create() {
        SWTGraphics2D copy = new SWTGraphics2D(this.gc);
        copy.setRenderingHints(getRenderingHints());
        copy.setTransform(getTransform());
        copy.setClip(getClip());
        copy.setPaint(getPaint());
        copy.setColor(getColor());
        copy.setComposite(getComposite());
        copy.setStroke(getStroke());
        copy.setFont(getFont());
        copy.setBackground(getBackground());
        return copy;
    }

    /**
     * Returns the device configuration associated with this
     * {@code Graphics2D}.
     *
     * @return The device configuration (never {@code null}).
     */
    @Override
    public GraphicsConfiguration getDeviceConfiguration() {
        if (this.deviceConfiguration == null) {
            int width = this.gc.getDevice().getBounds().width;
            int height = this.gc.getDevice().getBounds().height;
            this.deviceConfiguration = new SWTGraphicsConfiguration(width,
                    height);
        }
        return this.deviceConfiguration;
    }

    /**
     * Returns the current value for the specified hint key, or
     * {@code null} if no value is set.
     *
     * @param hintKey  the hint key ({@code null} permitted).
     *
     * @return The hint value, or {@code null}.
     *
     * @see #setRenderingHint(RenderingHints.Key, Object)
     */
    @Override
    public Object getRenderingHint(Key hintKey) {
        return this.hints.get(hintKey);
    }

    /**
     * Sets the value for a rendering hint.  For now, this graphics context
     * ignores all hints.
     *
     * @param hintKey  the key ({@code null} not permitted).
     * @param hintValue  the value (must be compatible with the specified key).
     *
     * @throws IllegalArgumentException if {@code hintValue} is not
     *         compatible with the {@code hintKey}.
     *
     * @see #getRenderingHint(RenderingHints.Key)
     */
    @Override
    public void setRenderingHint(Key hintKey, Object hintValue) {
        this.hints.put(hintKey, hintValue);
    }

    /**
     * Returns a copy of the hints collection for this graphics context.
     *
     * @return A copy of the hints collection.
     */
    @Override
    public RenderingHints getRenderingHints() {
        return (RenderingHints) this.hints.clone();
    }

    /**
     * Adds the hints in the specified map to the graphics context, replacing
     * any existing hints.  For now, this graphics context ignores all hints.
     *
     * @param hints  the hints ({@code null} not permitted).
     *
     * @see #setRenderingHints(Map)
     */
    @Override
    public void addRenderingHints(Map hints) {
        this.hints.putAll(hints);
    }

    /**
     * Replaces the existing hints with those contained in the specified
     * map.  Note that, for now, this graphics context ignores all hints.
     *
     * @param hints  the hints ({@code null} not permitted).
     *
     * @see #addRenderingHints(Map)
     */
    @Override
    public void setRenderingHints(Map hints) {
        if (hints == null) {
            throw new NullPointerException("Null 'hints' argument.");
        }
        this.hints = new RenderingHints(hints);
    }

    /**
     * Returns the current paint for this graphics context.
     *
     * @return The current paint.
     *
     * @see #setPaint(Paint)
     */
    @Override
    public Paint getPaint() {
        return this.awtPaint;
    }

    /**
     * Sets the paint for this graphics context.  For now, this graphics
     * context only supports instances of {@link Color} or
     * {@link GradientPaint} (in the latter case there is no real gradient
     * support, the paint used is the {@code Color} returned by
     * {@code getColor1()}).
     *
     * @param paint  the paint ({@code null} permitted, ignored).
     *
     * @see #getPaint()
     * @see #setColor(Color)
     */
    @Override
    public void setPaint(Paint paint) {
        if (paint == null) {
            return;  // to be consistent with other Graphics2D implementations
        }
        this.awtPaint = paint;
        if (paint instanceof Color) {
            this.awtColor = (Color) paint;
            org.eclipse.swt.graphics.Color swtColor = getSwtColorFromPool(this.awtColor);
            this.gc.setForeground(swtColor);
            // handle transparency and compositing.
            if (this.composite instanceof AlphaComposite) {
                AlphaComposite acomp = (AlphaComposite) this.composite;
                switch (acomp.getRule()) {
                    case AlphaComposite.SRC_OVER:
                        this.gc.setAlpha((int) (this.awtColor.getAlpha() * acomp.getAlpha()));
                        break;
                    default:
                        this.gc.setAlpha(this.awtColor.getAlpha());
                        break;
                }
            }
        }
        else if (paint instanceof GradientPaint) {
            GradientPaint gp = (GradientPaint) paint;
            Pattern pattern = fetchOrCreateSWTPattern(gp);
            this.gc.setForegroundPattern(pattern);
            this.gc.setBackgroundPattern(pattern);
        } else if (paint instanceof MultipleGradientPaint) {
            MultipleGradientPaint mgp = (MultipleGradientPaint) paint;
            // how to handle?
        }
        else {
            throw new RuntimeException("Can only handle 'Color' and 'GradientPaint' at present.");
        }
    }

    /**
     * Returns the current color for this graphics context.
     *
     * @return The current color.
     *
     * @see #setColor(Color)
     */
    @Override
    public Color getColor() {
        return this.awtColor;
    }

    /**
     * Sets the foreground color.  This method exists for backwards
     * compatibility in AWT, you should use the
     * {@link #setPaint(java.awt.Paint)} method.
     *
     * @param color  the color ({@code null} permitted but ignored).
     *
     * @see #setPaint(java.awt.Paint)
     */
    @Override
    public void setColor(Color color) {
        if (color == null) {
            return;
        }
        setPaint(color);
    }

    private Color backgroundColor;
    
    /**
     * Sets the background color.
     *
     * @param color  the color.
     */
    @Override
    public void setBackground(Color color) {
        // since this is only used by clearRect(), we don't update the GC yet
        this.backgroundColor = color;
    }

    /**
     * Returns the background color.
     *
     * @return The background color (possibly {@code null})..
     */
    @Override
    public Color getBackground() {
        return this.backgroundColor;
    }

    /**
     * Not implemented - see {@link Graphics#setPaintMode()}.
     */
    @Override
    public void setPaintMode() {
        // TODO Auto-generated method stub
    }

    /**
     * Not implemented - see {@link Graphics#setXORMode(Color)}.
     *
     * @param color  the color.
     */
    @Override
    public void setXORMode(Color color) {
        // TODO Auto-generated method stub
    }

    /**
     * Returns the current composite.
     *
     * @return The current composite.
     *
     * @see #setComposite(Composite)
     */
    @Override
    public Composite getComposite() {
        return this.composite;
    }

    /**
     * Sets the current composite.  This implementation currently supports
     * only the {@link AlphaComposite} class.
     *
     * @param comp  the composite ({@code null} not permitted).
     */
    @Override
    public void setComposite(Composite comp) {
        if (comp == null) {
            throw new IllegalArgumentException("Null 'comp' argument.");
        }
        this.composite = comp;
        if (comp instanceof AlphaComposite) {
            AlphaComposite acomp = (AlphaComposite) comp;
            int alpha = (int) (acomp.getAlpha() * 0xFF);
            this.gc.setAlpha(alpha);
        }
    }

    /**
     * Returns the current stroke for this graphics context.
     *
     * @return The current stroke.
     *
     * @see #setStroke(Stroke)
     */
    @Override
    public Stroke getStroke() {
        return new BasicStroke(this.gc.getLineWidth(),
                toAwtLineCap(this.gc.getLineCap()),
                toAwtLineJoin(this.gc.getLineJoin()));
    }

    /**
     * Sets the stroke for this graphics context.  For now, this implementation
     * only recognises the {@link BasicStroke} class.
     *
     * @param stroke  the stroke ({@code null} not permitted).
     *
     * @see #getStroke()
     */
    @Override
    public void setStroke(Stroke stroke) {
        if (stroke == null) {
            throw new IllegalArgumentException("Null 'stroke' argument.");
        }
        if (stroke instanceof BasicStroke) {
            BasicStroke bs = (BasicStroke) stroke;
            this.gc.setLineWidth((int) bs.getLineWidth());
            this.gc.setLineJoin(toSwtLineJoin(bs.getLineJoin()));
            this.gc.setLineCap(toSwtLineCap(bs.getEndCap()));

            // set the line style to solid by default
            this.gc.setLineStyle(SWT.LINE_SOLID);

            // apply dash style if any
            float[] dashes = bs.getDashArray();
            if (dashes != null) {
                int[] swtDashes = new int[dashes.length];
                for (int i = 0; i < swtDashes.length; i++) {
                    swtDashes[i] = (int) dashes[i];
                }
                this.gc.setLineDash(swtDashes);
            }
        }
        else {
            throw new RuntimeException(
                    "Can only handle 'Basic Stroke' at present.");
        }
    }

    /**
     * Applies the specified clip.
     *
     * @param s  the shape for the clip.
     */
    @Override
    public void clip(Shape s) {
        if (s instanceof Line2D) {
            s = s.getBounds2D();
        }
        if (this.clip == null) {
            setClip(s);
            return;
        }
        Shape ts = this.transform.createTransformedShape(s);
        if (!ts.intersects(this.clip.getBounds2D())) {
            setClip(new Rectangle2D.Double());
            return;
        } else {
            Area a1 = new Area(s);
            Area a2 = new Area(getClip());
            a1.intersect(a2);
            setClip(new Path2D.Double(a1));
        }
    }

    /**
     * Returns the clip bounds.
     *
     * @return The clip bounds (possibly {@code null)}.
     */
    @Override
    public Rectangle getClipBounds() {
        if (getClip() == null) {
            return null;
        }
        return getClip().getBounds();
    }

    /**
     * Sets the clipping to the intersection of the current clip region and
     * the specified rectangle.
     *
     * @param x  the x-coordinate.
     * @param y  the y-coordinate.
     * @param width  the width.
     * @param height  the height.
     */
    @Override
    public void clipRect(int x, int y, int width, int height) {
        setRect(x, y, width, height);
        clip(this.rect);
    }

    /**
     * Returns the user clipping region.  The initial default value is
     * {@code null}.
     *
     * @return The user clipping region (possibly {@code null}).
     *
     * @see #setClip(java.awt.Shape)
     */
    @Override
    public Shape getClip() {
        if (this.clip == null) {
            return null;
        }
        try {
            AffineTransform inv = this.transform.createInverse();
            return inv.createTransformedShape(this.clip);
        } catch (NoninvertibleTransformException ex) {
            return null;
        }
    }

    /**
     * Sets the clip region.
     *
     * @param region  the clip.
     */
    @Override
    public void setClip(Shape region) {
        this.clip = this.transform.createTransformedShape(region);
        if (this.clip != null) {
            Path clipPath = toSwtPath(region);
            this.gc.setClipping(clipPath);
            clipPath.dispose();
        } else {
            AffineTransform saved = getTransform();
            setTransform(null);
            this.gc.setClipping(swtInitialClip);
            setTransform(saved);
        }
    }

    /**
     * Sets the clip region to the specified rectangle.
     *
     * @param x  the x-coordinate.
     * @param y  the y-coordinate.
     * @param width  the width.
     * @param height  the height.
     */
    @Override
    public void setClip(int x, int y, int width, int height) {
        setRect(x, y, width, height);
        setClip(this.rect);
    }

    /**
     * Returns a copy of the current transform.
     *
     * @return A copy of the current transform (never {@code null}).
     *
     * @see #setTransform(java.awt.geom.AffineTransform)
     */
    @Override
    public AffineTransform getTransform() {
        return (AffineTransform) this.transform.clone();
    }

    /**
     * Sets the transform.
     *
     * @param t  the new transform ({@code null} permitted, resets to the
     *     identity transform).
     *
     * @see #getTransform()
     */
    @Override
    public void setTransform(AffineTransform t) {
        if (t == null) {
            this.transform = new AffineTransform();
        } else {
            this.transform = new AffineTransform(t);
        }
        Transform swtTransform = getSwtTransformFromPool(this.transform);
        this.gc.setTransform(swtTransform);
    }

    /**
     * Applies this transform to the existing transform by concatenating it.
     *
     * @param t  the transform ({@code null} not permitted).
     */
    @Override
    public void transform(AffineTransform t) {
        AffineTransform tx = getTransform();
        tx.concatenate(t);
        setTransform(tx);
    }

    /**
     * Applies the translation {@code (tx, ty)}.  This call is delegated
     * to {@link #translate(double, double)}.
     *
     * @param tx  the x-translation.
     * @param ty  the y-translation.
     *
     * @see #translate(double, double)
     */
    @Override
    public void translate(int tx, int ty) {
        translate((double) tx, (double) ty);
    }

    /**
     * Applies the translation {@code (tx, ty)}.
     *
     * @param tx  the x-translation.
     * @param ty  the y-translation.
     */
    @Override
    public void translate(double tx, double ty) {
        AffineTransform t = getTransform();
        t.translate(tx, ty);
        setTransform(t);
    }

    /**
     * Applies a rotation (anti-clockwise) about {@code (0, 0)}.
     *
     * @param theta  the rotation angle (in radians).
     */
    @Override
    public void rotate(double theta) {
        AffineTransform t = getTransform();
        t.rotate(theta);
        setTransform(t);
    }

    /**
     * Applies a rotation (anti-clockwise) about {@code (x, y)}.
     *
     * @param theta  the rotation angle (in radians).
     * @param x  the x-coordinate.
     * @param y  the y-coordinate.
     */
    @Override
    public void rotate(double theta, double x, double y) {
        translate(x, y);
        rotate(theta);
        translate(-x, -y);
    }

    /**
     * Applies a scale transform.
     *
     * @param sx  the scale factor along the x-axis.
     * @param sy  the scale factor along the y-axis.
     */
    @Override
    public void scale(double sx, double sy) {
        AffineTransform t = getTransform();
        t.scale(sx, sy);
        setTransform(t);
    }

    /**
     * Applies a shear transformation. This is equivalent to the following
     * call to the {@code transform} method:
     * <br><br>
     * <ul><li>
     * {@code transform(AffineTransform.getShearInstance(shx, shy));}
     * </ul>
     *
     * @param shx  the x-shear factor.
     * @param shy  the y-shear factor.
     */
    @Override
    public void shear(double shx, double shy) {
        transform(AffineTransform.getShearInstance(shx, shy));
    }

    /**
     * Draws the outline of the specified shape using the current stroke and
     * paint settings.
     *
     * @param shape  the shape ({@code null} not permitted).
     *
     * @see #getPaint()
     * @see #getStroke()
     * @see #fill(Shape)
     */
    @Override
    public void draw(Shape shape) {
        Path path = toSwtPath(shape);
        this.gc.drawPath(path);
        path.dispose();
    }

    /**
     * Draws a line from (x1, y1) to (x2, y2) using the current stroke
     * and paint settings.
     *
     * @param x1  the x-coordinate for the starting point.
     * @param y1  the y-coordinate for the starting point.
     * @param x2  the x-coordinate for the ending point.
     * @param y2  the y-coordinate for the ending point.
     *
     * @see #draw(Shape)
     */
    @Override
    public void drawLine(int x1, int y1, int x2, int y2) {
        this.gc.drawLine(x1, y1, x2, y2);
    }

    /**
     * Draws the outline of the polygon specified by the given points, using
     * the current paint and stroke settings.
     *
     * @param xPoints  the x-coordinates.
     * @param yPoints  the y-coordinates.
     * @param npoints  the number of points in the polygon.
     *
     * @see #draw(Shape)
     */
    @Override
    public void drawPolygon(int[] xPoints, int[] yPoints, int npoints) {
		final int[] polygons = new int[npoints * 2];
		for (int i = 0; i < npoints; i++) {
			polygons[i * 2] = xPoints[i];
			polygons[(2 * i) + 1] = yPoints[i];
		}
		gc.drawPolygon(polygons);
	}

    /**
     * Draws a sequence of connected lines specified by the given points, using
     * the current paint and stroke settings.
     *
     * @param xPoints  the x-coordinates.
     * @param yPoints  the y-coordinates.
     * @param npoints  the number of points in the polyline.
     *
     * @see #draw(Shape)
     */
    @Override
    public void drawPolyline(int[] xPoints, int[] yPoints, int npoints) {
		if (npoints > 1) {
			final int[] polylines = new int[npoints * 2];
			for (int i = 0; i < npoints; i++) {
				polylines[i * 2] = xPoints[i];
				polylines[(2 * i) + 1] = yPoints[i];
			}
			gc.drawPolyline(polylines);

		}
	}

    /**
     * Draws an oval that fits within the specified rectangular region.
     *
     * @param x  the x-coordinate.
     * @param y  the y-coordinate.
     * @param width  the frame width.
     * @param height  the frame height.
     *
     * @see #fillOval(int, int, int, int)
     * @see #draw(Shape)
     */
    @Override
    public void drawOval(int x, int y, int width, int height) {
        this.gc.drawOval(x, y, width - 1, height - 1);
    }

    /**
     * Draws an arc that is part of an ellipse that fits within the specified
     * framing rectangle.
     *
     * @param x  the x-coordinate.
     * @param y  the y-coordinate.
     * @param width  the frame width.
     * @param height  the frame height.
     * @param arcStart  the arc starting point, in degrees.
     * @param arcAngle  the extent of the arc.
     *
     * @see #fillArc(int, int, int, int, int, int)
     */
    @Override
    public void drawArc(int x, int y, int width, int height, int arcStart,
            int arcAngle) {
        this.gc.drawArc(x, y, width - 1, height - 1, arcStart, arcAngle);
    }

    /**
     * Draws a rectangle with rounded corners that fits within the specified
     * framing rectangle.
     *
     * @param x  the x-coordinate.
     * @param y  the y-coordinate.
     * @param width  the frame width.
     * @param height  the frame height.
     * @param arcWidth  the width of the arc defining the roundedness of the
     *         rectangle's corners.
     * @param arcHeight the height of the arc defining the roundedness of the
     *         rectangle's corners.
     *
     * @see #fillRoundRect(int, int, int, int, int, int)
     */
    @Override
    public void drawRoundRect(int x, int y, int width, int height,
            int arcWidth, int arcHeight) {
        this.gc.drawRoundRectangle(x, y, width - 1, height - 1, arcWidth,
                arcHeight);
    }

    /**
     * Fills the specified shape using the current paint.
     *
     * @param shape  the shape ({@code null} not permitted).
     *
     * @see #getPaint()
     * @see #draw(Shape)
     */
    @Override
    public void fill(Shape shape) {
        Path path = toSwtPath(shape);
        // Note that for consistency with the AWT implementation, it is
        // necessary to switch temporarily the foreground and background
        // colors
        if (this.gc.getForegroundPattern() == null) {
            switchColors();
        }
        if (shape instanceof Path2D) {
            Path2D p2d = (Path2D) shape;
            switch (p2d.getWindingRule()) {
                case Path2D.WIND_EVEN_ODD:
                    this.gc.setFillRule(SWT.FILL_EVEN_ODD);
                    break;
                case Path2D.WIND_NON_ZERO:
                    this.gc.setFillRule(SWT.FILL_WINDING);
                    break;
                default:
                    // not recognised
            }
        }
        this.gc.fillPath(path);
        if (this.gc.getForegroundPattern() == null) {
            switchColors();
        }
        path.dispose();
    }

    /**
     * Fill a rectangle area on the SWT graphic composite.
     * The {@code fillRectangle} method of the {@code GC}
     * class uses the background color so we must switch colors.
     * @see java.awt.Graphics#fillRect(int, int, int, int)
     */
    @Override
    public void fillRect(int x, int y, int width, int height) {
        this.switchColors();
        this.gc.fillRectangle(x, y, width, height);
        this.switchColors();
    }

    /**
     * Fills the specified rectangle with the current background color.
     *
     * @param x  the x-coordinate for the rectangle.
     * @param y  the y-coordinate for the rectangle.
     * @param width  the width.
     * @param height  the height.
     *
     * @see #fillRect(int, int, int, int)
     */
    @Override
    public void clearRect(int x, int y, int width, int height) {
        Color bgcolor = getBackground();
        if (bgcolor == null) {
            return;  // we can't do anything
        }
        Paint saved = getPaint();
        setPaint(bgcolor);
        fillRect(x, y, width, height);
        setPaint(saved);
    }

    /**
     * Fills the specified polygon.
     *
     * @param xPoints  the x-coordinates.
     * @param yPoints  the y-coordinates.
     * @param npoints  the number of points.
     */
    @Override
    public void fillPolygon(int[] xPoints, int[] yPoints, int npoints) {
        int[] pointArray = new int[npoints * 2];
        for (int i = 0; i < npoints; i++) {
            pointArray[2 * i] = xPoints[i];
            pointArray[2 * i + 1] = yPoints[i];
        }
        switchColors();
        this.gc.fillPolygon(pointArray);
        switchColors();
    }

    /**
     * Draws a rectangle with rounded corners that fits within the specified
     * framing rectangle.
     *
     * @param x  the x-coordinate.
     * @param y  the y-coordinate.
     * @param width  the frame width.
     * @param height  the frame height.
     * @param arcWidth  the width of the arc defining the roundedness of the
     *         rectangle's corners.
     * @param arcHeight the height of the arc defining the roundedness of the
     *         rectangle's corners.
     *
     * @see #drawRoundRect(int, int, int, int, int, int)
     */
    @Override
    public void fillRoundRect(int x, int y, int width, int height,
            int arcWidth, int arcHeight) {
        switchColors();
        this.gc.fillRoundRectangle(x, y, width - 1, height - 1, arcWidth,
                arcHeight);
        switchColors();
    }

    /**
     * Fills an oval that fits within the specified rectangular region.
     *
     * @param x  the x-coordinate.
     * @param y  the y-coordinate.
     * @param width  the frame width.
     * @param height  the frame height.
     *
     * @see #drawOval(int, int, int, int)
     * @see #fill(Shape)
     */
    @Override
    public void fillOval(int x, int y, int width, int height) {
        switchColors();
        this.gc.fillOval(x, y, width - 1, height - 1);
        switchColors();
    }

    /**
     * Fills an arc that is part of an ellipse that fits within the specified
     * framing rectangle.
     *
     * @param x  the x-coordinate.
     * @param y  the y-coordinate.
     * @param width  the frame width.
     * @param height  the frame height.
     * @param arcStart  the arc starting point, in degrees.
     * @param arcAngle  the extent of the arc.
     *
     * @see #drawArc(int, int, int, int, int, int)
     */
    @Override
    public void fillArc(int x, int y, int width, int height, int arcStart,
            int arcAngle) {
        switchColors();
        this.gc.fillArc(x, y, width - 1, height - 1, arcStart, arcAngle);
        switchColors();
    }

    /**
     * Returns the font in form of an AWT font created
     * with the parameters of the font of the SWT graphic
     * composite.
     * @return The font.
     * @see java.awt.Graphics#getFont()
     */
    @Override
    public Font getFont() {
        return this.awtFont;
    }

    /**
     * Set the font SWT graphic composite from the specified
     * AWT font. Be careful that the newly created SWT font
     * must be disposed separately.
     * @see java.awt.Graphics#setFont(java.awt.Font)
     */
    @Override
    public void setFont(Font font) {
        if (font == null) {
            return;
        }
        this.awtFont = font;
        org.eclipse.swt.graphics.Font swtFont = getSwtFontFromPool(font);
        this.gc.setFont(swtFont);
    }

    /**
     * Returns the font metrics.
     *
     * @param font the font.
     *
     * @return The font metrics.
     */
    @Override
    public FontMetrics getFontMetrics(Font font) {
        return SWTUtils.DUMMY_PANEL.getFontMetrics(font);
    }

    /**
     * Returns the font render context.
     *
     * @return The font render context.
     */
    @Override
    public FontRenderContext getFontRenderContext() {
        FontRenderContext fontRenderContext = new FontRenderContext(
                new AffineTransform(), true, true);
        return fontRenderContext;
    }

    /**
     * Draws the specified glyph vector at the location {@code (x, y)}.
     * 
     * @param g  the glyph vector ({@code null} not permitted).
     * @param x  the x-coordinate.
     * @param y  the y-coordinate.
     */
    @Override
    public void drawGlyphVector(GlyphVector g, float x, float y) {
        fill(g.getOutline(x, y));
    }

    /**
     * Draws a string at {@code (x, y)}.  The start of the text at the
     * baseline level will be aligned with the {@code (x, y)} point.
     * 
     * @param text  the string ({@code null} not permitted).
     * @param x  the x-coordinate.
     * @param y  the y-coordinate.
     * 
     * @see #drawString(java.lang.String, float, float) 
     */
    @Override
    public void drawString(String text, int x, int y) {
        drawString(text, (float) x, (float) y);
    }

    /**
     * Draws a string at the specified position.
     *
     * @param text  the string.
     * @param x  the x-coordinate.
     * @param y  the y-coordinate.
     */
    @Override
    public void drawString(String text, float x, float y) {
        if (text == null) {
            throw new NullPointerException("Null 'text' argument.");
        }
        float fm = this.gc.getFontMetrics().getAscent();
        this.gc.drawString(text, (int) x, (int) (y - fm), true);
    }

    /**
     * Draws a string at the specified position.
     *
     * @param iterator  the string.
     * @param x  the x-coordinate.
     * @param y  the y-coordinate.
     */
    @Override
    public void drawString(AttributedCharacterIterator iterator, int x, int y) {
        // for now we simply want to extract the chars from the iterator
        // and call an unstyled text renderer
        StringBuffer sb = new StringBuffer();
        int numChars = iterator.getEndIndex() - iterator.getBeginIndex();
        char c = iterator.first();
        for (int i = 0; i < numChars; i++) {
            sb.append(c);
            c = iterator.next();
        }
        drawString(new String(sb),x,y);
    }

    /**
     * Draws a string at the specified position.
     *
     * @param iterator  the string.
     * @param x  the x-coordinate.
     * @param y  the y-coordinate.
     */
    @Override
    public void drawString(AttributedCharacterIterator iterator, float x,
            float y) {
        drawString(iterator, (int) x, (int) y);
    }

    /**
     * Returns {@code true} if the rectangle (in device space) intersects
     * with the shape (the interior, if {@code onStroke} is false, 
     * otherwise the stroked outline of the shape).
     * 
     * @param rect  a rectangle (in device space).
     * @param s the shape.
     * @param onStroke  test the stroked outline only?
     * 
     * @return A boolean. 
     */
    @Override
    public boolean hit(Rectangle rect, Shape s, boolean onStroke) {
        AffineTransform transform = getTransform();
        Shape ts;
        if (onStroke) {
            Stroke stroke = getStroke();
            ts = transform.createTransformedShape(stroke.createStrokedShape(s));
        } else {
            ts = transform.createTransformedShape(s);
        }
        if (!rect.getBounds2D().intersects(ts.getBounds2D())) {
            return false;
        }
        Area a1 = new Area(rect);
        Area a2 = new Area(ts);
        a1.intersect(a2);
        return !a1.isEmpty();
    }

    /**
     * Not implemented - see {@link Graphics#copyArea(int, int, int, int, int,
     * int)}.
     */
    @Override
    public void copyArea(int x, int y, int width, int height, int dx, int dy) {
        // TODO Auto-generated method stub
    }

    /**
     * Draws an image with the specified transform. Note that the 
     * {@code observer} is ignored in this implementation.     
     * 
     * @param image  the image.
     * @param xform  the transform.
     * @param obs  the image observer (ignored).
     * 
     * @return {@code true} if the image is drawn. 
     */
    @Override
    public boolean drawImage(Image image, AffineTransform xform,
            ImageObserver obs) {
        AffineTransform savedTransform = getTransform();
        if (xform != null) {
            transform(xform);
        }
        boolean result = drawImage(image, 0, 0, obs);
        if (xform != null) {
            setTransform(savedTransform);
        }
        return result;
    }

    /**
     * Draws the image resulting from applying the {@code BufferedImageOp}
     * to the specified image at the location {@code (x, y)}.
     * 
     * @param image  the image.
     * @param op  the operation ({@code null} permitted).
     * @param x  the x-coordinate.
     * @param y  the y-coordinate.
     */
    @Override
    public void drawImage(BufferedImage image, BufferedImageOp op, int x,
            int y) {
        BufferedImage imageToDraw = image;
        if (op != null) {
            imageToDraw = op.filter(image, null);
        }
        drawImage(imageToDraw, new AffineTransform(1f, 0f, 0f, 1f, x, y), null);
    }

    /**
     * Draws an SWT image with the top left corner of the image aligned to the
     * point (x, y).
     *
     * @param image  the image.
     * @param x  the x-coordinate.
     * @param y  the y-coordinate.
     */
    public void drawImage(org.eclipse.swt.graphics.Image image, int x, int y) {
        this.gc.drawImage(image, x, y);
    }

    /**
     * Draws a rendered image. If {@code img} is {@code null} this method
     * does nothing.
     *
     * @param image  the rendered image ({@code null} permitted).
     * @param xform  the transform.
     */
    @Override
    public void drawRenderedImage(RenderedImage image, AffineTransform xform) {
        if (image == null) {
            return;
        }
        BufferedImage bi = convertRenderedImage(image);
        drawImage(bi, xform, null);
    }

    /**
     * Converts a rendered image to a {@code BufferedImage}.  This utility
     * method has come from a forum post by Jim Moore at:
     * <p>
     * <a href="http://www.jguru.com/faq/view.jsp?EID=114602">
     * http://www.jguru.com/faq/view.jsp?EID=114602</a>
     * 
     * @param img  the rendered image.
     * 
     * @return A buffered image. 
     */
    private static BufferedImage convertRenderedImage(RenderedImage img) {
        if (img instanceof BufferedImage) {
            return (BufferedImage) img;	
        }
        ColorModel cm = img.getColorModel();
        int width = img.getWidth();
        int height = img.getHeight();
        WritableRaster raster = cm.createCompatibleWritableRaster(width, height);
        boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
        Hashtable properties = new Hashtable();
        String[] keys = img.getPropertyNames();
        if (keys != null) {
            for (int i = 0; i < keys.length; i++) {
                properties.put(keys[i], img.getProperty(keys[i]));
            }
        }
        BufferedImage result = new BufferedImage(cm, raster, 
                isAlphaPremultiplied, properties);
        img.copyData(raster);
        return result;
    }

    /**
     * Draws the renderable image.
     * 
     * @param image  the renderable image.
     * @param xform  the transform.
     */
    @Override
    public void drawRenderableImage(RenderableImage image,
            AffineTransform xform) {
        RenderedImage ri = image.createDefaultRendering();
        drawRenderedImage(ri, xform);
    }

    /**
     * Draws an image with the top left corner aligned to the point (x, y).
     *
     * @param image  the image ({@code null} permitted...method will do nothing).
     * @param x  the x-coordinate.
     * @param y  the y-coordinate.
     * @param observer  ignored here.
     *
     * @return {@code true} if the image has been drawn.
     */
    @Override
    public boolean drawImage(Image image, int x, int y, 
            ImageObserver observer) {
        if (image == null) {
            return true;
        }
        int w = image.getWidth(observer);
        if (w < 0) {
            return false;
        }
        int h = image.getHeight(observer);
        if (h < 0) {
            return false;
        }
        return drawImage(image, x, y, w, h, observer);
    }

    /**
     * Draws an image with the top left corner aligned to the point (x, y),
     * and scaled to the specified width and height.
     *
     * @param image  the image ({@code null} permitted...draws nothing).
     * @param x  the x-coordinate.
     * @param y  the y-coordinate.
     * @param width  the width for the rendered image.
     * @param height  the height for the rendered image.
     * @param observer  ignored here.
     *
     * @return {@code true} if the image has been drawn.
     */
    @Override
    public boolean drawImage(Image image, int x, int y, int width, int height,
            ImageObserver observer) {
        if (image == null) {
            return true;
        }
        if (width <= 0 || height <= 0) {
            return true;
        }
        ImageData data = SWTUtils.convertAWTImageToSWT(image);
        if (data == null) {
            return false;
        }
        org.eclipse.swt.graphics.Image im = new org.eclipse.swt.graphics.Image(
                this.gc.getDevice(), data);
        org.eclipse.swt.graphics.Rectangle bounds = im.getBounds();
        this.gc.drawImage(im, 0, 0, bounds.width, bounds.height, x, y, width,
                height);
        im.dispose();
        return true;
    }

    /**
     * Draws an image.
     *
     * @param image  the image ({@code null} permitted...draws nothing).
     * @param x  the x-coordinate.
     * @param y  the y-coordinate.
     * @param bgcolor  the background color.
     * @param observer  an image observer.
     *
     * @return A boolean.
     */
    @Override
    public boolean drawImage(Image image, int x, int y, Color bgcolor,
            ImageObserver observer) {
        if (image == null) {
            return true;
        }
        int w = image.getWidth(null);
        if (w < 0) {
            return false;
        }
        int h = image.getHeight(null);
        if (h < 0) {
            return false;
        }
        return drawImage(image, x, y, w, h, bgcolor, observer);
    }

    /**
     * Draws an image to the rectangle {@code (x, y, w, h)} (scaling it if
     * required), first filling the background with the specified color.  Note 
     * that the {@code observer} is ignored.
     * 
     * @param image  the image.
     * @param x  the x-coordinate.
     * @param y  the y-coordinate.
     * @param width  the width.
     * @param height  the height.
     * @param bgcolor  the background color ({@code null} permitted).
     * @param observer  ignored.
     * 
     * @return {@code true} if the image is drawn.      
     */
    @Override
    public boolean drawImage(Image image, int x, int y, int width, int height,
            Color bgcolor, ImageObserver observer) {
        Paint saved = getPaint();
        setPaint(bgcolor);
        fillRect(x, y, width, height);
        setPaint(saved);
        return drawImage(image, x, y, width, height, observer);
    }

    /**
     * Draws part of an image (defined by the source rectangle 
     * {@code (sx1, sy1, sx2, sy2)}) into the destination rectangle
     * {@code (dx1, dy1, dx2, dy2)}.  Note that the {@code observer} 
     * is ignored.
     * 
     * @param image  the image.
     * @param dx1  the x-coordinate for the top left of the destination.
     * @param dy1  the y-coordinate for the top left of the destination.
     * @param dx2  the x-coordinate for the bottom right of the destination.
     * @param dy2  the y-coordinate for the bottom right of the destination.
     * @param sx1  the x-coordinate for the top left of the source.
     * @param sy1  the y-coordinate for the top left of the source.
     * @param sx2  the x-coordinate for the bottom right of the source.
     * @param sy2  the y-coordinate for the bottom right of the source.
     * 
     * @return {@code true} if the image is drawn. 
     */
    @Override
    public boolean drawImage(Image image, int dx1, int dy1, int dx2, int dy2,
            int sx1, int sy1, int sx2, int sy2, ImageObserver observer) {
        int w = dx2 - dx1;
        int h = dy2 - dy1;
        BufferedImage img2 = new BufferedImage(w, h, 
                BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img2.createGraphics();
        g2.drawImage(image, 0, 0, w, h, sx1, sy1, sx2, sy2, null);
        return drawImage(img2, dx1, dy1, null);
    }

    /**
     * Draws part of an image (defined by the source rectangle 
     * {@code (sx1, sy1, sx2, sy2)}) into the destination rectangle
     * {@code (dx1, dy1, dx2, dy2)}.  The destination rectangle is first
     * cleared by filling it with the specified {@code bgcolor}. Note that
     * the {@code observer} is ignored. 
     * 
     * @param image  the image.
     * @param dx1  the x-coordinate for the top left of the destination.
     * @param dy1  the y-coordinate for the top left of the destination.
     * @param dx2  the x-coordinate for the bottom right of the destination.
     * @param dy2  the y-coordinate for the bottom right of the destination.
     * @param sx1  the x-coordinate for the top left of the source.
     * @param sy1  the y-coordinate for the top left of the source.
     * @param sx2  the x-coordinate for the bottom right of the source.
     * @param sy2  the y-coordinate for the bottom right of the source.
     * @param bgcolor  the background color ({@code null} permitted).
     * @param observer  ignored.
     * 
     * @return {@code true} if the image is drawn. 
     */    
    public boolean drawImage(Image image, int dx1, int dy1, int dx2, int dy2,
            int sx1, int sy1, int sx2, int sy2, Color bgcolor,
            ImageObserver observer) {
        Paint saved = getPaint();
        setPaint(bgcolor);
        fillRect(dx1, dy1, dx2 - dx1, dy2 - dy1);
        setPaint(saved);
        return drawImage(image, dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2, 
                observer);
    }

    /**
     * Releases resources held by this instance (but note that the caller
     * must dispose of the 'GC' passed to the constructor).
     *
     * @see java.awt.Graphics#dispose()
     */
    @Override
    public void dispose() {
        // we dispose resources we own but user must dispose gc
        disposeResourcePool();
    }

    /**
     * Add given SWT resource to the resource pool. All resources added
     * to the resource pool will be disposed when {@link #dispose()} is called.
     *
     * @param resource the resource to add to the pool.
     * @return the SWT {@code Resource} just added.
     */
    private Resource addToResourcePool(Resource resource) {
        this.resourcePool.add(resource);
        return resource;
    }

    /**
     * Dispose the resource pool.
     */
    private void disposeResourcePool() {
        for (Iterator it = this.resourcePool.iterator(); it.hasNext();) {
            Resource resource = (Resource) it.next();
            resource.dispose();
        }
        this.fontsPool.clear();
        this.colorsPool.clear();
        this.patternsPool.clear();
        this.transformsPool.clear();
        this.resourcePool.clear();
    }

    /**
     * Internal method to convert a AWT font object into
     * a SWT font resource. If a corresponding SWT font
     * instance is already in the pool, it will be used
     * instead of creating a new one. This is used in
     * {@link #setFont(Font)} for instance.
     *
     * @param font The AWT font to convert.
     * @return The SWT font instance.
     */
    private org.eclipse.swt.graphics.Font getSwtFontFromPool(Font font) {
        org.eclipse.swt.graphics.Font swtFont = (org.eclipse.swt.graphics.Font)
        this.fontsPool.get(font);
        if (swtFont == null) {
            swtFont = new org.eclipse.swt.graphics.Font(this.gc.getDevice(),
                    SWTUtils.toSwtFontData(this.gc.getDevice(), font, true));
            addToResourcePool(swtFont);
            this.fontsPool.put(font, swtFont);
        }
        return swtFont;
    }

    /**
     * Internal method to convert a AWT color object into
     * a SWT color resource. If a corresponding SWT color
     * instance is already in the pool, it will be used
     * instead of creating a new one. This is used in
     * {@link #setColor(Color)} for instance.
     *
     * @param awtColor The AWT color to convert.
     * @return A SWT color instance.
     */
    private org.eclipse.swt.graphics.Color getSwtColorFromPool(Color awtColor) {
        org.eclipse.swt.graphics.Color swtColor =
                (org.eclipse.swt.graphics.Color)
                this.colorsPool.get(Integer.valueOf(awtColor.getRGB()));
        if (swtColor == null) {
            swtColor = SWTUtils.toSwtColor(this.gc.getDevice(), awtColor);
            addToResourcePool(swtColor);
            this.colorsPool.put(Integer.valueOf(awtColor.getRGB()), swtColor);
        }
        return swtColor;
    }

    /**
     * Fetches an SWT Pattern matching the supplied gradient paint, or creates one, and returns it.
     *
     * @param gp  the gradient paint.
     *
     * @return The SWT Pattern.
     */
    private Pattern fetchOrCreateSWTPattern(GradientPaint gp) {
        Pattern swtPattern = this.patternsPool.get(gp);
        if (swtPattern == null) {
            swtPattern = new Pattern(this.gc.getDevice(), (float) gp.getPoint1().getX(), (float) gp.getPoint1().getY(),
                    (float) gp.getPoint2().getX(), (float) gp.getPoint2().getY(), getSwtColorFromPool(gp.getColor1()),
                    getSwtColorFromPool(gp.getColor2()));
            addToResourcePool(swtPattern);
            this.patternsPool.put(gp, swtPattern);
        }
        return swtPattern;
    }

    /**
     * Internal method to convert a AWT transform object into
     * a SWT transform resource. If a corresponding SWT transform
     * instance is already in the pool, it will be used
     * instead of creating a new one. This is used in
     * {@link #setTransform(AffineTransform)} for instance.
     *
     * @param awtTransform The AWT transform to convert.
     * @return A SWT transform instance.
     */
    private Transform getSwtTransformFromPool(AffineTransform awtTransform) {
        Transform t = (Transform) this.transformsPool.get(awtTransform);
        if (t == null) {
            t = new Transform(this.gc.getDevice());
            double[] matrix = new double[6];
            awtTransform.getMatrix(matrix);
            t.setElements((float) matrix[0], (float) matrix[1],
                    (float) matrix[2], (float) matrix[3],
                    (float) matrix[4], (float) matrix[5]);
            addToResourcePool(t);
            this.transformsPool.put(awtTransform, t);
        }
        return t;
    }

    /**
     * Perform a switch between foreground and background
     * color of gc. This is needed for consistency with
     * the AWT behaviour, and is required notably for the
     * filling methods.
     */
    private void switchColors() {
        org.eclipse.swt.graphics.Color bg = this.gc.getBackground();
        org.eclipse.swt.graphics.Color fg = this.gc.getForeground();
        this.gc.setBackground(fg);
        this.gc.setForeground(bg);
    }

    /**
     * Converts an AWT {@code Shape} into a SWT {@code Path}.
     *
     * @param shape  the shape ({@code null} not permitted).
     *
     * @return The path.
     */
    private Path toSwtPath(Shape shape) {
        int type;
        float[] coords = new float[6];
        Path path = new Path(this.gc.getDevice());
        PathIterator pit = shape.getPathIterator(null);
        while (!pit.isDone()) {
            type = pit.currentSegment(coords);
            switch (type) {
                case (PathIterator.SEG_MOVETO):
                    path.moveTo(coords[0], coords[1]);
                    break;
                case (PathIterator.SEG_LINETO):
                    path.lineTo(coords[0], coords[1]);
                    break;
                case (PathIterator.SEG_QUADTO):
                    path.quadTo(coords[0], coords[1], coords[2], coords[3]);
                    break;
                case (PathIterator.SEG_CUBICTO):
                    path.cubicTo(coords[0], coords[1], coords[2],
                            coords[3], coords[4], coords[5]);
                    break;
                case (PathIterator.SEG_CLOSE):
                    path.close();
                    break;
                default:
                    break;
            }
            pit.next();
        }
        return path;
    }

    /**
     * Converts an SWT transform into the equivalent AWT transform.
     *
     * @param swtTransform  the SWT transform.
     *
     * @return The AWT transform.
     */
    private AffineTransform toAwtTransform(Transform swtTransform) {
        float[] elements = new float[6];
        swtTransform.getElements(elements);
        AffineTransform awtTransform = new AffineTransform(elements);
        return awtTransform;
    }

    /**
     * Returns the AWT line cap corresponding to the specified SWT line cap.
     *
     * @param swtLineCap  the SWT line cap.
     *
     * @return The AWT line cap.
     */
    private int toAwtLineCap(int swtLineCap) {
        if (swtLineCap == SWT.CAP_FLAT) {
            return BasicStroke.CAP_BUTT;
        }
        else if (swtLineCap == SWT.CAP_ROUND) {
            return BasicStroke.CAP_ROUND;
        }
        else if (swtLineCap == SWT.CAP_SQUARE) {
            return BasicStroke.CAP_SQUARE;
        }
        else {
            throw new IllegalArgumentException("SWT LineCap " + swtLineCap
                + " not recognised");
        }
    }

    /**
     * Returns the AWT line join corresponding to the specified SWT line join.
     *
     * @param swtLineJoin  the SWT line join.
     *
     * @return The AWT line join.
     */
    private int toAwtLineJoin(int swtLineJoin) {
        if (swtLineJoin == SWT.JOIN_BEVEL) {
            return BasicStroke.JOIN_BEVEL;
        }
        else if (swtLineJoin == SWT.JOIN_MITER) {
            return BasicStroke.JOIN_MITER;
        }
        else if (swtLineJoin == SWT.JOIN_ROUND) {
            return BasicStroke.JOIN_ROUND;
        }
        else {
            throw new IllegalArgumentException("SWT LineJoin " + swtLineJoin
                + " not recognised");
        }
    }

    /**
     * Returns the SWT line cap corresponding to the specified AWT line cap.
     *
     * @param awtLineCap  the AWT line cap.
     *
     * @return The SWT line cap.
     */
    private int toSwtLineCap(int awtLineCap) {
        if (awtLineCap == BasicStroke.CAP_BUTT) {
            return SWT.CAP_FLAT;
        }
        else if (awtLineCap == BasicStroke.CAP_ROUND) {
            return SWT.CAP_ROUND;
        }
        else if (awtLineCap == BasicStroke.CAP_SQUARE) {
            return SWT.CAP_SQUARE;
        }
        else {
            throw new IllegalArgumentException("AWT LineCap " + awtLineCap
                + " not recognised");
        }
    }

    /**
     * Returns the SWT line join corresponding to the specified AWT line join.
     *
     * @param awtLineJoin  the AWT line join.
     *
     * @return The SWT line join.
     */
    private int toSwtLineJoin(int awtLineJoin) {
        if (awtLineJoin == BasicStroke.JOIN_BEVEL) {
            return SWT.JOIN_BEVEL;
        }
        else if (awtLineJoin == BasicStroke.JOIN_MITER) {
            return SWT.JOIN_MITER;
        }
        else if (awtLineJoin == BasicStroke.JOIN_ROUND) {
            return SWT.JOIN_ROUND;
        }
        else {
            throw new IllegalArgumentException("AWT LineJoin " + awtLineJoin
                + " not recognised");
        }
    }

    /** A reusable rectangle to avoid garbage. */
    private Rectangle2D rect;

    /**
     * Sets the attributes of the reusable {@link Rectangle2D} object.
     *
     * @param x  the x-coordinate.
     * @param y  the y-coordinate.
     * @param width  the width.
     * @param height  the height.
     */
    private void setRect(int x, int y, int width, int height) {
        if (this.rect == null) {
            this.rect = new Rectangle2D.Double(x, y, width, height);
        } else {
            this.rect.setRect(x, y, width, height);
        }
    }

}

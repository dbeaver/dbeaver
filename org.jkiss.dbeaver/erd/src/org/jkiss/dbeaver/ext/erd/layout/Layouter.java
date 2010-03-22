// $Id: Layouter.java 106 2006-08-05 03:32:23Z bobtarling $
// Copyright (c) 2006 The Regents of the University of California. All
// Rights Reserved. Permission to use, copy, modify, and distribute this
// software and its documentation without fee, and without a written
// agreement is hereby granted, provided that the above copyright notice
// and this paragraph appear in all copies.  This software program and
// documentation are copyrighted by The Regents of the University of
// California. The software program and documentation are supplied "AS
// IS", without any accompanying services from The Regents. The Regents
// does not warrant that the operation of the program will be
// uninterrupted or error-free. The end-user understands that the program
// was developed for research purposes and is advised not to rely
// exclusively on the program for any reason.  IN NO EVENT SHALL THE
// UNIVERSITY OF CALIFORNIA BE LIABLE TO ANY PARTY FOR DIRECT, INDIRECT,
// SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES, INCLUDING LOST PROFITS,
// ARISING OUT OF THE USE OF THIS SOFTWARE AND ITS DOCUMENTATION, EVEN IF
// THE UNIVERSITY OF CALIFORNIA HAS BEEN ADVISED OF THE POSSIBILITY OF
// SUCH DAMAGE. THE UNIVERSITY OF CALIFORNIA SPECIFICALLY DISCLAIMS ANY
// WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
// MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE. THE SOFTWARE
// PROVIDED HEREUNDER IS ON AN "AS IS" BASIS, AND THE UNIVERSITY OF
// CALIFORNIA HAS NO OBLIGATIONS TO PROVIDE MAINTENANCE, SUPPORT,
// UPDATES, ENHANCEMENTS, OR MODIFICATIONS.

package org.jkiss.dbeaver.ext.erd.layout;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.List;

/**
 * Any layouter for any diagram type should implement this
 * interface.
 */
public interface Layouter {

    /**
     * Add another object to the diagram.
     *
     * @param obj represents the object to be part of the diagram.
     */
    public void add(LayouterObject obj);

    /**
     * Get all the layouted objects from this diagram.
     *
     * @return An array with the layouted objects of this Layouter.
     */
    public List getObjects();

    /**
     * This operation starts the actual layout process.
     */
    public void layout();

    /**
     * Get the total bounds of all objects in the layouter
     * @return the bounds of the layouter
     */
    public Rectangle getBounds();
    
    public void translate(int dx, int dy);
    
    public void setLocation(Point point);
}

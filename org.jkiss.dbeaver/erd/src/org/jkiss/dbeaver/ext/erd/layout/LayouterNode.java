// $Id: LayouterNode.java 76 2006-07-26 20:43:50Z bobtarling $
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

import java.awt.*;

/**
 * This interface has to be implemented by layouted nodes in
 * diagrams (i.e. classes or interfaces in a classdiagram).
 */
public interface LayouterNode extends LayouterObject {

    /**
     * Operation getSize returns the size of this node.
     *
     * @return The size of this node.
     */
    public Dimension getSize();

    /**
     * Operation getLocation returns the current location of
     * this node.
     *
     * @return The location of this node.
     */
    public Point getLocation();

    /**
     * Operation getBounds returns the current bounds of
     * this node.
     *
     * @return The location of this node.
     */
    public Rectangle getBounds();
    
    /**
     * Operation setLocation sets a new location for this
     * node.
     *
     * @param newLocation represents the new location for this node.
     */
    public void setLocation(Point newLocation);
}

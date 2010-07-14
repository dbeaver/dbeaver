// $Id: SimpleMethod.java 128 2006-08-29 13:59:33Z harrigan $
// Copyright (c) 2006 The Regents of the University of California. All
// Rights Reserved. Permission to use, copy, modify, and distribute this
// software and its documentation without fee, and without a written
// agreement is hereby granted, provided that the above copyright notice
// and this paragraph appear in all copies. This software program and
// documentation are copyrighted by The Regents of the University of
// California. The software program and documentation are supplied "AS
// IS", without any accompanying services from The Regents. The Regents
// does not warrant that the operation of the program will be
// uninterrupted or error-free. The end-user understands that the program
// was developed for research purposes and is advised not to rely
// exclusively on the program for any reason. IN NO EVENT SHALL THE
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

package org.jkiss.dbeaver.ext.erd.sugiyama.coordinate_assignment;

import java.awt.Point;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.dbeaver.ext.erd.sugiyama.SugiyamaGraph;
import org.jkiss.dbeaver.ext.erd.sugiyama.SugiyamaNode;

/**
 * This step assigns the final x-y coordinates to the nodes.
 *
 * @author harrigan
 */

public class SimpleMethod {
    
    /**
     * The logger.
     */
    private static final Log log = LogFactory.getLog(SimpleMethod.class);
    
    public SimpleMethod(SugiyamaGraph graph, int depth, int xGap, int yGap) {
            log.debug("Simple Coordinate Assignment");
            
            int y = yGap;
            
            for (int level = 1; level <= depth; level++) {
                int x = yGap;
                int yIncrement = 0;
                List<SugiyamaNode> nodes = graph.getLevel(level);
                for (SugiyamaNode node : nodes) {
                    if (node.isDummyNode()) {
                        log.debug("Routing edge along dummy node");
                        node.getEdge().addBend(new Point(x, y));
                        x += yGap;
                    } else {
                        if (node.getSize().height > yIncrement) {
                            yIncrement = node.getSize().height;
                        }
                        node.setLocation(new Point(x, y));
                        x += node.getSize().width + yGap; 
                    }
                }
                y += yIncrement + yGap;
            }
    }

}

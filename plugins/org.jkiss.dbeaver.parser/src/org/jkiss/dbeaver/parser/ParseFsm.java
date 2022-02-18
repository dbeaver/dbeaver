/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2022 DBeaver Corp and others
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
package org.jkiss.dbeaver.parser;

import java.util.List;

public class ParseFsm {
    private final List<ParseState> initialStates;
    private final List<ParseState> allStates;

    public ParseFsm(List<ParseState> initialStates, List<ParseState> parseFsmStates) {
        this.initialStates = initialStates;
        this.allStates = parseFsmStates;
    }

    public Iterable<ParseState> getInitialStates() {
        return initialStates;
    }

    public void prepare() {
        for (ParseState s : this.allStates) {
            s.prepare();
        }
    }

    /*
    public XmlGraph collectDebugView() {
        XmlGraph xg = new XmlGraph();
        Map<ParseState, XmlGraphNode> xnn = new HashMap<>();
        XmlGraphNode xend = xg.createNode();
        xend.setText("END");
        for (var s : allStates) {
            XmlGraphNode xn = xg.createNode();
            xn.setText("#" + s.getId());
            xnn.put(s, xn);
        }
        for (var s : allStates) {
            for (var t : s.getTransitions()) {
                XmlGraphNode xn = xg.createNode();
                xn.setText(t.pattern + "\n\n" + String.join("\n",
                        t.operations.stream().map(op -> op.toString()).collect(Collectors.toList())));
                xnn.get(s).connectTo(xn);
                XmlGraphNode xt = xnn.get(t.to);
                if (xt != null) {
                    xn.connectTo(xt);
                } else {
                    xn.connectTo(xend);
                }
            }
        } 
        xg.saveTo("d:\\temp\\out2.dgml");

        return xg;
    }*/
     
}

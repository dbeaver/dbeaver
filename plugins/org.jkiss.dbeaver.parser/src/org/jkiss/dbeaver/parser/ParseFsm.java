package org.jkiss.dbeaver.parser;

import java.util.List;

public class ParseFsm {
    private final List<ParseState> initialStates, allStates;

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

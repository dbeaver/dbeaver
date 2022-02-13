package org.jkiss.dbeaver.parser;

class ParseDispatchResult {
    private final int end;
    private final ParseStep step;

    public ParseDispatchResult(int end, ParseStep step) {
        this.end = end;
        this.step = step;
    }

    public int getEnd() {
        return end;
    }

    public ParseStep getStep() {
        return step;
    }
    
    
}

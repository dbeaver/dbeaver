package org.jkiss.dbeaver.erd.ui.internal.notation;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.erd.ui.notations.ERDNotationDescriptor;
import org.jkiss.dbeaver.erd.ui.notations.ERDNotationService;

public class ERDNotationServiceImpl implements ERDNotationService<ERDNotationDescriptor> {
    Log logger = Log.getLog(ERDNotationServiceImpl.class);
    private Map<String, ERDNotationDescriptor> notations = new LinkedHashMap<>();
    private ERDNotationDescriptor activeNotation;

    @Override
    public Collection<ERDNotationDescriptor> getERDNotations() {
        return notations.values();
    }

    @Override
    public void addNotation(ERDNotationDescriptor notation) {
        if (notations.containsKey(notation.getId())) {
            logger.error("ER Diagram Notation already defined for id:" + notation.getId());
        }
    }

    @Override
    public void setActiveNotation(ERDNotationDescriptor notation) {
        synchronized (this) {
            this.activeNotation = notation;
        }
    }

    @Override
    public ERDNotationDescriptor getActiveNotation() {
        synchronized (this) {
            if (activeNotation == null) {
                if (notations.isEmpty()) {
                    return null;
                }
                this.activeNotation = notations.values().iterator().next();
            } 
            return this.activeNotation;
        }
    }
}

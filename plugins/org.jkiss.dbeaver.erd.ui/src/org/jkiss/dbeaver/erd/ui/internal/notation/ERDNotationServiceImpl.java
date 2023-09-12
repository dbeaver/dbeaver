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
    private ERDNotationDescriptor defaultNotation;

    @Override
    public Collection<ERDNotationDescriptor> getERDNotations() {
        return notations.values();
    }

    @Override
    public void addNotation(ERDNotationDescriptor notation) {
        if (notations.containsKey(notation.getId())) {
            logger.error("ER Diagram Notation already defined for id:" + notation.getId());
        }
        if (notation.isDefault()) {
            if (defaultNotation == null) {
                defaultNotation = notation;
            } else {
                logger.error("The default ERD Notation already defined for id:" + defaultNotation.getId());
            }
        }
    }

    @Override
    public ERDNotationDescriptor getNotation(String id) {
        if (!notations.containsKey(id)) {
            logger.error("ERD Notation not defined for key:" + id);
            return null;
        }
        return notations.get(id);

    }

    @Override
    public ERDNotationDescriptor getDefaultNotation() {
        return this.defaultNotation;
    }
}

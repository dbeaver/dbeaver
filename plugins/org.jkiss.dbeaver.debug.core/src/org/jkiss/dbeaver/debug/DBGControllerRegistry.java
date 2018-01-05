package org.jkiss.dbeaver.debug;

public interface DBGControllerRegistry<C extends DBGController> {
    
    C createController(String dataTypeProviderId);

}

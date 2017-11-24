package org.jkiss.dbeaver.postgresql.debug;

public interface Variable<T> {
	
	T getVal();
	String getName();
	VariableType getType();

}

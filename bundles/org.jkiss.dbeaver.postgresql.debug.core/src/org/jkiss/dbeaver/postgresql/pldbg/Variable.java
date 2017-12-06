package org.jkiss.dbeaver.postgresql.pldbg;

public interface Variable<T> {
	
	T getVal();
	String getName();
	VariableType getType();

}

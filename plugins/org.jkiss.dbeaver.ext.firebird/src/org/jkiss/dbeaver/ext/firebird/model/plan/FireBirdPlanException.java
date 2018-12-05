package org.jkiss.dbeaver.ext.firebird.model.plan;

class FireBirdPlanException extends Exception {

	/**
	 * FireBirdPlanException
	 */
	private static final long serialVersionUID = 1L;

	public FireBirdPlanException(String expected, String actual, int position, String plan) {
		super(makeMessage(expected, actual, position, plan));
	}
	
	public FireBirdPlanException(String unexpected, int position, String plan) {
		super(makeMessage(unexpected, position, plan));
	}
	
	private static String makeMessage(String expected, String actual, int position, String plan)
	{
		return addPlanMark(
				String.format("Error parsing plan - expected %1$s at position %2$d but got %3$s", expected, position, actual), 
				position, plan);
	}
	
	private static String makeMessage(String unexpected, int position, String plan)
	{
		return addPlanMark(
				String.format("Error parsing plan - unexpected token %1$s at position %2$d", unexpected, position), 
				position, plan);
	}
	
	private static String addPlanMark(String message, int position, String plan)
	{
		StringBuilder sb = new StringBuilder(message);
		sb.append("\n");
		sb.append(plan.substring(0, position - 1));
		sb.append("^^^");
		sb.append(plan.substring(position));
        return sb.toString();
	}
    
}
package sas.data;

import java.util.ArrayList;
import java.util.List;

public class SASPlan
{	
	private SASState initial;
	private List<SASAction> actions;
	
	public SASPlan()
	{
		this.initial = new SASState();
		this.actions = new ArrayList<SASAction>();
	}
	
	public SASPlan(List<SASAction> actions)
	{
		this();
		this.actions = actions;
	}	
	
	public SASPlan(List<SASAction> actions, SASState init)
	{
		this(actions);
		this.initial = init;
	}
	
	@Override
	public String toString()
	{
		return this.actions.toString();
	}
	
	public void append(SASPlan plan)
	{
		this.append(plan.getActions());
	}
	
	public void append(List<SASAction> actions)
	{
		this.actions.addAll(actions);
	}
	
	public void append(SASAction action)
	{
		this.actions.add(action);
	}
	
	public void prepend(List<SASAction> actions)
	{
		this.actions.addAll(0, actions);
	}
	
	public void prepend(SASAction action)
	{
		this.actions.add(0, action);
	}
	
	public void prepend(SASPlan plan)
	{
		this.prepend(plan.getActions());
	}
	
	public int getPlanLength()
	{
		return this.actions.size();
	}
	
	/**
	 * Returns the cost which is the number of actions within this plan, or infinity if this plan is undefined.
	 * @return
	 */
	public double getTotalActionCost()
	{
		double actionCost = 0;
		for (SASAction a : this.actions)
			actionCost += a.getCost();
		
		return actionCost;
	}
	
//	public void setCost(double cost)
//	{
//		this.cost = cost;
//	}
	
	public void addAction(SASAction a)
	{
		this.actions.add(a);
	}
	
	public List<SASAction> getActions()
	{
		return this.actions;
	}

	public SASState getInitial()
	{
		return initial;
	}

	public void setInitial(SASState initial)
	{
		this.initial = initial;
	}
}

/* Instruction.java

	Purpose:
		
	Description:
		
	History:
		10:26 AM 2023/5/4, Created by jumperchen

Copyright (C) 2023 Potix Corporation. All Rights Reserved.
*/
package org.zkoss.differ;

import java.util.List;

import javax.annotation.Nullable;

import org.immutables.value.Value;

/**
 * The instrcution of diffing
 * @author jumperchen
 */
@Value.Immutable
@ImmutableStyle
public interface Instruction {

	/**
	 * The action of an instruction.
	 */
	public enum Action {
			addAttribute,
			modifyAttribute,
			removeAttribute,
			relocateGroup,
			removeElement,
			addElement,
			replaceElement,
	}

	/**
	 * Returns the action of the instruction.
	 */
	Action getAction();

	/**
	 * Returns the route of the updated component feature.
	 */
	List<Integer> getRoute();

	/**
	 * Returns the old value, if any.
	 */
	@Nullable
	Object getOldValue();

	/**
	 * Returns the new value, if any.
	 * @return
	 */
	@Nullable
	Object getNewValue();

	/**
	 * Returns the group length
	 */
	@Nullable
	Integer getGroupLength();

	/**
	 * Returns the index from which.
	 */
	@Nullable
	Integer getFrom();

	/**
	 * Returns the index to which.
	 */
	@Nullable
	Integer getTo();

	/**
	 * Returns the value of the property changed.
	 */
	@Nullable
	Object getValue();

	/**
	 * Returns the component feature of the instruction
	 */
	@Nullable
	ComponentFeature getElement();

	/**
	 * Returns the name of the property changed.
	 */
	@Nullable
	String getName();

	/**
	 * Constructs an instruction builder with the given action.
	 * @param action
	 * @return
	 */
	static Builder newBuilder(Action action) {
		return new Builder().setAction(action);
	}

	class Builder extends ImmutableInstruction.Builder {}
}

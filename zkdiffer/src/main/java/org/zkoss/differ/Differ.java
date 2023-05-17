/* Differ.java

	Purpose:
		
	Description:
		
	History:
		9:53 AM 2023/5/4, Created by jumperchen

Copyright (C) 2023 Potix Corporation. All Rights Reserved.
*/
package org.zkoss.differ;

import java.util.List;

import org.zkoss.zk.ui.Component;

/**
 * The differ util API to diff the differences between the souce compoent and the target
 * component. And this also provides {@link #patch(Component, List)} for these differences to apply.
 * @author jumperchen
 */
public class Differ {

	/**
	 * Diffs the differences of the whole tree between the source component and target component
	 * with the {@link DiffOptions#DEFAULT} option.
	 */
	public static List<Instruction> diff(Component source, Component target) {
		return diff(source, target, DiffOptions.DEFAULT);
	}

	/**
	 * Diffs the differences of the whole tree between the source component and target component.
	 */
	public static List<Instruction> diff(Component source, Component target, DiffOptions options) {
		return new DiffFinder(source, target, options).findOuter();
	}

	/**
	 * Diffs the differences of the whole tree between the source component and target component
	 * with the {@link DiffOptions#DEFAULT} option.
	 */
	public static List<Instruction> diff(ComponentFeature source, ComponentFeature target) {
		return diff(source, target, DiffOptions.DEFAULT);
	}

	/**
	 * Diffs the differences of the whole tree between the source component and target component.
	 */
	public static List<Instruction> diff(ComponentFeature source, ComponentFeature target, DiffOptions options) {
		return new DiffFinder(source, target, options).findOuter();
	}

	/**
	 * Diffs the differences of the subtree between the source component and target component
	 * with the {@link DiffOptions#DEFAULT} option. (Excluding both root components themselves)
	 */
	public static List<Instruction> diffInner(Component source, Component target) {
		return diffInner(source, target, DiffOptions.DEFAULT);
	}

	/**
	 * Diffs the differences of the subtree between the source component and target component.
	 * (Excluding both root components themselves)
	 */
	public static List<Instruction> diffInner(ComponentFeature source, ComponentFeature target, DiffOptions options) {
		return new DiffFinder(source, target, options).findInner();
	}

	/**
	 * Diffs the differences of the subtree between the source component and target component
	 * with the {@link DiffOptions#DEFAULT} option. (Excluding both root components themselves)
	 */
	public static List<Instruction> diffInner(ComponentFeature source, ComponentFeature target) {
		return diffInner(source, target, DiffOptions.DEFAULT);
	}

	/**
	 * Diffs the differences of the subtree between the source component and target component.
	 * (Excluding both root components themselves)
	 */
	public static List<Instruction> diffInner(Component source, Component target, DiffOptions options) {
		return new DiffFinder(source, target, options).findInner();
	}

	/**
	 * Patches the given differences into the source component.
	 * @return true if succeed.
	 */
	public static boolean patch(Component source, List<Instruction> diffs) {
		return Patcher.patch(source, diffs);
	}

	/**
	 * Merges the differences of the whole tree between the source component and
	 * the target compoonent with the {@link DiffOptions} options.
	 * @return true if succeed.
	 * @see #diff(Component, Component)
	 * @see #patch(Component, List)
	 */
	public static boolean merge(Component source, Component target) {
		return merge(source, target, DiffOptions.DEFAULT);
	}

	/**
	 * Merges the differences of the whole tree between the source component and
	 * the target compoonent.
	 * @return true if succeed.
	 * @see #diff(Component, Component)
	 * @see #patch(Component, List)
	 */
	public static boolean merge(Component source, Component target, DiffOptions options) {
		return patch(source, diff(source, target, options));
	}

	/**
	 * Merges the differences of the subtree between the source component and
	 * the target compoonent with the {@link DiffOptions} options. (Excluding both
	 * root components themselves)
	 * @return true if succeed.
	 * @see #diffInner(Component, Component)
	 * @see #patch(Component, List)
	 */
	public static boolean mergeInner(Component source, Component target) {
		return mergeInner(source, target, DiffOptions.DEFAULT);
	}


	/**
	 * Merges the differences of the subtree between the source component and
	 * the target compoonent. (Excluding both
	 * root components themselves)
	 * @return true if succeed.
	 * @see #diffInner(Component, Component)
	 * @see #patch(Component, List)
	 */
	public static boolean mergeInner(Component source, Component target, DiffOptions options) {
		return patch(source, diffInner(source, target, options));
	}
}

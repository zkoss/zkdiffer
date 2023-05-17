/* DiffOptions.java

	Purpose:
		
	Description:
		
	History:
		11:24 AM 2023/5/10, Created by jumperchen

Copyright (C) 2023 Potix Corporation. All Rights Reserved.
*/
package org.zkoss.differ;

import javax.annotation.Nullable;

import org.immutables.value.Value;

/**
 * The options for diffing
 * @author jumperchen
 */
@Value.Immutable
@ImmutableStyle
public interface DiffOptions {
	DiffOptions DEFAULT = new Builder().build();

	/**
	 * If the max child count is greater than 0, it only does a simplified form
	 * of diffing of contents so that the number of diffs
	 * cannot be higher than the number of child nodes.
	 * <p>Default: 50
	 */
	default int getMaxChildCount() {
		return 50;
	}

	/**
	 * Returns the source range to shrink the source component, if any.
	 */
	@Nullable
	DiffRange getSourceRange();

	/**
	 * Returns the target range to shrink the target component, if any.
	 */
	@Nullable
	DiffRange getTargetRange();

	/**
	 * Returns whether not to diff the root component.
	 * @return true to skip diffing the root component.
	 */
	default boolean isSkipRoot() {
		return false;
	}

	static DiffOptions ofSourceRange(DiffRange range) {
		return new Builder().setSourceRange(range).build();
	}

	static DiffOptions ofSourceRange(int start, int end) {
		return ofSourceRange(DiffRange.of(start, end));
	}

	static Builder newBuilder() {
		return new Builder();
	}

	class Builder extends ImmutableDiffOptions.Builder {}
}

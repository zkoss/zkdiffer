/* DiffRange.java

	Purpose:
		
	Description:
		
	History:
		11:29 AM 2023/5/10, Created by jumperchen

Copyright (C) 2023 Potix Corporation. All Rights Reserved.
*/
package org.zkoss.differ;

import org.immutables.value.Value;

/**
 * The range for diffing to shrink the source component.
 * @author jumperchen
 */
@Value.Immutable
@ImmutableStyle
public interface DiffRange {
	int getStart();
	int getEnd();
	static DiffRange of(int start, int end) {
		return new Builder().setStart(start).setEnd(end).build();
	}

	static Builder newBuilder() {
		return new Builder();
	}
	class Builder extends ImmutableDiffRange.Builder {}
}

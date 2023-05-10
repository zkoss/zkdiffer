/* ImmutableStyle.java

	Purpose:
		
	Description:
		
	History:
		12:15 PM 2023/5/10, Created by jumperchen

Copyright (C) 2023 Potix Corporation. All Rights Reserved.
*/
package org.zkoss.differ;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

import org.immutables.value.Value;

/**
 * An annotation of a customized immutable style.
 * @author jumperchen
 */
@Target(ElementType.TYPE)
@Value.Style(
		visibility = Value.Style.ImplementationVisibility.PACKAGE,
		defaultAsDefault = true,
		get = {"get*", "is*"},
		init = "set*",
		overshadowImplementation = true,
		defaults = @Value.Immutable())
public @interface ImmutableStyle {
}

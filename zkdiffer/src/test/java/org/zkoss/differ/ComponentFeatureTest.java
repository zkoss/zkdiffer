/* ComponentFeatureTest.java

	Purpose:
		
	Description:
		
	History:
		3:50 PM 2023/5/11, Created by jumperchen

Copyright (C) 2023 Potix Corporation. All Rights Reserved.
*/
package org.zkoss.differ;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Random;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import org.zkoss.zk.ui.AbstractComponent;
import org.zkoss.zul.A;
import org.zkoss.zul.Div;
import org.zkoss.zul.Hlayout;
import org.zkoss.zul.Popup;
import org.zkoss.zul.Span;
import org.zkoss.zul.Vlayout;
import org.zkoss.zul.Window;

/**
 * Test for component's features
 *
 * @author jumperchen
 */
public class ComponentFeatureTest extends ZKDifferTestBase {
	private static Class<? extends AbstractComponent>[] containers = new Class[] {
			Window.class, Div.class, Hlayout.class, Vlayout.class, Popup.class,
			Span.class, A.class, org.zkoss.zhtml.Div.class,
			org.zkoss.zhtml.Span.class, org.zkoss.zhtml.P.class,
			org.zkoss.zhtml.A.class};
	private static Random rand = new Random();

	@Test
	@RepeatedTest(4)
	public void testProperty() {
		int level = Math.max(4, rand.nextInt(10));
		AbstractComponent source = createNestedComponent(level);
		AbstractComponent target = createNestedComponent(level);
		int propertyLevel = rand.nextInt(4);
		updateProperty(target, propertyLevel, "sclass", "mydiv");
		Differ.merge(source, target);
		assertEquals(redraw(() -> source), redraw(() -> target));
		AbstractComponent childAtLevel = findChildAtLevel(source, propertyLevel);
		assertEquals("mydiv", childAtLevel.getPropertyAccess("sclass")
				.getValue(childAtLevel));
	}

	@Test
	@RepeatedTest(2)
	public void testAttribute() {
		int level = Math.max(4, rand.nextInt(10));
		AbstractComponent source = createNestedComponent(level);
		AbstractComponent target = createNestedComponent(level);
		updateAttribute(target, 4, "sclass", "mydiv");
		Differ.merge(source, target);
		assertEquals(redraw(() -> source), redraw(() -> target));
		AbstractComponent childAtLevel = findChildAtLevel(source, 4);
		assertEquals("mydiv", childAtLevel.getAttribute("sclass"));
	}

	@Test
	@RepeatedTest(2)
	public void testClientAttribute() {
		int level = Math.max(4, rand.nextInt(10));
		AbstractComponent source = createNestedComponent(level);
		AbstractComponent target = createNestedComponent(level);
		updateClientAttribute(target, 3, "sclass", "mydiv");
		Differ.merge(source, target);
		assertEquals(redraw(() -> source), redraw(() -> target));
		AbstractComponent childAtLevel = findChildAtLevel(source, 3);
		assertEquals("mydiv", childAtLevel.getClientAttribute("sclass"));
	}

	@Test
	@RepeatedTest(2)
	public void testWidgetAttribute() {
		int level = Math.max(4, rand.nextInt(10));
		AbstractComponent source = createNestedComponent(level);
		AbstractComponent target = createNestedComponent(level);
		updateWidgetAttribute(target, 2, "sclass", "mydiv");
		Differ.merge(source, target);
		assertEquals(redraw(() -> source), redraw(() -> target));
		AbstractComponent childAtLevel = findChildAtLevel(source, 2);
		assertEquals("mydiv", childAtLevel.getWidgetAttribute("sclass"));
	}

	@Test
	@RepeatedTest(2)
	public void testWidgetListener() {
		int level = Math.max(4, rand.nextInt(10));
		AbstractComponent source = createNestedComponent(level);
		AbstractComponent target = createNestedComponent(level);
		updateWidgetListener(target, 2, "sclass", "mydiv");
		Differ.merge(source, target);
		assertEquals(redraw(() -> source), redraw(() -> target));
		AbstractComponent childAtLevel = findChildAtLevel(source, 2);
		assertEquals("mydiv", childAtLevel.getWidgetListener("sclass"));
	}

	@Test
	@RepeatedTest(2)
	public void testWidgetOverride() {
		int level = Math.max(4, rand.nextInt(10));
		AbstractComponent source = createNestedComponent(level);
		AbstractComponent target = createNestedComponent(level);
		updateWidgetOverride(target, 2, "sclass", "mydiv");
		Differ.merge(source, target);
		assertEquals(redraw(() -> source), redraw(() -> target));
		AbstractComponent childAtLevel = findChildAtLevel(source, 2);
		assertEquals("mydiv", childAtLevel.getWidgetOverride("sclass"));
	}

	private static void updateProperty(AbstractComponent component, int level,
			String property, Object value) {
		AbstractComponent current = findChildAtLevel(component, level);
		current.getPropertyAccess(property).setValue(current, value);
	}

	private static void updateAttribute(AbstractComponent component, int level,
			String property, Object value) {
		AbstractComponent current = findChildAtLevel(component, level);
		current.setAttribute(property, value);
	}

	private static void updateClientAttribute(AbstractComponent component,
			int level, String property, String value) {
		AbstractComponent current = findChildAtLevel(component, level);
		current.setClientAttribute(property, value);
	}

	private static void updateWidgetAttribute(AbstractComponent component,
			int level, String property, String value) {
		AbstractComponent current = findChildAtLevel(component, level);
		current.setWidgetAttribute(property, value);
	}

	private static void updateWidgetOverride(AbstractComponent component,
			int level, String property, String value) {
		AbstractComponent current = findChildAtLevel(component, level);
		current.setWidgetOverride(property, value);
	}

	private static void updateWidgetListener(AbstractComponent component,
			int level, String property, String value) {
		AbstractComponent current = findChildAtLevel(component, level);
		current.setWidgetListener(property, value);
	}

	private static AbstractComponent findChildAtLevel(AbstractComponent parent,
			int level) {
		AbstractComponent current = parent;
		while (level-- > 1) {
			current = (AbstractComponent) current.getFirstChild();
		}
		return current;
	}

	private static AbstractComponent createNestedComponent(int level) {
		Div root = new Div();
		AbstractComponent parent = root;
		while (level-- > 1) {
			parent.appendChild(newRandomComponent());
			parent = (AbstractComponent) parent.getFirstChild();
		}
		return root;
	}

	private static AbstractComponent newRandomComponent() {
		try {
			return containers[rand.nextInt(containers.length)].getConstructor()
					.newInstance();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}

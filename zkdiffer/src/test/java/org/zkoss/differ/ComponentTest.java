/* ComponentTest.java

	Purpose:
		
	Description:
		
	History:
		12:22 PM 2023/5/11, Created by jumperchen

Copyright (C) 2023 Potix Corporation. All Rights Reserved.
*/
package org.zkoss.differ;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import org.zkoss.zk.ui.AbstractComponent;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Button;
import org.zkoss.zul.Div;
import org.zkoss.zul.Hlayout;
import org.zkoss.zul.Label;
import org.zkoss.zul.Window;

/**
 * Test component structure
 * @author jumperchen
 */
public class ComponentTest extends ZKDifferTestBase {

	@Test
	public void testSameTree() {
		AbstractComponent source = createComponent();
		AbstractComponent target = createComponent();
		source.setAttribute("test", "abc");
		assertEquals(redraw(() -> source), redraw(() -> target));
		List<Instruction> diff = Differ.diff(source, target);
		assertEquals(1, diff.size());
		assertEquals(diff.get(0).getAction(), Instruction.Action.removeAttribute);

		Differ.patch(source, diff);
		assertEquals(redraw(() -> source), redraw(() -> target));
	}

	@Test
	public void testWithZhtml() {
		AbstractComponent source = createComponent();
		AbstractComponent target = createComponent();
		target.appendChild(createZhtmlComponent());
		Differ.merge(source, target);
		assertEquals(redraw(() -> source), redraw(() -> target));
	}

	@Test
	public void testInsertBefore() {
		AbstractComponent source = createComponent();
		AbstractComponent target = createComponent();

		// insert to source, i.e. remove from source
		source.insertBefore(new Label("new One"), source.getFirstChild());
		Differ.merge(source, target);
		assertEquals(redraw(() -> source), redraw(() -> target));

		// insert to target, i.e. add to source
		target.insertBefore(new Label("new One"), target.getFirstChild());
		Differ.merge(source, target);
		assertEquals(redraw(() -> source), redraw(() -> target));

		// replace element
		source.getFirstChild().detach();
		source.insertBefore(new Button("new Button"), source.getFirstChild());
		Differ.merge(source, target);
		assertEquals(redraw(() -> source), redraw(() -> target));

		assertEquals(source.getFirstChild().getWidgetClass(), "zul.wgt.Label");
	}


	@Test
	public void testAppendChild() {
		AbstractComponent source = createComponent();
		AbstractComponent target = createComponent();

		// add to source, i.e. remove from source
		source.appendChild(new Label("new One"));
		Differ.merge(source, target);
		assertEquals(redraw(() -> source), redraw(() -> target));

		// add to target, i.e. add to source
		target.appendChild(new Label("new One"));
		Differ.merge(source, target);
		assertEquals(redraw(() -> source), redraw(() -> target));

		// replace element
		source.getLastChild().detach();
		source.appendChild(new Button("new Button"));
		Differ.merge(source, target);
		assertEquals(redraw(() -> source), redraw(() -> target));

		assertEquals(source.getLastChild().getWidgetClass(), "zul.wgt.Label");
	}

	@Test
	public void replaceAll() {
		Div source = new Div();
		Div target = new Div();
		source.appendChild(createComponent());
		target.appendChild(createZhtmlComponent());
		Differ.merge(source, target);
		assertEquals(redraw(() -> source), redraw(() -> target));
		assertEquals(source.getChildren().size(), 1);
	}

	@Test
	public void testInnerPatch() {
		AbstractComponent source = new Window();
		AbstractComponent target = new Hlayout();
		source.appendChild(createComponent());
		target.appendChild(createComponent());
		target.getFirstChild().setClientAttribute("Test", "Abc");
		Differ.mergeInner(source, target);
		assertNotEquals(redraw(() -> source), redraw(() -> target));
		assertEquals(redraw(() -> (AbstractComponent) source.getFirstChild()), redraw(() -> (AbstractComponent) target.getFirstChild()));
		assertEquals("Abc", source.getFirstChild().getClientAttribute("Test"));
	}

	private static AbstractComponent createComponent() {
		Div div = new Div();
		div.appendChild(new Label("Test 1"));
		div.appendChild(new Label("Test 2"));
		return div;
	}
	private static AbstractComponent createZhtmlComponent() {
		org.zkoss.zhtml.Button button = new org.zkoss.zhtml.Button();
		button.setValue("Click Me");
		button.addEventListener(Events.ON_CLICK, event -> {
			System.out.println(event.getName());
		});
		return button;
	}
}

/* Patcher.java

	Purpose:
		
	Description:
		
	History:
		3:08 PM 2023/5/4, Created by jumperchen

Copyright (C) 2023 Potix Corporation. All Rights Reserved.
*/
package org.zkoss.differ;

import static org.zkoss.differ.Instruction.*;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.zkoss.lang.Classes;
import org.zkoss.lang.Objects;
import org.zkoss.lang.reflect.Fields;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.sys.ComponentCtrl;
import org.zkoss.zk.ui.sys.PropertyAccess;

/**
 * The patcher to apply the differences into a ZK component tree.
 * @author jumperchen
 */
/*package*/ class Patcher {
	/*package*/ static boolean patch(Component source, List<Instruction> diffs) {
		return diffs.stream().allMatch(diff -> patchDiff(source, diff));
	}

	private static void setProperty(Component component, String propName, Object propValue) {
		if (propName.startsWith("$$")) return; // ignore event changed.
		PropertyAccess propertyAccess = ((ComponentCtrl) component).getPropertyAccess(
				propName);
		if (propertyAccess != null) {
			propertyAccess.setValue(component, propValue);
		} else {
			if (propName.startsWith("_")) {
				throw new RuntimeException("No such method for [" + propName + "]");
			}
			// try reflection
			try {
				Fields.set(component, propName, propValue, true);
			} catch (NoSuchMethodException e) {
				throw new RuntimeException(e);
			}
		}
	}

	private static Object getProperty(Component component, String propName)
			throws InvocationTargetException, NoSuchMethodException,
			InstantiationException, IllegalAccessException {
		if (propName.startsWith("$$")) return null;
		PropertyAccess propertyAccess = ((ComponentCtrl) component).getPropertyAccess(
				propName);
		if (propertyAccess != null) {
			return propertyAccess.getValue(component);
		} else {
			if (propName.startsWith("_")) {
				throw new RuntimeException("No such method for [" + propName + "]");
			}
			// try reflection
			return Fields.get(Classes.newInstance(component.getClass(), null),
					propName);
		}
	}

	private static boolean patchDiff(Component source, Instruction diff) {
		Action action = diff.getAction();
		List<Integer> route = diff.getRoute();
		Component node = null;
		if (action != Action.addElement) {
			node = getFromRoute(source, route);
		}
		Component newNode = null;
		Component reference = null;
		List<Component> nodeArray;

		switch (action) {
		case addAttribute:
			if (node == null) {
				return false;
			}
			node.setAttribute(diff.getName(), diff.getValue());
			break;
		case modifyAttribute:
			if (node == null) {
				return false;
			}
			node.setAttribute(diff.getName(), diff.getNewValue());
			break;
		case removeAttribute:
			if (node == null) {
				return false;
			}
			node.removeAttribute(diff.getName());
			break;
		case addProperty:
			if (node == null) {
				return false;
			}
			try {
				setProperty(node, diff.getName(), diff.getValue());
			} catch (Throwable t) {
				// rollback with replace element
				Component parent = node.getParent();
				Component newChild = ((ComponentFeature) diff.getElement()).toComponent();
				parent.insertBefore(newChild, node);
				node.detach();
			}
			break;
		case modifyProperty:
			if (node == null) {
				return false;
			}
			try {
				setProperty(node, diff.getName(), diff.getNewValue());
			} catch (Throwable t) {
				// rollback with replace element
				Component parent = node.getParent();
				Component newChild = ((ComponentFeature) diff.getElement()).toComponent();
				parent.insertBefore(newChild, node);
				node.detach();
			}
			break;
		case removeProperty:
			if (node == null) {
				return false;
			}
			try {
				String propName = diff.getName();
				// reset with the default value, if possible
				setProperty(node, propName, getProperty(node, propName));
			} catch (Throwable t) {
				// rollback with replace element
				Component parent = node.getParent();
				Component newChild = ((ComponentFeature) diff.getElement()).toComponent();
				parent.insertBefore(newChild, node);
				node.detach();
			}
			break;
		case addWidgetOverride:
			if (node == null) {
				return false;
			}
			node.setWidgetOverride(diff.getName(), (String) diff.getValue());
			break;
		case modifyWidgetOverride:
			if (node == null) {
				return false;
			}
			node.setWidgetOverride(diff.getName(), (String) diff.getNewValue());
			break;
		case removeWidgetOverride:
			if (node == null) {
				return false;
			}
			node.setWidgetOverride(diff.getName(), null);
			break;
		case addWidgetListener:
			if (node == null) {
				return false;
			}
			node.setWidgetListener(diff.getName(), (String) diff.getValue());
			break;
		case modifyWidgetListener:
			if (node == null) {
				return false;
			}
			node.setWidgetListener(diff.getName(), (String) diff.getNewValue());
			break;
		case removeWidgetListener:
			if (node == null) {
				return false;
			}
			node.setWidgetListener(diff.getName(), null);
			break;
		case addWidgetAttribute:
			if (node == null) {
				return false;
			}
			node.setWidgetAttribute(diff.getName(), (String) diff.getValue());
			break;
		case modifyWidgetAttribute:
			if (node == null) {
				return false;
			}
			node.setWidgetAttribute(diff.getName(), (String) diff.getNewValue());
			break;
		case removeWidgetAttribute:
			if (node == null) {
				return false;
			}
			node.setWidgetAttribute(diff.getName(), null);
			break;
		case addClientAttribute:
			if (node == null) {
				return false;
			}
			node.setClientAttribute(diff.getName(), (String) diff.getValue());
			break;
		case modifyClientAttribute:
			if (node == null) {
				return false;
			}
			node.setClientAttribute(diff.getName(), (String) diff.getNewValue());
			break;
		case removeClientAttribute:
			if (node == null) {
				return false;
			}
			node.setClientAttribute(diff.getName(), null);
			break;
		case replaceElement: {
			Component parent = node.getParent();
			Component newChild = ((ComponentFeature) diff.getNewValue()).toComponent();
			parent.insertBefore(newChild, node);
			node.detach();
			break;
		}
		case relocateGroup:
			int groupLength = diff.getGroupLength();
			int from = diff.getFrom();
			final Component target = node;
			nodeArray = IntStream.range(0, groupLength).mapToObj(ignore -> {
				Component child = target.getChildren().get(from);
				target.removeChild(child);
				return child;
			}).collect(Collectors.toList());

			for (int i = 0, j = nodeArray.size(); i < j; i++) {
				Component childNode = nodeArray.get(i);
				if (i == 0) {
					reference = node.getChildren().get(diff.getTo());
				}
				node.insertBefore(childNode, reference);
			}
		break;
		case removeElement:
			node.getParent().removeChild(node);
			break;
		case addElement: {
			ArrayDeque<Integer> parentRoute = new ArrayDeque<>(route);
            int c = parentRoute.removeLast();
			node = getFromRoute(source, new ArrayList<>(parentRoute));
			if (node == null) {
				return false;
			}
			List<Component> children = node.getChildren();
			node.insertBefore(diff.getElement().toComponent(),
					children.size() > c ? children.get(c) : null);
			break;
		}
		default:
			throw new RuntimeException("unknown action [" + action + "]");
		}
		return true;
	}

	private static Component getFromRoute(Component node, List<Integer> route) {
		ArrayDeque<Integer> arrayDeque = new ArrayDeque<>(route);
		while (!arrayDeque.isEmpty()) {
			int c = arrayDeque.pop();
			node = node.getChildren().get(c);
		}
		return node;
	}
}

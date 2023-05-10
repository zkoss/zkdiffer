/* Patcher.java

	Purpose:
		
	Description:
		
	History:
		3:08 PM 2023/5/4, Created by jumperchen

Copyright (C) 2023 Potix Corporation. All Rights Reserved.
*/
package org.zkoss.differ;

import static org.zkoss.differ.Instruction.*;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.zkoss.lang.Classes;
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
		PropertyAccess propertyAccess = ((ComponentCtrl) component).getPropertyAccess(
				propName);
		if (propertyAccess != null) {
			propertyAccess.setValue(component, propValue);
		} else {
			// try reflection
			try {
				Fields.set(component, propName, propValue, true);
			} catch (NoSuchMethodException e) {
				throw new RuntimeException(e);
			}
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
			setProperty(node, diff.getName(), diff.getValue());
			break;
		case modifyAttribute:
			if (node == null) {
				return false;
			}
			setProperty(node, diff.getName(), diff.getNewValue());
			break;
		case removeAttribute:
			if (node == null) {
				return false;
			}
			try {
				// reset with the default value, if possible
				setProperty(node, diff.getName(),
						Fields.get(Classes.newInstance(node.getClass(), null),
								diff.getName()));
			} catch (Exception e) {
				setProperty(node, diff.getName(), null);
			}
			break;
//		case Type.modifyTextElement:
//			if (!node || !(node instanceof Text)) {
//				return false
//			}
//			options.textDiff(
//					node,
//					node.data,
//					diff.getValue(Type.oldValue) as string,
//					diff.getNewValue() as string
//			)
//			if (node.parentNode instanceof HTMLTextAreaElement) {
//				node.parentNode.value = diff.getNewValue() as string
//			}
//			break
//		case Type.modifyValue:
//			if (!node || typeof node.value === "undefined") {
//			return false
//		}
//		node.value = diff.getNewValue()
//		break
//		case Type.modifyComment:
//			if (!node || !(node instanceof Comment)) {
//				return false
//			}
//			options.textDiff(
//					node,
//					node.data,
//					diff.getValue(Type.oldValue) as string,
//					diff.getNewValue() as string
//			)
//			break
//		case Type.modifyChecked:
//			if (!node || typeof node.checked === "undefined") {
//			return false
//		}
//		node.checked = diff.getNewValue()
//		break
//		case Type.modifySelected:
//			if (!node || typeof node.selected === "undefined") {
//			return false
//		}
//		node.selected = diff.getNewValue()
//		break
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
//		case Type.removeTextElement: {
//			if (!node || node.nodeType !== 3) {
//				return false
//			}
//            const parentNode = node.parentNode
//			parentNode.removeChild(node)
//			if (parentNode instanceof HTMLTextAreaElement) {
//				parentNode.value = ""
//			}
//			break
//		}
//		case Type.addTextElement: {
//            const parentRoute = route.slice()
//            const c: number = parentRoute.splice(parentRoute.length - 1, 1)[0]
//			newNode = options.document.createTextNode(
//					diff.getValue() as string
//			)
//			node = getFromRoute(tree, parentRoute)
//			if (!node.childNodes) {
//				return false
//			}
//			node.insertBefore(newNode, node.childNodes[c] || null)
//			if (node.parentNode instanceof HTMLTextAreaElement) {
//				node.parentNode.value = diff.getValue() as string
//			}
//			break
//		}
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

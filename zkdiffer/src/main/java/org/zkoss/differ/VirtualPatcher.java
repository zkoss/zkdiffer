/* Patcher.java

	Purpose:
		
	Description:
		
	History:
		10:48 AM 2023/5/4, Created by jumperchen

Copyright (C) 2023 Potix Corporation. All Rights Reserved.
*/
package org.zkoss.differ;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * A virtual patcher to apply the differences into the source component feature tree.
 * @author jumperchen
 */
/*package*/ class VirtualPatcher {
	/*package*/ static void patch(ComponentFeature source, List<Instruction> diffs, DiffOptions options, Map<String, List<DiffFinder.SubtreeInfo>> subtreeInfos) {
		diffs.forEach(diff -> patchDiff(source, diff, options, subtreeInfos));
	}
	private static class RouteInfo {
		ComponentFeature node;
		ComponentFeature parentNode;
		int nodeIndex;

		/*package*/ RouteInfo(ComponentFeature node, ComponentFeature parentNode,
				int nodeIndex) {
			this.node = node;
			this.parentNode = parentNode;
			this.nodeIndex = nodeIndex;
		}
	}
	private static RouteInfo getFromVirtualRoute(ComponentFeature tree, List<Integer> route) {
		ComponentFeature node = tree;
		ComponentFeature parentNode = null;
		int nodeIndex = 0;

		ArrayDeque<Integer> route0 = new ArrayDeque<>(route);
		while (route0.size() > 0) {
			nodeIndex = route0.pop();
			parentNode = node;
			List<ComponentFeature> children = node.getChildren();
			node = children.size() > nodeIndex ? children.get(nodeIndex) : null;
		}
		return new RouteInfo(
				node,
				parentNode,
				nodeIndex);
	}
	private static void patchDiff(ComponentFeature source, Instruction diff, DiffOptions options, Map<String, List<DiffFinder.SubtreeInfo>> subtreeInfos) {
		ComponentFeature node = null, parentNode = null;
		int[] nodeIndexArray = new int[] {0};
		Instruction.Action action = diff.getAction();
		if (action != Instruction.Action.addElement) {
			// For adding nodes, we calculate the route later on. It's different because it includes the position of the newly added item.
        RouteInfo routeInfo = getFromVirtualRoute(source, diff.getRoute());
			node = routeInfo.node;
			parentNode = routeInfo.parentNode;
			nodeIndexArray[0] = routeInfo.nodeIndex;
		}
		List<DiffFinder.SubtreeInfo> newSubsets = new ArrayList<>();


	ComponentFeature newNode;
	List<ComponentFeature> nodeArray;
	List<Integer> route;

		switch (diff.getAction()) {
		case addAttribute:

			assert node != null;
			node.setProperty(diff.getName(), diff.getValue());
			break;
		case modifyAttribute:
			assert node != null;
			node.setProperty(diff.getName(), diff.getNewValue());
			break;
		case removeAttribute:
			assert node != null;
			node.removeProperty(diff.getName());
			break;
//		case Instruction.Type.modifyTextElement:
//			node.data = diff.getNewValue()
//			if (parentNode.nodeName === "TEXTAREA") {
//				parentNode.value = diff.getNewValue()
//			}
//			break
//		case Instruction.Type.modifyValue:
//			node.value = diff.getNewValue()
//			break
//		case Instruction.Type.modifyComment:
//			node.data = diff.getNewValue()
//			break
//		case Instruction.Type.modifyChecked:
//			node.checked = diff.getNewValue()
//			break
//		case Instruction.Type.modifySelected:
//			node.selected = diff.getNewValue()
//			break
		case replaceElement:
			newNode = (ComponentFeature) diff.getNewValue();
			assert parentNode != null;
			parentNode.replaceChild(nodeIndexArray[0], newNode);
			break;
		case relocateGroup: {
			int from = diff.getFrom();
			int groupLength = diff.getGroupLength();
			assert node != null;
			nodeArray = node.getChildren().subList(from, from + groupLength);
			Collections.reverse(nodeArray);
			int to = diff.getTo();
			for (ComponentFeature movedNode : nodeArray) {
				node.addChild(to, movedNode);
			}
			List<DiffFinder.SubtreeInfo> subsets = subtreeInfos.get(node.getUuid());
			if (subsets != null) {
				subsets.forEach(map -> {
					if (from < to && map.oldIndex <= to && map.oldIndex > from) {
						map.oldIndex -= groupLength;
						int splitLength = map.oldIndex + map.length - to;
						if (splitLength > 0) {
							// new insertion splits map.
							newSubsets.add(new DiffFinder.SubtreeInfo(to + groupLength,
									map.newIndex + map.length - splitLength,
									splitLength));
							map.length -= splitLength;
						}
					} else if (from > to && map.oldIndex > to && map.oldIndex < from) {
						map.oldIndex += groupLength;
						int splitLength = map.oldIndex + map.length - to;
						if (splitLength > 0) {
							// new insertion splits map.
							newSubsets.add(new DiffFinder.SubtreeInfo(to + groupLength,
									map.newIndex + map.length - splitLength,
									splitLength));
							map.length -= splitLength;
						}
					} else if (map.oldIndex == from) {
						map.oldIndex = to;
					}
				});
			}

			break;
		}
		case removeElement:
			assert parentNode != null;
			parentNode.removeChild(nodeIndexArray[0]);
			if (subtreeInfos.containsKey(parentNode.getUuid())) {
				List<DiffFinder.SubtreeInfo> parentSubsets = subtreeInfos.get(parentNode.getUuid());
				parentSubsets.forEach(map -> {
					int nodeIndex = nodeIndexArray[0];
					if (map.oldIndex > nodeIndex) {
						map.oldIndex -= 1;
					} else if (map.oldIndex == nodeIndex) {
						map.delete = true;
					} else if (map.oldIndex + map.length > nodeIndex) {
						if (map.oldIndex + map.length - 1 == nodeIndex) {
							map.length--;
						} else {
							newSubsets.add(new DiffFinder.SubtreeInfo(
							map.newIndex + nodeIndex - map.oldIndex,
									nodeIndex,
								map.length - nodeIndex + map.oldIndex - 1
							));
							map.length = nodeIndex - map.oldIndex;
						}
					}
				});
			}
			node = parentNode;
			break;
		case addElement: {
			route = new ArrayList<>(diff.getRoute());
            int c = route.remove(route.size() - 1);
			node = getFromVirtualRoute(source, route).node;
			newNode = diff.getElement();

			if (c >= node.getChildren().size()) {
				node.appendChild(newNode);
			} else {
				node.addChild(c, newNode);
			}
			List<DiffFinder.SubtreeInfo> subsets = subtreeInfos.get(node.getUuid());
			if (subsets != null) {
				subsets.forEach(map -> {
					if (map.oldIndex >= c) {
						map.oldIndex += 1;
					} else if (map.oldIndex + map.length > c) {
                        int splitLength = map.oldIndex + map.length - c;
						newSubsets.add(new DiffFinder.SubtreeInfo(
								map.newIndex + map.length - splitLength,
								c + 1,
								splitLength
						));
						map.length -= splitLength;
					}
				});
			}
			break;
		}
//		case Instruction.Type.removeTextElement:
//			parentNode.childNodes.splice(nodeIndex, 1)
//			if (parentNode.nodeName === "TEXTAREA") {
//				delete parentNode.value
//			}
//			if (parentNode.subsets) {
//				parentNode.subsets.forEach((map: subsetType) => {
//					if (map.oldIndex > nodeIndex[0]) {
//						map.oldIndex -= 1
//					} else if (map.oldIndex === nodeIndex[0]) {
//						map.delete = true
//					} else if (
//							map.oldIndex < nodeIndex &&
//									map.oldIndex + map.length > nodeIndex
//					) {
//						if (map.oldIndex + map.length - 1 === nodeIndex[0]) {
//							map.length--
//						} else {
//							newSubsets.push({
//									newValue:
//							map.newIndex + nodeIndex - map.oldIndex,
//									oldValue: nodeIndex,
//									length:
//							map.length - nodeIndex + map.oldIndex - 1,
//                            })
//							map.length = nodeIndex - map.oldIndex
//						}
//					}
//				})
//			}
//			node = parentNode
//			break
//		case Instruction.Type.addTextElement: {
//			route = diff.getRoute().slice()
//            const c: number = route.splice(route.length - 1, 1)[0]
//			newNode = {}
//			newNode.nodeName = "#text"
//			newNode.data = diff.getValue()
//			node = getFromVirtualRoute(tree, route).node
//			if (!node.childNodes) {
//				node.childNodes = []
//			}
//
//			if (c >= node.childNodes.length) {
//				node.childNodes.push(newNode)
//			} else {
//				node.childNodes.splice(c, 0, newNode)
//			}
//			if (node.nodeName === "TEXTAREA") {
//				node.value = diff.getNewValue()
//			}
//			if (node.subsets) {
//				node.subsets.forEach((map: subsetType) => {
//					if (map.oldIndex >= c) {
//						map.oldIndex += 1
//					}
//					if (map.oldIndex < c && map.oldIndex + map.length > c) {
//                        const splitLength = map.oldIndex + map.length - c
//						newSubsets.push({
//								newValue: map.newIndex + map.length - splitLength,
//								oldValue: c + 1,
//								length: splitLength,
//                        })
//						map.length -= splitLength
//					}
//				})
//			}
//			break
//		}
		default:
			throw new RuntimeException("unknown action [" + diff.getAction());
		}

		if (node != null) {
			List<DiffFinder.SubtreeInfo> subsets = subtreeInfos.get(node.getUuid());
			if (subsets != null) {
				subtreeInfos.put(node.getUuid(), subsets.stream()
						.filter(map -> !map.delete && map.oldIndex != map.newIndex)
						.collect(Collectors.toList()));
				if (!newSubsets.isEmpty()) {
					subsets.addAll(newSubsets);
				}
			}
		}
	}
}

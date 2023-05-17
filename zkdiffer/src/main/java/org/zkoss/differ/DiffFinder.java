/* DiffFinder.java

	Purpose:
		
	Description:
		
	History:
		10:05 AM 2023/5/4, Created by jumperchen

Copyright (C) 2023 Potix Corporation. All Rights Reserved.
*/
package org.zkoss.differ;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.zkoss.lang.Objects;
import org.zkoss.util.Pair;
import org.zkoss.zk.ui.Component;

/**
 * The implementation refers to <a href="https://github.com/fiduswriter/diffDOM">DiffDOM</a>
 * @author jumperchen
 */
/*package*/ class DiffFinder {
	private ComponentFeature _source;
	private ComponentFeature _target;
	private DiffOptions _options;
	private boolean _foundAll;
	private Map<String, List<SubtreeInfo>> _subtreeInfosCache;


	/*package*/ DiffFinder(Component source, Component target, DiffOptions options) {
		_source = ComponentFeature.build(source, options);
		_target = ComponentFeature.build(target,
				new DiffOptions.Builder().from(options).setSourceRange(options.getTargetRange())
						.build()); // reset diff range for target
		_options = options;
	}

	/*package*/ DiffFinder(ComponentFeature source, ComponentFeature target, DiffOptions options) {
		_source = source;
		_target = target;
		_options = options;
	}
	/*package*/ List<Instruction> findOuter() {
		_subtreeInfosCache = new HashMap<>();
		try {
			if (_options.isSkipRoot()) {
				_options = new DiffOptions.Builder().from(_options).setSkipRoot(false).build();
			}
			return findDiffs(_source, _target);
		} finally {
			_subtreeInfosCache = null;
		}
	}

	/*package*/ List<Instruction> findInner() {
		_subtreeInfosCache = new HashMap<>();
		try {
			_source.outerDone = true;
			if (!_options.isSkipRoot()) {
				_options = new DiffOptions.Builder().from(_options).setSkipRoot(true).build();
			}
			return findDiffs(_source, _target);
		} finally {
			_subtreeInfosCache = null;
		}
	}

	/*package*/ static class SubtreeInfo {
		/*package*/ int oldIndex, newIndex, length;
		/*package*/ boolean delete;
		/*package*/ SubtreeInfo(int oldIndex, int newIndex, int length) {
			this.oldIndex = oldIndex;
			this.newIndex = newIndex;
			this.length = length;
		}
	}

	private List<Instruction> findDiffs(ComponentFeature source, ComponentFeature target) {
		List<Instruction> diffs;
		List<Instruction> result = new ArrayList<>();
		do {
			diffs = findNextDiff(source, target, new ArrayList<>());
			if (diffs.isEmpty()) {
				// Last check if the elements really are the same now.
				// If not, remove all info about being done and start over.
				// Sometimes a node can be marked as done, but the creation of subsequent diffs means that it has to be changed again.
				if (!isEqual(source, target)) {
					if (_foundAll) {
						throw new RuntimeException("Could not find remaining diffs!");
					} else {
						_foundAll = true;
						removeDone(source);
						diffs = this.findNextDiff(source, target, new ArrayList<>());
					}
				}
			}
			if (!diffs.isEmpty()) {
				_foundAll = false;
				result.addAll(diffs);
				VirtualPatcher.patch(source, diffs, _options, _subtreeInfosCache);
			}
		} while (diffs.size() > 0);

		return fixDiffRange(result);
	}

	private List<Instruction> fixDiffRange(List<Instruction> diffs) {
		if (diffs.isEmpty()) return diffs;
		DiffRange diffRange = _options.getSourceRange();
		if (diffRange == null || diffRange.getStart() == 0) {
			// no change
			return diffs;
		}
		return diffs.stream().map(instruction -> {
			List<Integer> route = instruction.getRoute();
			// ignore for root
			if (!route.isEmpty()) {
				Instruction.Builder newInstruction = new Instruction.Builder().from(instruction);
				List<Integer> newRoute = new ArrayList<>(route);
				newRoute.set(0, newRoute.get(0) + diffRange.getStart());
				return newInstruction.setRoute(newRoute).build();
			}
			return instruction;
		}).collect(Collectors.toList());
	}

	private void removeDone(ComponentFeature source) {
		source.outerDone = false;
		source.innerDone = false;
		source.getChildren().forEach(this::removeDone);
	}

	private List<Instruction> findNextDiff(ComponentFeature source, ComponentFeature target, List<Integer> route) {
		List<Instruction> diffs;

		// outer differences?
		if (!source.outerDone) {
			diffs = findOuterDiff(source, target, route);
			source.outerDone = true;
			if (diffs.size() > 0) {
				return diffs;
			}
		}
		// inner differences?
		if (!source.innerDone) {
			diffs = findInnerDiff(source, target, route);
			if (diffs.size() > 0) {
				return diffs;
			} else {
				source.innerDone = true;
			}
		}
		return Collections.emptyList();
	}
	private List<Instruction> findInnerDiff(ComponentFeature source,
			ComponentFeature target, List<Integer> route) {
		final List<ComponentFeature> sourceChildren = new ArrayList<>(source.getChildren());
		final List<ComponentFeature> targetChildren = new ArrayList<>(target.getChildren());
		int last = Math.max(sourceChildren.size(), targetChildren.size());
		int childrenLengthDifference = Math.abs(
				sourceChildren.size() - targetChildren.size());
		List<Instruction> diffs = new ArrayList<>();
		int index = 0;
		if (_options.getMaxChildCount() <= 0
				|| last < _options.getMaxChildCount()) {
			if (!sourceChildren.isEmpty() && !targetChildren.isEmpty()) {
				boolean cachedSubtrees = _subtreeInfosCache.containsKey(source.getUuid());
				List subtreeInfos = _subtreeInfosCache.computeIfAbsent(
						source.getUuid(),
						uuid -> createSubtreeInfos(sourceChildren, targetChildren));

				/* One or more groups have been identified among the childnodes of source
				 * and target.
				 */
				diffs = attemptGroupRelocation(source, target, subtreeInfos, route, cachedSubtrees);

				if (diffs.size() > 0) {
					return diffs;
				}
			}
		}
		/* 0 or 1 groups of similar child nodes have been found
		 * for source and target. 1 If there is 1, it could be a sign that the
		 * contents are the same. When the number of groups is below 2,
		 * source and target are made to have the same length and each of the
		 * pairs of child nodes are diffed.
		 */
		for (int i = 0; i < last; i += 1) {
			ComponentFeature e1 =
					sourceChildren.size() > i ? sourceChildren.get(i) : null;
			ComponentFeature e2 =
					targetChildren.size() > i ? targetChildren.get(i) : null;

			if (childrenLengthDifference > 0) {
				/* source and target have different amounts of childNodes. Add
				 * and remove as necessary to obtain the same length */
				if (e1 != null && e2 == null) {
					diffs.add(Instruction.newBuilder(Instruction.Action.removeElement)
									.setRoute(concat(route, index)).setElement(e1.clone()).build());
					index -= 1;
				} else if (e2 != null && e1 == null) {
					diffs.add(Instruction.newBuilder(
							Instruction.Action.addElement).setRoute(
							concat(route, index)).setElement(e2.clone()).build());
				}
			}
			/* We are now guaranteed that childNodes e1 and e2 exist,
			 * and that they can be diffed.
			 */
			/* Diffs in child nodes should not affect the parent node,
			 * so we let these diffs be submitted together with other
			 * diffs.
			 */

			if (e1 != null && e2 != null) {
				if (_options.getMaxChildCount() <= 0
						|| last < _options.getMaxChildCount()) {
					diffs.addAll(this.findNextDiff(e1, e2, concat(route, index)));
				} else if (!isEqual(e1, e2)) {
					if (sourceChildren.size() > targetChildren.size()) {
						diffs.add(Instruction.newBuilder(Instruction.Action.removeElement)
								.setElement(e1.clone())
								.setRoute(concat(route, index)).build());
						sourceChildren.remove(i);
						i -= 1;
						index -= 1;

						childrenLengthDifference -= 1;
					} else if (sourceChildren.size() < targetChildren.size()) {
						ComponentFeature cloneChild = e2.clone();
						diffs.add(Instruction.newBuilder(Instruction.Action.addElement)
								.setElement(cloneChild)
								.setRoute(concat(route, index)).build());
						sourceChildren.add(i, cloneChild);
						childrenLengthDifference -= 1;
					} else {
						diffs.add(Instruction.newBuilder(Instruction.Action.replaceElement)
								.setOldValue(e1.clone()).setNewValue(e2.clone())
								.setRoute(concat(route, index)).build());
					}
				}
			}
			index += 1;
		}
		source.innerDone = true;
		return diffs;
	}

	private List<Integer> concat(List<Integer> list, Integer newValue) {
		List<Integer> newList = new ArrayList<>(list);
		newList.add(newValue);
		return newList;
	}
	private List<Instruction> attemptGroupRelocation(ComponentFeature source,
			ComponentFeature target, List<SubtreeInfo> subtreeInfos, List<Integer> route, boolean cachedSubtrees) {
		Pair<Object[], Object[]> gapInfo = getGapInformation(source, target,
				subtreeInfos);

		List<ComponentFeature> sourceChildren = source.getChildren();
		List<ComponentFeature> targetChildren = target.getChildren();
		List<Object> gaps1 = new ArrayList<>(Arrays.asList(gapInfo.x));
		List<Object> gaps2 = new ArrayList<>(Arrays.asList(gapInfo.y));
		int shortest = Math.min(gaps1.size(), gaps2.size());
		List<Instruction> diffs = new ArrayList<>();
		for (int index2 = 0, index1 = 0; index2 < shortest; index1 += 1, index2 += 1) {
			if (cachedSubtrees && (
					Objects.equals(gaps1.get(index2), Boolean.TRUE) || Objects.equals(gaps2.get(index2),
							Boolean.TRUE))) {
				// pass
			} else if (Objects.equals(gaps1.get(index2), Boolean.TRUE)) {
				ComponentFeature c1 = sourceChildren.get(index1);
				diffs.add(Instruction.newBuilder(
						Instruction.Action.removeElement).setRoute(
						concat(route, index2)).setElement(c1.clone()).build());
				gaps1.remove(index2);
				shortest = Math.min(gaps1.size(), gaps2.size());
				index2 -= 1;
			} else if (Objects.equals(gaps2.get(index2), Boolean.TRUE)) {
				ComponentFeature c2 = targetChildren.get(index2);
				diffs.add(Instruction.newBuilder(
						Instruction.Action.addElement).setRoute(
						concat(route, index2)).setElement(c2.clone()).build());
				gaps1.add(index2, Boolean.TRUE);
				shortest = Math.min(gaps1.size(), gaps2.size());
				index1 -= 1;
			} else if (!Objects.equals(gaps1.get(index2), gaps2.get(index2))) {
				if (diffs.size() > 0) {
					return diffs;
				}
				// group relocation
				SubtreeInfo group = subtreeInfos.get((int) gaps1.get(index2));
				int toGroup = Math.min(group.newIndex, (sourceChildren.size() - group.length));
				if (toGroup != group.oldIndex) {
					boolean destinationDifferent = false;
					for (int j = 0; j < group.length; j++) {
						if (!roughlyEqual(sourceChildren.get(toGroup + j),
								sourceChildren.get(group.oldIndex + j), Collections.emptyMap(), false, false)) {
							destinationDifferent = true;
						}
					}
					if (destinationDifferent) {
						return Collections.singletonList(Instruction.newBuilder(
												Instruction.Action.relocateGroup)
										.setGroupLength(group.length)
										.setFrom(group.oldIndex).setTo(toGroup)
										.setRoute(route).build());
					}
				}
			}
		}
		return diffs;
	}

	private Pair<Object[], Object[]> getGapInformation(ComponentFeature source, ComponentFeature target, List<SubtreeInfo> subtreeInfos) {
		List<ComponentFeature> sourceChildren = source.getChildren();
		List<ComponentFeature> targetChildren = target.getChildren();
		int sourceSize = sourceChildren.size();
		int targetSize = targetChildren.size();
		Object[] gaps1 = new Object[sourceSize];
		Object[] gaps2 = new Object[targetSize];

		Arrays.fill(gaps1, true);
		Arrays.fill(gaps2, true);
		int group = 0;

		for (int i = 0, length = subtreeInfos.size(); i < length; i++) {
			SubtreeInfo info = subtreeInfos.get(i);
			int endOld = info.oldIndex + info.length;
			int endNew = info.newIndex + info.length;
			for (int j = info.oldIndex; j < endOld; j++) {
				gaps1[j] = group;
			}
			for (int j = info.newIndex; j < endNew; j++) {
				gaps2[j] = group;
			}
			group += 1;
		}

		return new Pair<>(gaps1, gaps2);
	}

	private List<SubtreeInfo> createSubtreeInfos(List<ComponentFeature> sourceChildren, List<ComponentFeature> targetChildren) {
		boolean[] marked1 = new boolean[sourceChildren.size()];
		boolean[] marked2 = new boolean[targetChildren.size()];
		SubtreeInfo subtreeInfo = null;
		List<SubtreeInfo> subtreeInfos = new ArrayList<>();

		do {
			subtreeInfo = findCommonSubsets(sourceChildren, targetChildren, marked1, marked2);
			if (subtreeInfo != null) {
				subtreeInfos.add(subtreeInfo);
				for (int i = 0; i < subtreeInfo.length; i++) {
					marked1[subtreeInfo.oldIndex + i] = true;
					marked2[subtreeInfo.newIndex + i] = true;
				}
			}
		} while (subtreeInfo != null);
		return subtreeInfos;
	}

	private boolean roughlyEqual(ComponentFeature c1, ComponentFeature c2, Map<String, Boolean> uniqueFeatures, boolean sameChildren, boolean recursive) {
		if (c1 == null || c2 == null) return false;
		if (!Objects.equals(c1.getWidgetName(), c2.getWidgetName())) {
			return false;
		}

		Object textValue1 = c1.getTextValue();
		Object textValue2 = c2.getTextValue();
		if (textValue1 != null || textValue2 != null) {
			return Objects.equals(textValue1, textValue2);
		}

		List<String> descriptors = c1.getDescriptors();
		if (uniqueFeatures.containsKey(descriptors)) return true;
		String cid1 = c1.getId();
		if (cid1 != null) {
			if (!Objects.equals(cid1, c2.getId())) {
				return false;
			} else {
				if (uniqueFeatures.containsKey(c1.getWidgetName() + "#" + cid1)) {
					return true;
				}
			}
		}
		String sclass = (String) c1.getProperties().get("sclass");
		if (sclass != null) {
			if (!Objects.equals(sclass, c2.getProperties().get("sclass"))) {
				return false;
			} else {
				if (uniqueFeatures.containsKey(c1.getWidgetName() + "." + (sclass.replace(" ", ".")))) {
					return true;
				}
			}
		}
		if (sameChildren) {
			return true;
		}

		List<ComponentFeature> sourceChildren = c1.getChildren();
		List<ComponentFeature> targetChildren = c2.getChildren();

		if (sourceChildren.size() != targetChildren.size()) {
			return false;
		}

		if (recursive) {
			Map<String, Boolean> childUniqueFeatures = uniqueInBoth(sourceChildren, targetChildren);
			Iterator<ComponentFeature> targetIter = targetChildren.iterator();
			return sourceChildren.stream().allMatch(comp -> roughlyEqual(comp,
					targetIter.hasNext() ? targetIter.next() : null,
					childUniqueFeatures, true, false));
		} else {
			Iterator<ComponentFeature> targetIter = targetChildren.iterator();
			return sourceChildren.stream().allMatch(comp -> comp.getClass() ==  (targetIter.hasNext() ? targetIter.next().getClass() : null));
		}
	}

	/**
	 * based on https://en.wikibooks.org/wiki/Algorithm_implementation/Strings/Longest_common_substring#JavaScript
	 */
	private SubtreeInfo findCommonSubsets(List<ComponentFeature> sourceChildren,
			List<ComponentFeature> targetChildren, boolean[] marked1, boolean[] marked2) {

		int lcsSize = 0;
		int[] index = new int[2];

		int sourceSize = sourceChildren.size();
		int targetSize = targetChildren.size();
		// set up the matching table
		int[][] matches = new int[sourceSize + 1][targetSize + 1];

		Map<String, Boolean> uniqueFeatures = uniqueInBoth(sourceChildren, targetChildren);

		AtomicBoolean subsetsSame = new AtomicBoolean(sourceSize == targetSize);

		if (subsetsSame.get()) {
			Iterator<ComponentFeature> targetIter = targetChildren.iterator();
			sourceChildren.stream().anyMatch(componentFeature -> {
				List<String> descriptors1 = componentFeature.getDescriptors();
				if (targetIter.hasNext()) {
					List<String> descriptors2 = targetIter.next().getDescriptors();
					if (descriptors1.size() != descriptors2.size()) {
						subsetsSame.set(false);
						return true;
					}
					for (int i = 0, j = descriptors1.size(); i < j; i++) {
						if (!Objects.equals(descriptors1.get(i), descriptors2.get(i))) {
							subsetsSame.set(false);
							break;
						}
					}
				}
				return !subsetsSame.get();
			});
		}

		for (int c1Index = 0; c1Index < sourceSize; ++c1Index) {
			ComponentFeature c1 = sourceChildren.get(c1Index);
			for (int c2Index = 0; c2Index < targetSize; ++c2Index) {
				ComponentFeature c2 = targetChildren.get(c2Index);
				// calculate cost/score
				if (!marked1[c1Index] && !marked2[c2Index] && roughlyEqual(c1, c2, uniqueFeatures, subsetsSame.get(), true)) {
					matches[c1Index + 1][c2Index + 1] = matches[c1Index][c2Index] + 1;
					if (matches[c1Index + 1][c2Index + 1] > lcsSize) {
						lcsSize = matches[c1Index + 1][c2Index + 1];
						index = new int[]{c1Index + 1, c2Index + 1};
					}
				} else {
					matches[c1Index + 1][c2Index + 1] = 0;
				}
			}
		}
		if (lcsSize == 0) return null;
		return new SubtreeInfo(index[0] - lcsSize, index[1] - lcsSize, lcsSize);
	}
	private Map<String, Boolean> uniqueInBoth(List<ComponentFeature> sourceChildren, List<ComponentFeature> targetChildren) {
		Map<String, Boolean> uniqueFeatures = findUniqueDescriptors(sourceChildren);
		uniqueFeatures.keySet().retainAll(findUniqueDescriptors(targetChildren).keySet());
		return uniqueFeatures;
	}

	private Map<String, Boolean> findUniqueDescriptors(List<ComponentFeature> sourceChildren) {
		Map<String, Boolean> uniqueFeatures = new HashMap<>();
		Map<String, Boolean> duplicatedFeatures = new HashMap<>();

		sourceChildren.forEach(componentFeature -> {
			componentFeature.getDescriptors().forEach(descriptor -> {
				if (uniqueFeatures.containsKey(descriptor)) {
					uniqueFeatures.remove(descriptor);
					duplicatedFeatures.put(descriptor, Boolean.TRUE);
				} else if (!duplicatedFeatures.containsKey(descriptor)) {
					uniqueFeatures.put(descriptor, Boolean.TRUE);
				}
			});
		});
		return uniqueFeatures;
	}

	private List<Instruction> findOuterDiff(ComponentFeature source,
			ComponentFeature target, List<Integer> route) {
		List<Instruction> diffs = new ArrayList<>();
		if (!Objects.equals(source.getWidgetName(), target.getWidgetName())) {
			if (route.isEmpty()) {
				throw new RuntimeException(
						"Top level nodes have to be of the same kind.");
			}
			return Collections.singletonList(Instruction.newBuilder(
							Instruction.Action.replaceElement)
					.setOldValue(source.clone()).setNewValue(target.clone())
					.setRoute(route).build());
		}
		if (!route.isEmpty() && _options.getMaxChildCount() < Math.abs(
				source.getChildren().size() - target.getChildren().size())) {
			return Collections.singletonList(Instruction.newBuilder(
							Instruction.Action.replaceElement)
					.setOldValue(source.clone()).setNewValue(target.clone())
					.setRoute(route).build());
		}

		// check properties.
		diffMap(diffs, route, target, source.getProperties(), target.getProperties(),
				Instruction.Action.addProperty,
				Instruction.Action.removeProperty,
				Instruction.Action.modifyProperty);

		// check widget overrides.
		diffMap(diffs, route, null, source.getWidgetOverrides(), target.getWidgetOverrides(),
				Instruction.Action.addWidgetOverride,
				Instruction.Action.removeWidgetOverride,
				Instruction.Action.modifyWidgetOverride);

		// check widget attributes.
		diffMap(diffs, route, null, source.getWidgetAttributes(), target.getWidgetAttributes(),
				Instruction.Action.addWidgetAttribute,
				Instruction.Action.removeWidgetAttribute,
				Instruction.Action.modifyWidgetAttribute);

		// check widget listeners.
		diffMap(diffs, route, null, source.getWidgetListeners(), target.getWidgetListeners(),
				Instruction.Action.addWidgetListener,
				Instruction.Action.removeWidgetListener,
				Instruction.Action.modifyWidgetListener);

		// check widget listeners.
		diffMap(diffs, route, null, source.getClientAttributes(), target.getClientAttributes(),
				Instruction.Action.addClientAttribute,
				Instruction.Action.removeClientAttribute,
				Instruction.Action.modifyClientAttribute);


		// check component attributes.
		diffMap(diffs, route, null, source.getAttributes(), target.getAttributes(),
				Instruction.Action.addAttribute,
				Instruction.Action.removeAttribute,
				Instruction.Action.modifyAttribute);
		return diffs;
	}

	private static void diffMap(List<Instruction> diffs, List<Integer> route, ComponentFeature targetElement,
			Map<String, ?> source, Map<String, ?> target,
			Instruction.Action addAction, Instruction.Action removeAction,
			Instruction.Action modifyAction) {

		if (source == target) return;
		if (source == null) {
			source = Collections.emptyMap();
		}
		if (target == null) {
			target = Collections.emptyMap();
		}
		if (target.isEmpty() && source.isEmpty()) return;

		Map<String, ?> cloneTarget = new HashMap<>(target); // clone it

		for (Map.Entry<String, ?> me1 : source.entrySet()) {
			if (!cloneTarget.containsKey(me1.getKey())) {
				diffs.add(Instruction.newBuilder(removeAction).setRoute(route)
						.setName(me1.getKey()).setValue(me1.getValue()).setElement(targetElement)
						.build());
			} else {
				Object m2Value = cloneTarget.remove(me1.getKey());
				if (!Objects.equals(me1.getValue(), m2Value)) {
					diffs.add(
							Instruction.newBuilder(modifyAction).setRoute(route)
									.setName(me1.getKey())
									.setOldValue(me1.getValue())
									.setNewValue(m2Value)
									.setElement(targetElement).build());
				}
			}
		}

		for (Map.Entry<String, ?> me2 : cloneTarget.entrySet()) {
			// add OP
			diffs.add(Instruction.newBuilder(addAction).setRoute(route)
					.setName(me2.getKey()).setValue(me2.getValue())
					.setElement(targetElement).build());
		}
	}

	private boolean isEqual(ComponentFeature c1, ComponentFeature c2) {
		if (!_options.isSkipRoot() || c1 != _source) {
			// ignore for skip root check
			if (!Objects.equals(c1.getWidgetName(), c2.getWidgetName())) {
				return false;
			}
			if (!c1.match(c2)) {
				return false;
			}
		}

		if (c1.getChildren().size() != c2.getChildren().size()) {
			return false;
		}
		Iterator<ComponentFeature> i1 = c1.getChildren().iterator();
		Iterator<ComponentFeature> i2 = c2.getChildren().iterator();
		while (i1.hasNext() && i2.hasNext()) {
			if (!isEqual(i1.next(), i2.next())) {
				return false;
			}
		}
		return true;
	}
}
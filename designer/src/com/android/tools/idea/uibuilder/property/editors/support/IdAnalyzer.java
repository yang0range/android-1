/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.tools.idea.uibuilder.property.editors.support;

import static com.android.SdkConstants.ANDROID_URI;
import static com.android.SdkConstants.ATTR_CHECKED_BUTTON;
import static com.android.SdkConstants.ATTR_CHECKED_CHIP;
import static com.android.SdkConstants.ATTR_LAYOUT_ABOVE;
import static com.android.SdkConstants.ATTR_LAYOUT_ALIGN_BASELINE;
import static com.android.SdkConstants.ATTR_LAYOUT_ALIGN_BOTTOM;
import static com.android.SdkConstants.ATTR_LAYOUT_ALIGN_END;
import static com.android.SdkConstants.ATTR_LAYOUT_ALIGN_LEFT;
import static com.android.SdkConstants.ATTR_LAYOUT_ALIGN_RIGHT;
import static com.android.SdkConstants.ATTR_LAYOUT_ALIGN_START;
import static com.android.SdkConstants.ATTR_LAYOUT_ALIGN_TOP;
import static com.android.SdkConstants.ATTR_LAYOUT_BASELINE_TO_BASELINE_OF;
import static com.android.SdkConstants.ATTR_LAYOUT_BELOW;
import static com.android.SdkConstants.ATTR_LAYOUT_BOTTOM_TO_BOTTOM_OF;
import static com.android.SdkConstants.ATTR_LAYOUT_BOTTOM_TO_TOP_OF;
import static com.android.SdkConstants.ATTR_LAYOUT_LEFT_TO_LEFT_OF;
import static com.android.SdkConstants.ATTR_LAYOUT_LEFT_TO_RIGHT_OF;
import static com.android.SdkConstants.ATTR_LAYOUT_RIGHT_TO_LEFT_OF;
import static com.android.SdkConstants.ATTR_LAYOUT_RIGHT_TO_RIGHT_OF;
import static com.android.SdkConstants.ATTR_LAYOUT_TOP_TO_BOTTOM_OF;
import static com.android.SdkConstants.ATTR_LAYOUT_TOP_TO_TOP_OF;
import static com.android.SdkConstants.ATTR_LAYOUT_TO_END_OF;
import static com.android.SdkConstants.ATTR_LAYOUT_TO_LEFT_OF;
import static com.android.SdkConstants.ATTR_LAYOUT_TO_RIGHT_OF;
import static com.android.SdkConstants.ATTR_LAYOUT_TO_START_OF;
import static com.android.SdkConstants.AUTO_URI;
import static com.android.SdkConstants.CHIP;
import static com.android.SdkConstants.RADIO_BUTTON;
import static com.android.ide.common.resources.ResourcesUtil.stripPrefixFromId;

import com.android.tools.idea.common.model.NlComponent;
import com.android.tools.idea.common.property.NlProperty;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import com.intellij.openapi.util.text.StringUtil;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;

public class IdAnalyzer {
  private final NlProperty myProperty;
  private final Set<String> myReservedIds;
  private final PropertyGroup myPropertyGroup;
  private final Set<String> myResult;

  public IdAnalyzer(@NotNull NlProperty property) {
    myProperty = property;
    myPropertyGroup = findPropertyGroup(property.getName());
    myReservedIds = new HashSet<>();
    myResult = new HashSet<>();
  }

  @NotNull
  public List<String> findIds() {
    myPropertyGroup.findIds(this);
    return myResult.stream().sorted().collect(Collectors.toList());
  }

  @NotNull
  private static PropertyGroup findPropertyGroup(@NotNull String attributeName) {
    for (PropertyGroup group : PROPERTY_GROUPS) {
      if (group.myAttributes.contains(attributeName)) {
        return group;
      }
    }
    return DEFAULT_GROUP;
  }

  private void findIdsAmongSiblings() {
    List<NlComponent> parents = myProperty.getComponents().stream()
      .map(NlComponent::getParent)
      .filter(Objects::nonNull)
      .distinct()
      .collect(Collectors.toList());
    if (parents.size() == 1) {
      NlComponent parent = parents.get(0);

      reserveAllSelectedIds();

      if (!myReservedIds.isEmpty()) {
        reserveAllIdsWithTransitiveReferencesToSelectedIds(parent);
      }

      parent.getChildren().stream()
        .map(NlComponent::getId)
        .filter(id -> !StringUtil.isEmpty(id))
        .filter(id -> !myReservedIds.contains(id))
        .forEach(myResult::add);
    }
  }

  // Avoid creating references to the component itself
  private void reserveAllSelectedIds() {
    myProperty.getComponents().stream()
      .map(NlComponent::getId)
      .filter(id -> !StringUtil.isEmpty(id))
      .forEach(myReservedIds::add);
  }

  // Avoid creating a circular list of references
  private void reserveAllIdsWithTransitiveReferencesToSelectedIds(@NotNull NlComponent parent) {
    Multimap<String, String> referenceMap = HashMultimap.create();
    for (NlComponent component : parent.getChildren()) {
      String id = component.getId();
      if (!StringUtil.isEmpty(id)) {
        for (String attribute : myPropertyGroup.myAttributes) {
          String attributeValue = component.getAttribute(myPropertyGroup.myNamespace, attribute);
          if (attributeValue != null) {
            referenceMap.put(stripPrefixFromId(attributeValue), id);
          }
        }
      }
    }

    Set<String> references = new HashSet<>(myReservedIds);
    while (!references.isEmpty()) {
      String reference = references.iterator().next();
      references.remove(reference);
      myReservedIds.add(reference);
      //noinspection SuspiciousMethodCalls
      referenceMap.get(reference).stream()
        .filter(id -> !myReservedIds.contains(id))
        .forEach(references::add);
    }
  }

  private void findChildIdsWithTagName(@NotNull String childTagName) {
    List<NlComponent> components = myProperty.getComponents();
    if (components.size() == 1) {
      //noinspection SuspiciousMethodCalls
      components.get(0).getChildren().stream()
        .filter(component -> component.getTagName().equals(childTagName))
        .map(NlComponent::getId)
        .filter(id -> !StringUtil.isEmpty(id))
        .forEach(myResult::add);
    }
  }

  private void findAllIds() {
    reserveAllSelectedIds();
    myProperty.getModel().flattenComponents()
      .map(NlComponent::getId)
      .filter(id -> !StringUtil.isEmpty(id))
      .filter(id -> !myReservedIds.contains(id))
      .forEach(myResult::add);
  }

  private static final PropertyGroup DEFAULT_GROUP = new PropertyGroup(
    ANDROID_URI,
    ImmutableList.of()
  );

  private static final PropertyGroup RELATIVE_LAYOUT_GROUP = new SiblingValuePropertyGroup(
    ANDROID_URI,
    ImmutableList.of(ATTR_LAYOUT_TO_RIGHT_OF, ATTR_LAYOUT_TO_LEFT_OF, ATTR_LAYOUT_ABOVE,
                     ATTR_LAYOUT_BELOW, ATTR_LAYOUT_ALIGN_BASELINE, ATTR_LAYOUT_ALIGN_LEFT, ATTR_LAYOUT_ALIGN_TOP,
                     ATTR_LAYOUT_ALIGN_RIGHT, ATTR_LAYOUT_ALIGN_BOTTOM, ATTR_LAYOUT_ALIGN_START, ATTR_LAYOUT_ALIGN_END,
                     ATTR_LAYOUT_TO_START_OF, ATTR_LAYOUT_TO_END_OF)
  );

  private static final PropertyGroup CONSTRAINT_LAYOUT_GROUP = new SiblingValuePropertyGroup(
    AUTO_URI,
    ImmutableList.of(ATTR_LAYOUT_LEFT_TO_LEFT_OF, ATTR_LAYOUT_LEFT_TO_RIGHT_OF, ATTR_LAYOUT_RIGHT_TO_LEFT_OF,
                     ATTR_LAYOUT_RIGHT_TO_RIGHT_OF, ATTR_LAYOUT_TOP_TO_TOP_OF, ATTR_LAYOUT_TOP_TO_BOTTOM_OF,
                     ATTR_LAYOUT_BOTTOM_TO_TOP_OF, ATTR_LAYOUT_BOTTOM_TO_BOTTOM_OF, ATTR_LAYOUT_BASELINE_TO_BASELINE_OF)
  );

  private static final PropertyGroup RADIO_GROUP = new ChildTagPropertyGroup(
    AUTO_URI,
    ImmutableList.of(ATTR_CHECKED_BUTTON),
    RADIO_BUTTON
  );

  private static final PropertyGroup CHIP_GROUP = new ChildTagPropertyGroup(
    AUTO_URI,
    ImmutableList.of(ATTR_CHECKED_CHIP),
    CHIP
  );

  private static final List<PropertyGroup> PROPERTY_GROUPS = ImmutableList.of(
    RELATIVE_LAYOUT_GROUP,
    CONSTRAINT_LAYOUT_GROUP,
    RADIO_GROUP,
    CHIP_GROUP);

  private static class PropertyGroup {
    private final String myNamespace;
    private final List<String> myAttributes;

    PropertyGroup(@NotNull String namespace, @NotNull List<String> attributes) {
      myNamespace = namespace;
      myAttributes = attributes;
    }

    protected void findIds(@NotNull IdAnalyzer analyzer) {
      analyzer.findAllIds();
    }
  }

  private static class SiblingValuePropertyGroup extends PropertyGroup {

    SiblingValuePropertyGroup(@NotNull String namespace, @NotNull List<String> attributes) {
      super(namespace, attributes);
    }

    @Override
    protected void findIds(@NotNull IdAnalyzer analyzer) {
      analyzer.findIdsAmongSiblings();
    }
  }

  private static class ChildTagPropertyGroup extends PropertyGroup {
    private final String myChildTagName;

    ChildTagPropertyGroup(@NotNull String namespace, @NotNull List<String> attributes, @NotNull String childTagName) {
      super(namespace, attributes);
      myChildTagName = childTagName;
    }

    @Override
    protected void findIds(@NotNull IdAnalyzer analyzer) {
      analyzer.findChildIdsWithTagName(myChildTagName);
    }
  }
}

/*
 * Copyright (C) 2018 The Android Open Source Project
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
package com.android.tools.idea.naveditor.property

import com.android.tools.idea.common.model.NlModel
import com.android.tools.idea.common.property.NlProperty
import com.android.tools.idea.common.property.inspector.InspectorComponent
import com.android.tools.idea.naveditor.NavModelBuilderUtil.navigation
import com.android.tools.idea.naveditor.NavTestCase
import com.android.tools.idea.naveditor.property.inspector.NavInspectorProviders
import com.android.tools.idea.naveditor.property.inspector.SimpleProperty
import com.android.tools.idea.res.ResourceNotificationManager
import com.android.tools.idea.uibuilder.property.NlPropertyItem
import com.google.common.collect.HashBasedTable
import com.intellij.util.ui.UIUtil
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.`when`
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

class NavPropertiesPanelTest : NavTestCase() {
  @Suppress("UNCHECKED_CAST")
  fun testRefresh() {
    val model = model("nav.xml") {
      navigation {
        fragment("f1")
      }
    }

    val navPropertiesManager = NavPropertiesManager(myFacet, model.surface, myRootDisposable)
    val inspectorProviders = mock(NavInspectorProviders::class.java)
    val inspector1 = mock(InspectorComponent::class.java) as InspectorComponent<NavPropertiesManager>
    val inspector2 = mock(InspectorComponent::class.java) as InspectorComponent<NavPropertiesManager>
    `when`(inspectorProviders.createInspectorComponents(any(), any(), any())).thenReturn(listOf(inspector1, inspector2))
    val selectedItems = listOf(model.find("f1")!!)
    navPropertiesManager.myProviders = inspectorProviders
    navPropertiesManager.propertiesPanel.setItems(selectedItems, HashBasedTable.create())

    model.resourcesChanged(setOf(ResourceNotificationManager.Reason.EDIT))

    // Allow com.intellij.util.ui.update.MergingUpdateQueue to pump events
    Thread.sleep(2L * NlModel.DELAY_AFTER_TYPING_MS)
    UIUtil.dispatchAllInvocationEvents()

    verify(inspector1).refresh()
    verify(inspector2).refresh()
  }

  @Suppress("UNCHECKED_CAST")
  fun testPropertiesWrapped() {
    val model = model("nav.xml") {
      navigation {
        fragment("f1")
      }
    }

    val navPropertiesManager = NavPropertiesManager(myFacet, model.surface, myRootDisposable)
    val inspectorProviders = mock(NavInspectorProviders::class.java)
    val selectedItems = listOf(model.find("f1")!!)
    navPropertiesManager.myProviders = inspectorProviders
    val property1 = mock(NlPropertyItem::class.java)
    doReturn("p1").`when`(property1).name
    val property2 = mock(NlPropertyItem::class.java)

    val properties = HashBasedTable.create<String, String, NlPropertyItem>()
    properties.put("", "p1", property1)
    properties.put("myns", "p2", property2)
    navPropertiesManager.propertiesPanel.setItems(selectedItems, properties)

    val propertiesCaptor = ArgumentCaptor.forClass(Map::class.java) as ArgumentCaptor<Map<String, NlProperty>>
    verify(inspectorProviders).createInspectorComponents(eq(selectedItems), propertiesCaptor.capture(), eq(navPropertiesManager))
    val wrappedProperties = propertiesCaptor.value
    // All properties either must be wrapped or be marker properties
    assertTrue(wrappedProperties.values.all { it is NavPropertyWrapper || it is SimpleProperty })

    // make sure the properties are connected correctly
    wrappedProperties["p1"]?.setValue("foo")
    verify(property1).setValue("foo")
  }
}
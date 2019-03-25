/*
 * Copyright (C) 2019 The Android Open Source Project
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
package com.android.tools.idea.uibuilder.handlers.constraint

import com.android.tools.adtui.common.AdtSecondaryPanel
import com.android.tools.adtui.common.ColoredIconGenerator
import com.android.tools.adtui.common.secondaryPanelBackground
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBList
import com.intellij.util.ui.JBDimension
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import icons.StudioIcons
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import java.util.Vector
import javax.swing.DefaultListCellRenderer
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.JPanel
import javax.swing.ListSelectionModel
import java.awt.Component
import java.awt.Font
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.SwingConstants

private const val PREFERRED_WIDTH = 280
private const val COMPONENT_HEIGHT = 20

class WidgetConstraintSection(private val widgetModel : WidgetConstraintModel) : AdtSecondaryPanel() {

  private val sectionTitle = SectionTitle()
  private val list: JList<ConstraintCellData>
  private val listData = Vector<ConstraintCellData>()
  private val warningPanel = WarningPanel()

  private var expanded: Boolean = false

  init {
    border = JBUI.Borders.empty(4, 4, 4, 4)
    layout = BorderLayout()

    list = JBList<ConstraintCellData>().apply { setEmptyText("") }
    list.selectionMode = ListSelectionModel.SINGLE_SELECTION
    list.cellRenderer = ConstraintItemRenderer()

    list.addKeyListener(object: KeyAdapter() {
      override fun keyReleased(e: KeyEvent) {
        if (e.keyCode == KeyEvent.VK_DELETE || e.keyCode == KeyEvent.VK_BACK_SPACE) {

          val index = list.selectedIndex
          val item = listData.removeAt(index)

          widgetModel.removeAttributes(item.namespace, item.attribute)
          e.consume()
        }
        else {
          super.keyReleased(e)
        }
      }
    })

    list.setListData(listData)
    add(sectionTitle, BorderLayout.NORTH)
    sectionTitle.addMouseListener(object : MouseAdapter() {
      override fun mouseClicked(e: MouseEvent) {
        setExpand(!expanded)
        e.consume()
      }
    })
    add(list, BorderLayout.CENTER)
    add(warningPanel, BorderLayout.SOUTH)

    // TODO: load project setting to see if it is collapse before.
    setExpand(expanded)
  }

  private fun setExpand(expanded: Boolean) {
    this.expanded = expanded
    sectionTitle.updateTitle()
    warningPanel.updateWarningMessage()

    warningPanel.isVisible = expanded
    list.isVisible = if (listData.size == 0) false else expanded

    sectionTitle.updateUI()
    list.updateUI()
    warningPanel.updateUI()

    invalidate()
  }

  override fun getPreferredSize(): Dimension {
    val titleSize = sectionTitle.preferredSize
    if (!expanded) {
      return JBDimension(PREFERRED_WIDTH, titleSize.height)
    }
    else {
      val listHeight = list.preferredSize.height + COMPONENT_HEIGHT / 2
      val warningTextHeight = warningPanel.preferredSize.height
      return JBDimension(PREFERRED_WIDTH, titleSize.height + listHeight + warningTextHeight)
    }
  }

  fun configureUi() {
    listData.clear()

    val component = widgetModel.component
    if (component != null) {
      for (item in CONSTRAINT_WIDGET_SECTION_ITEMS) {
        if (item.condition(component)) {
          val boldText = item.boldTextFunc(component)
          val fadingText = item.fadingTextFuc(component)
          listData.add(ConstraintCellData(item.namespace, item.attribute, item.displayName, boldText, fadingText))
        }
      }
    }
    list.visibleRowCount = listData.size

    setExpand(expanded)
    repaint()
  }

  private inner class SectionTitle: JPanel(BorderLayout()) {

    private val warningIcon = JLabel()

    private val icon = JLabel()

    init {
      preferredSize = JBUI.size(PREFERRED_WIDTH, COMPONENT_HEIGHT)
      border = JBUI.Borders.empty(4)
      background = secondaryPanelBackground

      icon.icon = if (expanded) UIUtil.getTreeExpandedIcon() else UIUtil.getTreeCollapsedIcon()
      icon.text = "Constraints"
      add(icon, BorderLayout.CENTER)

      add(warningIcon, BorderLayout.EAST)
      updateTitle()
    }

    fun updateTitle() {
      icon.icon = if (expanded) UIUtil.getTreeExpandedIcon() else UIUtil.getTreeCollapsedIcon()
      warningIcon.icon = if (expanded) {
         null
      }
      else {
        val hasWarning = widgetModel.isMissingHorizontalConstrained ||
                         widgetModel.isMissingVerticalConstrained ||
                         widgetModel.isOverConstrained
        if (hasWarning) StudioIcons.Common.WARNING else null
      }
    }
  }

  private inner class WarningPanel: JPanel(BorderLayout()) {

    private val horizontalWarning = JLabel()
    private val verticalWarning = JLabel()
    private val overConstrainedWarning = JLabel()

    init {
      background = secondaryPanelBackground

      horizontalWarning.icon = StudioIcons.Common.WARNING
      horizontalWarning.text = "Not Horizontally Constrained"
      horizontalWarning.border = JBUI.Borders.empty(2)

      verticalWarning.icon = StudioIcons.Common.WARNING
      verticalWarning.text = "Not Vertically Constrained"
      verticalWarning.border = JBUI.Borders.empty(2)

      overConstrainedWarning.icon = StudioIcons.Common.WARNING
      overConstrainedWarning.text = "Over Constrained"
      overConstrainedWarning.border = JBUI.Borders.empty(2)

      add(horizontalWarning, BorderLayout.NORTH)
      add(verticalWarning, BorderLayout.CENTER)
      add(overConstrainedWarning, BorderLayout.SOUTH)
    }

    fun updateWarningMessage() {
      horizontalWarning.isVisible = widgetModel.isMissingHorizontalConstrained
      verticalWarning.isVisible = widgetModel.isMissingVerticalConstrained
      overConstrainedWarning.isVisible = widgetModel.isOverConstrained

      isVisible = components.any { it.isVisible }
    }

    override fun getPreferredSize(): Dimension {
      val horizontal = if (horizontalWarning.isVisible) horizontalWarning.preferredSize.height else 0
      val vertical = if (verticalWarning.isVisible) verticalWarning.preferredSize.height else 0
      val over = if (overConstrainedWarning.isVisible) overConstrainedWarning.preferredSize.height else 0

      return JBDimension(PREFERRED_WIDTH, horizontal + vertical + over)
    }
  }
}

private class ConstraintCellData(val namespace: String,
                                 val attribute: String,
                                 val displayName: String,
                                 val boldValue: String?,
                                 val fadingValue: String?)

private val constraintIcon = StudioIcons.LayoutEditor.Palette.CONSTRAINT_LAYOUT
private val highlightConstraintIcon = ColoredIconGenerator.generateWhiteIcon(constraintIcon)

private val FADING_LABEL_COLOR = JBColor(Color.LIGHT_GRAY, Color.LIGHT_GRAY)

private class ConstraintItemRenderer : DefaultListCellRenderer() {
  private val panel = JPanel()
  private val iconLabel = JLabel(StudioIcons.LayoutEditor.Palette.CONSTRAINT_LAYOUT)
  private val nameLabel = JLabel()
  private val boldLabel = JLabel()
  private val fadingLabel = JLabel()

  init {
    horizontalAlignment = SwingConstants.LEADING

    preferredSize = JBDimension(PREFERRED_WIDTH, COMPONENT_HEIGHT)

    panel.layout = BorderLayout()
    panel.background = secondaryPanelBackground

    val centerPanel = JPanel(BorderLayout()).apply { isOpaque = true}

    iconLabel.border = JBUI.Borders.empty(2)
    iconLabel.isOpaque = true

    nameLabel.border = JBUI.Borders.empty(2)
    nameLabel.isOpaque = true

    boldLabel.border = JBUI.Borders.empty(2)
    boldLabel.isOpaque = true
    // font of value should be bold
    val valueFont = boldLabel.font
    boldLabel.font = valueFont.deriveFont(valueFont.style or Font.BOLD)

    fadingLabel.border = JBUI.Borders.empty(2)
    fadingLabel.foreground = Color.LIGHT_GRAY
    fadingLabel.isOpaque = true

    centerPanel.add(nameLabel, BorderLayout.WEST)

    val valuePanel = JPanel(BorderLayout())
    valuePanel.add(boldLabel, BorderLayout.WEST)
    valuePanel.add(fadingLabel, BorderLayout.CENTER)
    valuePanel.isOpaque = true

    centerPanel.add(valuePanel, BorderLayout.CENTER)

    panel.add(iconLabel, BorderLayout.WEST)
    panel.add(centerPanel, BorderLayout.CENTER)
  }

  override fun getListCellRendererComponent(list: JList<*>,
                                            value: Any?,
                                            index: Int,
                                            selected: Boolean,
                                            expanded: Boolean): Component {
    val item = value as ConstraintCellData
    nameLabel.text = item.displayName
    boldLabel.text = if (item.boldValue != null) item.boldValue.removePrefix("@+id/").removePrefix("@id/") else ""
    fadingLabel.text = if (item.fadingValue != null) "(${item.fadingValue})" else ""
    panel.toolTipText = "${item.displayName}=${item.boldValue}"

    if (selected) {
      iconLabel.icon = highlightConstraintIcon

      iconLabel.background = list.selectionBackground
      panel.background = list.selectionBackground
      nameLabel.background = list.selectionBackground
      boldLabel.background = list.selectionBackground
      fadingLabel.background = list.selectionBackground

      iconLabel.foreground = list.selectionForeground
      panel.foreground = list.selectionForeground
      nameLabel.foreground = list.selectionForeground
      boldLabel.foreground = list.selectionForeground
      fadingLabel.foreground = list.selectionForeground
    }
    else {
      iconLabel.icon = constraintIcon

      iconLabel.background = secondaryPanelBackground
      panel.background = secondaryPanelBackground
      nameLabel.background = secondaryPanelBackground
      boldLabel.background = secondaryPanelBackground
      fadingLabel.background = secondaryPanelBackground

      iconLabel.foreground = list.foreground
      panel.foreground = list.foreground
      nameLabel.foreground = list.foreground
      boldLabel.foreground = list.foreground
      fadingLabel.foreground = FADING_LABEL_COLOR
    }

    return panel
  }
}
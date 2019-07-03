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
package com.android.build.attribution

import com.intellij.build.BuildContentManager
import com.intellij.openapi.project.Project
import org.gradle.tooling.events.ProgressEvent

class BuildAttributionManagerImpl(
  private val myProject: Project,
  private val myBuildContentManager: BuildContentManager
) : BuildAttributionManager {

  override fun onBuildStart() {
    // TODO implement
  }

  override fun onBuildSuccess() {
    // TODO implement
  }

  override fun onBuildFailure() {
    // TODO implement
  }

  override fun statusChanged(event: ProgressEvent?) {
    // TODO implement
  }
}
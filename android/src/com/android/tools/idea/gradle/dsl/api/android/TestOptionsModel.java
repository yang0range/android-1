/*
 * Copyright (C) 2017 The Android Open Source Project
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
package com.android.tools.idea.gradle.dsl.api.android;

import com.android.tools.idea.gradle.dsl.api.android.testOptions.UnitTestsModel;
import com.android.tools.idea.gradle.dsl.api.values.GradleNullableValue;
import org.jetbrains.annotations.NotNull;

public interface TestOptionsModel {
  @NotNull
  GradleNullableValue<String> reportDir();

  @NotNull
  TestOptionsModel setReportDir(@NotNull String reportDir);

  @NotNull
  TestOptionsModel removeReportDir();

  @NotNull
  GradleNullableValue<String> resultsDir();

  @NotNull
  TestOptionsModel setResultsDir(@NotNull String resultsDir);

  @NotNull
  TestOptionsModel removeResultsDir();

  @NotNull
  UnitTestsModel unitTests();
}

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
package com.android.tools.idea.gradle.project.sync.setup.module.android;

import static com.android.SdkConstants.ANDROIDX_ANNOTATIONS_ARTIFACT;
import static com.android.SdkConstants.ANNOTATIONS_LIB_ARTIFACT;
import static com.android.SdkConstants.DOT_JAR;
import static com.android.SdkConstants.FD_RES;
import static com.android.SdkConstants.FN_ANNOTATIONS_ZIP;
import static com.intellij.openapi.roots.OrderRootType.CLASSES;
import static com.intellij.openapi.roots.OrderRootType.SOURCES;
import static java.io.File.separatorChar;

import com.android.tools.idea.gradle.LibraryFilePaths;
import com.android.tools.idea.gradle.project.sync.setup.module.common.ModuleDependenciesSetup;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.externalSystem.service.project.IdeModifiableModelsProvider;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.AnnotationOrderRootType;
import com.intellij.openapi.roots.DependencyScope;
import com.intellij.openapi.roots.JavadocOrderRootType;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.util.text.StringUtil;
import java.io.File;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.gradle.util.GradleConstants;

class AndroidModuleDependenciesSetup extends ModuleDependenciesSetup {
  private static final Logger LOG = Logger.getInstance(AndroidModuleDependenciesSetup.class);

  void setUpLibraryDependency(@NotNull Module module,
                              @NotNull IdeModifiableModelsProvider modelsProvider,
                              @NotNull String libraryName,
                              @NotNull DependencyScope scope,
                              @NotNull File artifactPath,
                              boolean exported) {
    setUpLibraryDependency(module, modelsProvider, libraryName, scope, artifactPath, new File[]{artifactPath}, exported);
  }

  void setUpLibraryDependency(@NotNull Module module,
                              @NotNull IdeModifiableModelsProvider modelsProvider,
                              @NotNull String libraryName,
                              @NotNull DependencyScope scope,
                              @NotNull File artifactPath,
                              @NotNull File[] binaryPaths,
                              boolean exported) {

    // let's use the same format for libraries imported from Gradle, to be compatible with API like ExternalSystemApiUtil.isExternalSystemLibrary()
    // and be able to reuse common cleanup service, see LibraryDataService.postProcess()
    String prefix = GradleConstants.SYSTEM_ID.getReadableName() + ": ";
    libraryName = libraryName.isEmpty() || StringUtil.startsWith(libraryName, prefix) ? libraryName : prefix + libraryName;

    LibraryFilePaths libraryFilePaths = LibraryFilePaths.getInstance(module.getProject());
    File sourceJarPath = libraryFilePaths.findSourceJarPath(libraryName, artifactPath);
    File javadocJarPath = libraryFilePaths.findJavadocJarPath(libraryName, artifactPath);
    boolean newLibrary = false;
    Library library = modelsProvider.getLibraryByName(libraryName);
    if (library == null || !isLibraryValid(modelsProvider.getModifiableLibraryModel(library), binaryPaths, javadocJarPath, sourceJarPath)) {
      if (library != null) {
        if (LOG.isDebugEnabled()) {
          LOG.debug(library.getName() + " not valid after sync.");
        }
        modelsProvider.removeLibrary(library);
      }
      library = modelsProvider.createLibrary(libraryName);
      newLibrary = true;
    }

    if (newLibrary) {
      updateLibraryRootTypePaths(library, CLASSES, modelsProvider, binaryPaths);

      // It is common that the same dependency is used by more than one module. Here we update the "sources" and "documentation" paths if they
      // were not set before.

      // Example:
      // In a multi-project project, there are 2 modules: 'app'' (an Android app) and 'util' (a Java lib.) Both of them depend on Guava. Since
      // Android artifacts do not support source attachments, the 'app' module may not indicate where to find the sources for Guava, but the
      // 'util' method can, since it is a plain Java module.
      // If the 'Guava' library was already defined when setting up 'app', it won't have source attachments. When setting up 'util' we may
      // have source attachments, but the library may have been already created. Here we just add the "source" paths if they were not already
      // set.
      if (sourceJarPath != null) {
        updateLibraryRootTypePaths(library, SOURCES, modelsProvider, sourceJarPath);
      }
      if (javadocJarPath != null) {
        updateLibraryRootTypePaths(library, JavadocOrderRootType.getInstance(), modelsProvider, javadocJarPath);
      }

      // Add external annotations.
      // TODO: Add this to the model instead!
      for (File binaryPath : binaryPaths) {
        String pathName = binaryPath.getPath();
        if (pathName.endsWith(FD_RES) && pathName.length() > FD_RES.length() &&
            pathName.charAt(pathName.length() - FD_RES.length() - 1) == separatorChar) {
          File annotations = new File(pathName.substring(0, pathName.length() - FD_RES.length()), FN_ANNOTATIONS_ZIP);
          if (annotations.isFile()) {
            updateLibraryRootTypePaths(library, AnnotationOrderRootType.getInstance(), modelsProvider, annotations);
          }
        }
        else if ((libraryName.startsWith(ANDROIDX_ANNOTATIONS_ARTIFACT, prefix.length()) ||
                  libraryName.startsWith(ANNOTATIONS_LIB_ARTIFACT, prefix.length())) &&
                 pathName.endsWith(DOT_JAR)) {
          // The support annotations is a Java library, not an Android library, so it's not distributed as an AAR
          // with its own external annotations. However, there are a few that we want to make available in the
          // IDE (for example, code completion on @VisibleForTesting(otherwise = |), so we bundle these in the
          // platform annotations zip file instead. We'll also need to add this as a root here.
          File annotations = new File(pathName.substring(0, pathName.length() - DOT_JAR.length()) + "-" + FN_ANNOTATIONS_ZIP);
          if (annotations.isFile()) {
            updateLibraryRootTypePaths(library, AnnotationOrderRootType.getInstance(), modelsProvider, annotations);
          }
        }
      }
    }

    addLibraryAsDependency(library, libraryName, scope, module, modelsProvider, exported);
  }
}

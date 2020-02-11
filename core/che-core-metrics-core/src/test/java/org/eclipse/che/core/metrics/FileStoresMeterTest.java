/*
 * Copyright (c) 2012-2018 Red Hat, Inc.
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Red Hat, Inc. - initial API and implementation
 */
package org.eclipse.che.core.metrics;

import static org.testng.Assert.assertEquals;

import com.google.common.collect.Lists;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.nio.file.FileStore;
import java.nio.file.FileSystems;
import java.util.ArrayList;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class FileStoresMeterTest {

  MeterRegistry registry;
  // Sometimes during test execution number of FileStore can change.
  // To avoid false test failures this data collected only once.
  private static final Iterable<FileStore> FILE_STORES = FileSystems.getDefault().getFileStores();

  @BeforeClass
  public void setup() {
    registry = new SimpleMeterRegistry();
    new FileStoresMeterBinder(FILE_STORES).bindTo(registry);
  }

  @Test(dataProvider = "fileStores")
  public void shouldBindFileStores(FileStore fileStore) {

    assertEquals(registry.get("disk.free").tag("path", fileStore.toString()).gauges().size(), 1);
    assertEquals(registry.get("disk.total").tag("path", fileStore.toString()).gauges().size(), 1);
    assertEquals(registry.get("disk.usable").tag("path", fileStore.toString()).gauges().size(), 1);
  }

  @DataProvider
  public Object[][] fileStores() {
    ArrayList<FileStore> fileStores = Lists.newArrayList(FILE_STORES);
    FileStore[][] result = new FileStore[fileStores.size()][1];
    for (int i = 0; i < fileStores.size(); i++) {
      result[i][0] = fileStores.get(i);
    }
    return result;
  }
}

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
package org.eclipse.che.api.metrics;

import static org.eclipse.che.api.metrics.WorkspaceBinders.workspaceMetric;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.MultiGauge;
import io.micrometer.core.instrument.MultiGauge.Row;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.binder.MeterBinder;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.workspace.server.WorkspaceManager;
import org.eclipse.che.commons.schedule.ScheduleDelay;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Provides metrics of workspace. */
@Singleton
public class WorkspaceMeterBinder implements MeterBinder {

  private static final Logger LOG = LoggerFactory.getLogger(WorkspaceMeterBinder.class);

  private final WorkspaceManager workspaceManager;
  private MultiGauge workspaceStacksGouge;

  @Inject
  public WorkspaceMeterBinder(WorkspaceManager workspaceManager) {
    this.workspaceManager = workspaceManager;
  }

  @Override
  public void bindTo(MeterRegistry registry) {
    Gauge.builder(workspaceMetric("total"), count(workspaceManager::getTotalCount))
        .description("Total number of workspaces")
        .register(registry);
    Gauge.builder(
            workspaceMetric("source.total"),
            count(() -> workspaceManager.getTotalCountWithAttribute("factoryurl")))
        .tag("source", "factory")
        .description("Total number of workspaces")
        .register(registry);
    workspaceStacksGouge = MultiGauge.builder(workspaceMetric("stacks.total")).register(registry);
  }

  @ScheduleDelay(initialDelay = 10, delay = 60)
  public void refresh() {
    try {

      List<Row<?>> rows =
          workspaceManager
              .getTotalCountWithAttributeGroupByValue("stackName")
              .entrySet()
              .stream()
              .map(e -> Row.of(Tags.of("stack", e.getKey()), e.getValue()))
              .collect(Collectors.toList());
      workspaceStacksGouge.register(rows, true);

    } catch (ServerException e) {
      LOG.warn("Fail to update statistics about stack usage. Reason {}", e.getMessage());
      workspaceStacksGouge.register(Collections.emptyList(), true);
    }
    //    Flux
    //            .fromIterable(asJavaCollection(adminClient.listAllGroupsFlattened()))
    //            .map(GroupOverview::groupId)
    //            .doOnNext(groupId -> log.info("Found groupId: {}", groupId))
    //            .flatMap(groupId -> Mono
    //                    .just(mapAsJavaMap(adminClient.listGroupOffsets(groupId)))
    //                    .flatMapIterable(Map::entrySet)
    //                    .map(entry -> toRow(groupId, entry)))
    //            .collectList()
    //            .doOnNext(rows -> gauge.register(rows, true))
    //            .then()
    //            .block();
  }

  private Supplier<Number> count(Callable<Long> callable) {
    return () -> {
      try {
        return Double.valueOf(callable.call());
      } catch (Exception e) {
        return Double.NaN;
      }
    };
  }
}

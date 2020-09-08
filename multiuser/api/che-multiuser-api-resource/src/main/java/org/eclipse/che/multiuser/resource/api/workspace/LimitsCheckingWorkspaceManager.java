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
package org.eclipse.che.multiuser.resource.api.workspace;

import com.google.common.annotations.VisibleForTesting;
import org.eclipse.che.account.api.AccountManager;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.core.notification.EventService;
import org.eclipse.che.api.user.server.PreferenceManager;
import org.eclipse.che.api.workspace.server.WorkspaceManager;
import org.eclipse.che.api.workspace.server.WorkspaceRuntimes;
import org.eclipse.che.api.workspace.server.WorkspaceValidator;
import org.eclipse.che.api.workspace.server.devfile.validator.DevfileIntegrityValidator;
import org.eclipse.che.api.workspace.server.spi.WorkspaceDao;
import org.eclipse.che.commons.lang.Size;
import org.eclipse.che.multiuser.resource.api.exception.NoEnoughResourcesException;
import org.eclipse.che.multiuser.resource.api.type.RuntimeResourceType;
import org.eclipse.che.multiuser.resource.api.type.WorkspaceResourceType;
import org.eclipse.che.multiuser.resource.api.usage.ResourceManager;
import org.eclipse.che.multiuser.resource.api.usage.ResourcesLocks;
import org.eclipse.che.multiuser.resource.api.usage.tracker.EnvironmentRamCalculator;
import org.eclipse.che.multiuser.resource.model.Resource;
import org.eclipse.che.multiuser.resource.spi.impl.ResourceImpl;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.List;
import java.util.Optional;

import static java.util.Collections.singletonList;

/**
 * Manager that checks limits and delegates all its operations to the {@link WorkspaceManager}.
 * Doesn't contain any logic related to start/stop or any kind of operations different from limits
 * checks.
 *
 * @author Yevhenii Voevodin
 * @author Igor Vinokur
 * @author Sergii Leschenko
 */
@Singleton
public class LimitsCheckingWorkspaceManager extends WorkspaceManager {

  private final EnvironmentRamCalculator environmentRamCalculator;
  private final ResourceManager resourceManager;
  private final ResourcesLocks resourcesLocks;
  private final AccountManager accountManager;

  private final long maxRamPerEnvMB;

  @Inject
  public LimitsCheckingWorkspaceManager(
      WorkspaceDao workspaceDao,
      WorkspaceRuntimes runtimes,
      EventService eventService,
      AccountManager accountManager,
      PreferenceManager preferenceManager,
      WorkspaceValidator workspaceValidator,
      // own injects
      @Named("che.limits.workspace.env.ram") String maxRamPerEnv,
      EnvironmentRamCalculator environmentRamCalculator,
      ResourceManager resourceManager,
      ResourcesLocks resourcesLocks,
      DevfileIntegrityValidator devfileIntegrityValidator) {
    super(
        workspaceDao,
        runtimes,
        eventService,
        accountManager,
        preferenceManager,
        workspaceValidator,
        devfileIntegrityValidator);
    this.environmentRamCalculator = environmentRamCalculator;
    this.maxRamPerEnvMB = "-1".equals(maxRamPerEnv) ? -1 : Size.parseSizeToMegabytes(maxRamPerEnv);
    this.resourceManager = resourceManager;
    this.resourcesLocks = resourcesLocks;
    this.accountManager = accountManager;
  }


  @VisibleForTesting
  void checkWorkspaceResourceAvailability(String accountId)
      throws NotFoundException, ServerException {
    try {
      resourceManager.checkResourcesAvailability(
          accountId,
          singletonList(new ResourceImpl(WorkspaceResourceType.ID, 1, WorkspaceResourceType.UNIT)));
    } catch (NoEnoughResourcesException e) {
      throw new LimitExceededException("You are not allowed to create more workspaces.");
    }
  }

  @VisibleForTesting
  void checkRuntimeResourceAvailability(String accountId)
      throws NotFoundException, ServerException {
    try {
      resourceManager.checkResourcesAvailability(
          accountId,
          singletonList(new ResourceImpl(RuntimeResourceType.ID, 1, RuntimeResourceType.UNIT)));
    } catch (NoEnoughResourcesException e) {
      throw new LimitExceededException("You are not allowed to start more workspaces.");
    }
  }

  /**
   * Returns resource with specified type from list or resource with specified default amount if
   * list doesn't contain it
   */
  private Resource getResourceOrDefault(
      List<? extends Resource> resources,
      String resourceType,
      long defaultAmount,
      String defaultUnit) {
    Optional<? extends Resource> resource = getResource(resources, resourceType);
    if (resource.isPresent()) {
      return resource.get();
    } else {
      return new ResourceImpl(resourceType, defaultAmount, defaultUnit);
    }
  }

  /** Returns resource with specified type from list */
  private Optional<? extends Resource> getResource(
      List<? extends Resource> resources, String resourceType) {
    return resources.stream().filter(r -> r.getType().equals(resourceType)).findAny();
  }

  private String printResourceInfo(Resource resource) {
    return resource.getAmount() + resource.getUnit().toUpperCase();
  }
}

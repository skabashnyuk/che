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
package org.eclipse.che.multiuser.permission.devfile.server.spi.jpa;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

import com.google.inject.persist.Transactional;
import java.util.List;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.persistence.EntityManager;
import org.eclipse.che.api.core.ConflictException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.Page;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.core.notification.EventService;
import org.eclipse.che.api.devfile.server.event.BeforeDevfileRemovedEvent;
import org.eclipse.che.api.devfile.server.model.impl.UserDevfileImpl;
import org.eclipse.che.api.devfile.server.spi.UserDevfileDao;
import org.eclipse.che.api.devfile.shared.event.DevfileDeletedEvent;
import org.eclipse.che.commons.lang.Pair;
import org.eclipse.che.core.db.jpa.DuplicateKeyException;

/** JPA based implementation of {@link UserDevfileDao}. */
@Singleton
public class MultiuserJpaUserDevfileDao implements UserDevfileDao {

  @Inject private EventService eventService;
  @Inject private Provider<EntityManager> managerProvider;

  private static final String findByWorkerQuery =
      "SELECT devfile FROM UserDevfilePermission permission  "
          + "          LEFT JOIN permission.userDevfile devfile "
          + "          WHERE worker.userId = :userId "
          + "          AND 'read' MEMBER OF permission.actions";
  private static final String findByWorkerCountQuery =
      "SELECT COUNT(devfile) FROM UserDevfilePermission permission  "
          + "          LEFT JOIN permission.userDevfile devfile "
          + "          WHERE permission.userId = :userId "
          + "          AND 'read' MEMBER OF permission.actions";

  @Override
  public UserDevfileImpl create(UserDevfileImpl devfile) throws ServerException, ConflictException {
    requireNonNull(devfile, "Required non-null devfile");
    try {
      doCreate(devfile);
    } catch (DuplicateKeyException dkEx) {
      throw new ConflictException(
          format(
              "Devfile with id '%s' or name '%s' already exists",
              devfile.getId(), devfile.getName()));
    } catch (RuntimeException x) {
      throw new ServerException(x.getMessage(), x);
    }
    return new UserDevfileImpl(devfile);
  }

  @Override
  public UserDevfileImpl update(UserDevfileImpl devfile)
      throws NotFoundException, ConflictException, ServerException {
    requireNonNull(devfile, "Required non-null update");
    try {
      return new UserDevfileImpl(doUpdate(devfile));
    } catch (DuplicateKeyException dkEx) {
      throw new ConflictException(
          format("Devfile with name '%s' already exists", devfile.getName()));
    } catch (RuntimeException x) {
      throw new ServerException(x.getMessage(), x);
    }
  }

  @Override
  public void remove(String id) throws ServerException {
    requireNonNull(id, "Required non-null id");
    Optional<UserDevfileImpl> workspaceOpt;
    try {
      workspaceOpt = doRemove(id);
      workspaceOpt.ifPresent(
          workspace -> eventService.publish(new DevfileDeletedEvent(workspace.getId())));
    } catch (RuntimeException x) {
      throw new ServerException(x.getLocalizedMessage(), x);
    }
  }

  @Override
  public UserDevfileImpl getById(String id) throws NotFoundException, ServerException {
    requireNonNull(id, "Required non-null id");
    try {
      final UserDevfileImpl userDevfile = managerProvider.get().find(UserDevfileImpl.class, id);
      if (userDevfile == null) {
        throw new NotFoundException(format("User devfile with id '%s' doesn't exist", id));
      }
      return new UserDevfileImpl(userDevfile);
    } catch (RuntimeException x) {
      throw new ServerException(x.getLocalizedMessage(), x);
    }
  }

  @Override
  public Page<UserDevfileImpl> getDevfiles(
      String userId,
      int maxItems,
      int skipCount,
      List<Pair<String, String>> filter,
      List<Pair<String, String>> order)
      throws ServerException {
    return null;
  }

  @Override
  public long getTotalCount() throws ServerException {
    try {
      return managerProvider
          .get()
          .createNamedQuery("UserDevfile.getTotalCount", Long.class)
          .getSingleResult();
    } catch (RuntimeException x) {
      throw new ServerException(x.getLocalizedMessage(), x);
    }
  }

  @Transactional
  protected void doCreate(UserDevfileImpl userDevfile) {
    EntityManager manager = managerProvider.get();
    manager.persist(userDevfile);
    manager.flush();
  }

  @Transactional(rollbackOn = {RuntimeException.class, ServerException.class})
  protected Optional<UserDevfileImpl> doRemove(String id) throws ServerException {
    final UserDevfileImpl userDevfile = managerProvider.get().find(UserDevfileImpl.class, id);
    if (userDevfile == null) {
      return Optional.empty();
    }
    final EntityManager manager = managerProvider.get();
    eventService
        .publish(new BeforeDevfileRemovedEvent(new UserDevfileImpl(userDevfile)))
        .propagateException();
    manager.remove(userDevfile);
    manager.flush();
    return Optional.of(userDevfile);
  }

  @Transactional
  protected UserDevfileImpl doUpdate(UserDevfileImpl userDevfile) throws NotFoundException {
    EntityManager manager = managerProvider.get();
    if (manager.find(UserDevfileImpl.class, userDevfile.getId()) == null) {
      throw new NotFoundException(
          format("User devfile with id '%s' doesn't exist", userDevfile.getId()));
    }

    UserDevfileImpl merged = manager.merge(userDevfile);
    manager.flush();
    return merged;
  }
}

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
package org.eclipse.che.api.devfile.server;

import static java.util.Objects.requireNonNull;

import com.google.common.annotations.Beta;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.eclipse.che.api.core.ConflictException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.Page;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.core.model.workspace.devfile.Devfile;
import org.eclipse.che.api.core.model.workspace.devfile.UserDevfile;
import org.eclipse.che.api.core.notification.EventService;
import org.eclipse.che.api.devfile.server.model.impl.UserDevfileImpl;
import org.eclipse.che.api.devfile.server.spi.UserDevfileDao;
import org.eclipse.che.api.devfile.shared.event.DevfileCreatedEvent;
import org.eclipse.che.api.devfile.shared.event.DevfileDeletedEvent;
import org.eclipse.che.api.devfile.shared.event.DevfileUpdatedEvent;
import org.eclipse.che.commons.env.EnvironmentContext;
import org.eclipse.che.commons.lang.NameGenerator;
import org.eclipse.che.commons.lang.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Beta
@Singleton
public class UserDevfileManager {
  private static final Logger LOG = LoggerFactory.getLogger(UserDevfileManager.class);
  private final UserDevfileDao userDevfileDao;
  private final EventService eventService;

  @Inject
  public UserDevfileManager(UserDevfileDao userDevfileDao, EventService eventService) {
    this.userDevfileDao = userDevfileDao;
    this.eventService = eventService;
  }

  /**
   * Stores {@link Devfile} instance
   *
   * @param devfile instance of devfile which would be stored
   * @return new persisted devfile instance
   * @throws ConflictException when any conflict occurs (e.g Devfile with such name already exists
   *     for {@code owner})
   * @throws ServerException when any other error occurs
   */
  public UserDevfileImpl createDevfile(Devfile devfile)
      throws ServerException, NotFoundException, ConflictException {
    requireNonNull(devfile, "Required non-null devfile");
    UserDevfileImpl result =
        userDevfileDao.create(
            new UserDevfileImpl(NameGenerator.generate("userdevfile", 16), devfile));
    LOG.debug(
        "UserDevfile '{}' with id '{}' created by user '{}'",
        result.getName(),
        result.getId(),
        EnvironmentContext.getCurrent().getSubject().getUserName());
    eventService.publish(new DevfileCreatedEvent(result));
    return result;
  }

  /**
   * Gets UserDevfile by given id.
   *
   * @param id userdevfile identifier
   * @return userdevfile instance
   * @throws NullPointerException when {@code id} is null
   * @throws NotFoundException when userdevfile with given id not found
   * @throws ServerException when any server errors occurs
   */
  public UserDevfile getById(String id) throws NotFoundException, ServerException {
    requireNonNull(id);
    return userDevfileDao.getById(id);
  }

  /**
   * Updates an existing user devfile in accordance to the new configuration.
   *
   * <p>Note: Replace strategy is used for user devfile update, it means that existing devfile data
   * will be replaced with given {@code update}.
   *
   * @param update user devfile update
   * @return updated user devfile
   * @throws NullPointerException when {@code update} is null
   * @throws ConflictException when any conflict occurs.
   * @throws NotFoundException when user devfile with given id not found
   * @throws ServerException when any server error occurs
   */
  public UserDevfileImpl updateUserDevfile(UserDevfile update)
      throws ConflictException, NotFoundException, ServerException {
    requireNonNull(update);
    UserDevfileImpl result = userDevfileDao.update(new UserDevfileImpl(update));
    LOG.debug(
        "UserDevfile '{}' with id '{}' update by user '{}'",
        result.getName(),
        result.getId(),
        EnvironmentContext.getCurrent().getSubject().getUserName());
    eventService.publish(new DevfileUpdatedEvent(result));
    return result;
  }

  /**
   * Removes stored {@link UserDevfile} by given id.
   *
   * @param id user devfile identifier
   * @throws NullPointerException when {@code id} is null
   * @throws ServerException when any server errors occurs
   */
  public void removeUserDevfile(String id) throws ServerException {
    requireNonNull(id);
    userDevfileDao.remove(id);
    LOG.debug(
        "UserDevfile with id '{}' removed by user '{}'",
        id,
        EnvironmentContext.getCurrent().getSubject().getUserName());
  }

  /**
   * Gets list of devfiles. Parameters, returned values and possible exceptions are the same as in
   * UserDevfileDao#getDevfiles(String, int, int, List, List)}.
   */
  public Page<UserDevfileImpl> getUserDevfiles(
      String userId,
      int maxItems,
      int skipCount,
      List<Pair<String, String>> filter,
      List<Pair<String, String>> order)
      throws ServerException {
    requireNonNull(userId, "Required non-null user id");
    return userDevfileDao.getDevfiles(userId, maxItems, skipCount, filter, order);
  }
}

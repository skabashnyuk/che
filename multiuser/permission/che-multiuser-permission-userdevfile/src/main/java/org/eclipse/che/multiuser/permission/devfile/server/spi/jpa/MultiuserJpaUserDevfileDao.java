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

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;
import static org.eclipse.che.api.devfile.server.jpa.JpaUserDevfileDao.DEFAULT_ORDER;
import static org.eclipse.che.multiuser.permission.devfile.server.spi.jpa.MultiuserJpaUserDevfileDao.MultiuserUserDevfileSearchQueryBuilder.newBuilder;

import com.google.inject.persist.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import org.eclipse.che.api.core.ConflictException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.Page;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.core.notification.EventService;
import org.eclipse.che.api.devfile.server.event.BeforeDevfileRemovedEvent;
import org.eclipse.che.api.devfile.server.jpa.JpaUserDevfileDao;
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
    requireNonNull(userId);
    checkArgument(maxItems > 0, "The number of items has to be positive.");
    checkArgument(
        skipCount >= 0,
        "The number of items to skip can't be negative or greater than " + Integer.MAX_VALUE);

    if (filter != null && !filter.isEmpty()) {
      List<Pair<String, String>> invalidFilter =
          filter
              .stream()
              .filter(p -> !p.first.equalsIgnoreCase("devfile.metadata.name"))
              .collect(toList());
      if (!invalidFilter.isEmpty()) {
        throw new IllegalArgumentException(
            "Filtering allowed only on `devfile.metadata.name`. But got: " + invalidFilter);
      }
    }
    List<Pair<String, String>> effectiveOrder = DEFAULT_ORDER;
    if (order != null && !order.isEmpty()) {
      List<Pair<String, String>> invalidSortOrder =
          order
              .stream()
              .filter(p -> !p.second.equalsIgnoreCase("asc"))
              .filter(p -> !p.second.equalsIgnoreCase("desc"))
              .collect(Collectors.toList());
      if (!invalidSortOrder.isEmpty()) {
        throw new IllegalArgumentException(
            "Invalid sort order direction. Possible values 'asc' or 'desc'. But got: "
                + invalidSortOrder);
      }
      effectiveOrder = order;
    }
    try {
      final long count =
          newBuilder(managerProvider.get())
              .withUserId(userId)
              .withFilter(filter)
              .buildCountQuery()
              .getSingleResult();

      if (count == 0) {
        return new Page<>(emptyList(), skipCount, maxItems, count);
      }
      List<UserDevfileImpl> result =
          newBuilder(managerProvider.get())
              .withUserId(userId)
              .withFilter(filter)
              .withOrder(effectiveOrder)
              .withMaxItems(maxItems)
              .withSkipCount(skipCount)
              .buildSelectItemsQuery()
              .getResultList()
              .stream()
              .map(UserDevfileImpl::new)
              .collect(toList());
      return new Page<>(result, skipCount, maxItems, count);

    } catch (RuntimeException x) {
      throw new ServerException(x.getLocalizedMessage(), x);
    }
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

  public static class MultiuserUserDevfileSearchQueryBuilder
      extends JpaUserDevfileDao.UserDevfileSearchQueryBuilder {

    MultiuserUserDevfileSearchQueryBuilder(EntityManager entityManager) {
      super(entityManager);
    }

    public MultiuserUserDevfileSearchQueryBuilder withUserId(String userId) {
      params.put("userId", userId);
      return this;
    }

    public static MultiuserUserDevfileSearchQueryBuilder newBuilder(EntityManager entityManager) {
      return new MultiuserUserDevfileSearchQueryBuilder(entityManager);
    }

    @Override
    public JpaUserDevfileDao.UserDevfileSearchQueryBuilder withFilter(
        List<Pair<String, String>> filter) {
      super.withFilter(filter);
      if (this.filter.isEmpty()) {
        this.filter = "WHERE permission.userId = :userId AND 'read' MEMBER OF permission.actions";
      } else {
        this.filter += " AND permission.userId = :userId AND 'read' MEMBER OF permission.actions";
      }
      return this;
    }

    @Override
    public TypedQuery<Long> buildCountQuery() {
      StringBuilder query =
          new StringBuilder()
              .append("SELECT ")
              .append(" COUNT(userdevfile) ")
              .append("FROM UserDevfilePermission permission ")
              .append("LEFT JOIN permission.userDevfile userdevfile ")
              .append(filter);
      TypedQuery<Long> typedQuery = entityManager.createQuery(query.toString(), Long.class);
      params.forEach((k, v) -> typedQuery.setParameter(k, v));
      return typedQuery;
    }

    @Override
    public TypedQuery<UserDevfileImpl> buildSelectItemsQuery() {
      StringBuilder query =
          new StringBuilder()
              .append("SELECT ")
              .append(" userdevfile ")
              .append("FROM UserDevfilePermission permission ")
              .append("LEFT JOIN permission.userDevfile userdevfile ")
              .append(filter)
              .append(order);
      TypedQuery<UserDevfileImpl> typedQuery =
          entityManager
              .createQuery(query.toString(), UserDevfileImpl.class)
              .setFirstResult(skipCount)
              .setMaxResults(maxItems);
      params.forEach((k, v) -> typedQuery.setParameter(k, v));
      return typedQuery;
    }
  }
}

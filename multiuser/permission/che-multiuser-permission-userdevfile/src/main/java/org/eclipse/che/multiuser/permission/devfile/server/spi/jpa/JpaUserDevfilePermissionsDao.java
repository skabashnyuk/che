package org.eclipse.che.multiuser.permission.devfile.server.spi.jpa;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.persist.Transactional;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.Page;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.core.notification.EventService;
import org.eclipse.che.api.devfile.server.event.BeforeDevfileRemovedEvent;
import org.eclipse.che.api.user.server.event.BeforeUserRemovedEvent;
import org.eclipse.che.commons.annotation.Nullable;
import org.eclipse.che.core.db.cascade.CascadeEventSubscriber;
import org.eclipse.che.multiuser.api.permission.server.AbstractPermissionsDomain;
import org.eclipse.che.multiuser.api.permission.server.jpa.AbstractJpaPermissionsDao;
import org.eclipse.che.multiuser.permission.devfile.server.model.impl.UserDevfilePermissionsImpl;
import org.eclipse.che.multiuser.permission.devfile.server.spi.UserDevfilePermissionsDao;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

public class JpaUserDevfilePermissionsDao
    extends AbstractJpaPermissionsDao<UserDevfilePermissionsImpl>
    implements UserDevfilePermissionsDao {

  @Inject
  public JpaUserDevfilePermissionsDao(
      AbstractPermissionsDomain<UserDevfilePermissionsImpl> supportedDomain) {
    super(supportedDomain);
  }

  @Override
  public UserDevfilePermissionsImpl get(String userId, String instanceId)
      throws ServerException, NotFoundException {

    requireNonNull(instanceId, "Workspace identifier required");
    requireNonNull(userId, "User identifier required");
    try {
      return new UserDevfilePermissionsImpl(getEntity(wildcardToNull(userId), instanceId));
    } catch (RuntimeException x) {
      throw new ServerException(x.getLocalizedMessage(), x);
    }
  }

  @Override
  public List<UserDevfilePermissionsImpl> getByUser(String userId) throws ServerException {
    requireNonNull(userId, "User identifier required");
    return doGetByUser(wildcardToNull(userId)).stream()
        .map(UserDevfilePermissionsImpl::new)
        .collect(toList());
  }

  @Override
  @Transactional
  public Page<UserDevfilePermissionsImpl> getByInstance(
      String instanceId, int maxItems, long skipCount) throws ServerException {
    requireNonNull(instanceId, "Workspace identifier required");
    checkArgument(
        skipCount <= Integer.MAX_VALUE,
        "The number of items to skip can't be greater than " + Integer.MAX_VALUE);

    try {
      final EntityManager entityManager = managerProvider.get();
      final List<UserDevfilePermissionsImpl> permissions =
          entityManager
              .createNamedQuery(
                  "UserDevfilePermissions.getByUserDevfileId", UserDevfilePermissionsImpl.class)
              .setParameter("userDevfileId", instanceId)
              .setMaxResults(maxItems)
              .setFirstResult((int) skipCount)
              .getResultList()
              .stream()
              .map(UserDevfilePermissionsImpl::new)
              .collect(toList());
      final Long workersCount =
          entityManager
              .createNamedQuery("UserDevfilePermissions.getCountByUserDevfileId", Long.class)
              .setParameter("userDevfileId", instanceId)
              .getSingleResult();
      return new Page<>(permissions, skipCount, maxItems, workersCount);
    } catch (RuntimeException e) {
      throw new ServerException(e.getLocalizedMessage(), e);
    }
  }

  @Override
  protected UserDevfilePermissionsImpl getEntity(String userId, String instanceId)
      throws NotFoundException, ServerException {
    try {
      return doGet(userId, instanceId);
    } catch (NoResultException e) {
      throw new NotFoundException(
          format("Worker of workspace '%s' with id '%s' was not found.", instanceId, userId));
    } catch (RuntimeException e) {
      throw new ServerException(e.getMessage(), e);
    }
  }

  @Override
  public UserDevfilePermissionsImpl getUserDevfilePermissions(String userDevfileId, String userId)
      throws ServerException, NotFoundException {
    return new UserDevfilePermissionsImpl(get(userId, userDevfileId));
  }

  @Override
  public void removeUserDevfilePermissions(String userDevfileId, String userId)
      throws ServerException {
    try {
      super.remove(userId, userDevfileId);
    } catch (NotFoundException e) {
      throw new ServerException(e);
    }
  }

  @Override
  public Page<UserDevfilePermissionsImpl> getUserDevfilePermissions(
      String userDevfileId, int maxItems, long skipCount) throws ServerException {
    return getByInstance(userDevfileId, maxItems, skipCount);
  }

  @Override
  public List<UserDevfilePermissionsImpl> getUserDevfilePermissionsByUser(String userId)
      throws ServerException {
    return getByUser(userId);
  }

  @Transactional
  protected UserDevfilePermissionsImpl doGet(String userId, String instanceId) {
    return managerProvider
        .get()
        .createNamedQuery(
            "UserDevfilePermissions.getByUserAndUserDevfileId", UserDevfilePermissionsImpl.class)
        .setParameter("workspaceId", instanceId)
        .setParameter("userId", userId)
        .getSingleResult();
  }

  @Transactional
  protected List<UserDevfilePermissionsImpl> doGetByUser(@Nullable String userId)
      throws ServerException {
    try {
      return managerProvider
          .get()
          .createNamedQuery("UserDevfilePermissions.getByUserId", UserDevfilePermissionsImpl.class)
          .setParameter("userId", userId)
          .getResultList();
    } catch (RuntimeException e) {
      throw new ServerException(e.getLocalizedMessage(), e);
    }
  }

  @Singleton
  public static class RemoveUserDevfilePermissionsBeforeUserDevfuleRemovedEventSubscriber
      extends CascadeEventSubscriber<BeforeDevfileRemovedEvent> {
    private static final int PAGE_SIZE = 100;

    @Inject private EventService eventService;
    @Inject private UserDevfilePermissionsDao userDevfilePermissionsDao;

    @PostConstruct
    public void subscribe() {
      eventService.subscribe(this, BeforeDevfileRemovedEvent.class);
    }

    @PreDestroy
    public void unsubscribe() {
      eventService.unsubscribe(this, BeforeDevfileRemovedEvent.class);
    }

    @Override
    public void onCascadeEvent(BeforeDevfileRemovedEvent event) throws Exception {
      removeWorkers(event.getUserDevfile().getId(), PAGE_SIZE);
    }

    @VisibleForTesting
    void removeWorkers(String workspaceId, int pageSize) throws ServerException {
      Page<UserDevfilePermissionsImpl> permissionsPage;
      do {
        // skip count always equals to 0 because elements will be shifted after removing previous
        // items
        permissionsPage =
            userDevfilePermissionsDao.getUserDevfilePermissions(workspaceId, pageSize, 0);
        for (UserDevfilePermissionsImpl permission : permissionsPage.getItems()) {
          userDevfilePermissionsDao.removeUserDevfilePermissions(
              permission.getInstanceId(), permission.getUserId());
        }
      } while (permissionsPage.hasNextPage());
    }
  }

  @Singleton
  public static class RemoveUserDevfilePermissionsBeforeUserRemovedEventSubscriber
      extends CascadeEventSubscriber<BeforeUserRemovedEvent> {
    @Inject private EventService eventService;
    @Inject private UserDevfilePermissionsDao userDevfilePermissionsDao;

    @PostConstruct
    public void subscribe() {
      eventService.subscribe(this, BeforeUserRemovedEvent.class);
    }

    @PreDestroy
    public void unsubscribe() {
      eventService.unsubscribe(this, BeforeUserRemovedEvent.class);
    }

    @Override
    public void onCascadeEvent(BeforeUserRemovedEvent event) throws Exception {
      for (UserDevfilePermissionsImpl permission :
          userDevfilePermissionsDao.getUserDevfilePermissionsByUser(event.getUser().getId())) {
        userDevfilePermissionsDao.removeUserDevfilePermissions(
            permission.getInstanceId(), permission.getUserId());
      }
    }
  }
}

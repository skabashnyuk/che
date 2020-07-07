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

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.eclipse.che.account.shared.model.Account;
import org.eclipse.che.account.spi.AccountImpl;
import org.eclipse.che.api.user.server.model.impl.UserImpl;
import org.eclipse.che.api.workspace.server.jpa.JpaWorkspaceDao;
import org.eclipse.che.api.workspace.server.model.impl.WorkspaceConfigImpl;
import org.eclipse.che.api.workspace.server.model.impl.WorkspaceImpl;
import org.eclipse.che.commons.test.tck.TckResourcesCleaner;
import org.eclipse.che.multiuser.permission.devfile.server.model.impl.UserDevfilePermissionsImpl;
import org.eclipse.che.multiuser.permission.devfile.server.spi.jpa.JpaUserDevfilePermissionsDao.RemoveUserDevfilePermissionsBeforeUserDevfuleRemovedEventSubscriber;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.persistence.EntityManager;
import java.util.Arrays;
import java.util.stream.Stream;

import static org.testng.AssertJUnit.assertEquals;

/**
 * Tests for {@link RemoveUserDevfilePermissionsBeforeUserDevfuleRemovedEventSubscriber}
 *
 * @author Sergii Leschenko
 */
public class RemoveUserDevfilePermissionsBeforeUserDevfileRemovedEventSubscriberTest {
  private TckResourcesCleaner tckResourcesCleaner;
  private EntityManager manager;
  private JpaUserDevfilePermissionsDao userDevfilePermissionsDao;
  private JpaWorkspaceDao workspaceDao;

  private RemoveUserDevfilePermissionsBeforeUserDevfuleRemovedEventSubscriber subscriber;

  private WorkspaceImpl workspace;
  private UserDevfilePermissionsImpl[] userDevfilePermissions;
  private UserImpl[] users;
  private Account account;

  @BeforeClass
  public void setupEntities() throws Exception {
    account = new AccountImpl("account1", "accountName", "test");

    users =
        new UserImpl[] {
          new UserImpl("user1", "user1@com.com", "usr1"),
          new UserImpl("user2", "user2@com.com", "usr2")
        };

    workspace =
        new WorkspaceImpl(
            "ws1", account, new WorkspaceConfigImpl("", "", "cfg1", null, null, null, null));

    userDevfilePermissions =
        new UserDevfilePermissionsImpl[] {
          new UserDevfilePermissionsImpl("ws1", "user1", Arrays.asList("read", "use", "run")),
          new UserDevfilePermissionsImpl("ws1", "user2", Arrays.asList("read", "use"))
        };

    Injector injector = Guice.createInjector(new JpaTckModule());

    manager = injector.getInstance(EntityManager.class);
    userDevfilePermissionsDao = injector.getInstance(JpaUserDevfilePermissionsDao.class);
    workspaceDao = injector.getInstance(JpaWorkspaceDao.class);
    subscriber = injector.getInstance(RemoveUserDevfilePermissionsBeforeUserDevfuleRemovedEventSubscriber.class);
    subscriber.subscribe();
    tckResourcesCleaner = injector.getInstance(TckResourcesCleaner.class);
  }

  @BeforeMethod
  public void setUp() throws Exception {
    manager.getTransaction().begin();
    manager.persist(account);
    manager.persist(workspace);
    Stream.of(users).forEach(manager::persist);
    Stream.of(userDevfilePermissions).forEach(manager::persist);
    manager.getTransaction().commit();
    manager.clear();
  }

  @AfterMethod
  public void cleanup() {
    manager.getTransaction().begin();

    manager
        .createQuery("SELECT e FROM Worker e", UserDevfilePermissionsImpl.class)
        .getResultList()
        .forEach(manager::remove);

    manager
        .createQuery("SELECT w FROM Workspace w", WorkspaceImpl.class)
        .getResultList()
        .forEach(manager::remove);

    manager
        .createQuery("SELECT u FROM Usr u", UserImpl.class)
        .getResultList()
        .forEach(manager::remove);

    manager
        .createQuery("SELECT a FROM Account a", AccountImpl.class)
        .getResultList()
        .forEach(manager::remove);
    manager.getTransaction().commit();
  }

  @AfterClass
  public void shutdown() throws Exception {
    subscriber.unsubscribe();
    tckResourcesCleaner.clean();
  }

  @Test
  public void shouldRemoveAllWorkersWhenWorkspaceIsRemoved() throws Exception {
    workspaceDao.remove(workspace.getId());

    assertEquals(userDevfilePermissionsDao.getUserDevfilePermissions(workspace.getId(), 1, 0).getTotalItemsCount(), 0);
  }

  @Test
  public void shouldRemoveAllWorkersWhenPageSizeEqualsToOne() throws Exception {
    subscriber.removeWorkers(workspace.getId(), 1);

    assertEquals(userDevfilePermissionsDao.getUserDevfilePermissions(workspace.getId(), 1, 0).getTotalItemsCount(), 0);
  }
}

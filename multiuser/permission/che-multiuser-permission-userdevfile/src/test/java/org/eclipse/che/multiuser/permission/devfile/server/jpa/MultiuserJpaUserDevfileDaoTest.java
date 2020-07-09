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
package org.eclipse.che.multiuser.permission.devfile.server.jpa;

import static org.eclipse.che.commons.lang.NameGenerator.generate;
import static org.eclipse.che.multiuser.permission.devfile.server.TestObjectGenerator.createDevfile;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import com.google.common.collect.ImmutableList;
import com.google.inject.Guice;
import com.google.inject.Injector;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.persistence.EntityManager;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.devfile.server.model.impl.UserDevfileImpl;
import org.eclipse.che.api.user.server.model.impl.UserImpl;
import org.eclipse.che.commons.test.tck.TckResourcesCleaner;
import org.eclipse.che.multiuser.permission.devfile.server.model.impl.UserDevfilePermissionImpl;
import org.eclipse.che.multiuser.permission.devfile.server.spi.jpa.MultiuserJpaUserDevfileDao;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** @author Max Shaposhnik */
public class MultiuserJpaUserDevfileDaoTest {
  private TckResourcesCleaner tckResourcesCleaner;
  private EntityManager manager;
  private MultiuserJpaUserDevfileDao dao;

  private List<UserDevfilePermissionImpl> permissions;
  private List<UserImpl> users;
  private List<UserDevfileImpl> userDevfiles;

  @BeforeClass
  public void setupEntities() throws Exception {
    permissions =
        ImmutableList.of(
            new UserDevfilePermissionImpl(
                "devfile_id1", "user1", Arrays.asList("read", "use", "search")),
            new UserDevfilePermissionImpl("devfile_id2", "user1", Arrays.asList("read", "search")),
            new UserDevfilePermissionImpl("devfile_id3", "user1", Arrays.asList("none", "run")),
            new UserDevfilePermissionImpl("devfile_id1", "user2", Arrays.asList("read", "use")));

    users =
        ImmutableList.of(
            new UserImpl("user1", "user1@com.com", "usr1"),
            new UserImpl("user2", "user2@com.com", "usr2"));

    userDevfiles =
        ImmutableList.of(
            new UserDevfileImpl("devfile_id1", createDevfile(generate("name", 6))),
            new UserDevfileImpl("devfile_id2", createDevfile(generate("name", 6))),
            new UserDevfileImpl("devfile_id3", createDevfile(generate("name", 6))));
    Injector injector = Guice.createInjector(new UserDevfileTckModule());
    manager = injector.getInstance(EntityManager.class);
    dao = injector.getInstance(MultiuserJpaUserDevfileDao.class);
    tckResourcesCleaner = injector.getInstance(TckResourcesCleaner.class);
  }

  @BeforeMethod
  public void setUp() throws Exception {
    manager.getTransaction().begin();

    users.forEach(manager::persist);
    userDevfiles.forEach(manager::persist);
    permissions.forEach(manager::persist);

    manager.getTransaction().commit();
    manager.clear();
  }

  @AfterMethod
  public void cleanup() {
    manager.getTransaction().begin();

    manager
        .createQuery("SELECT e FROM UserDevfilePermission e", UserDevfilePermissionImpl.class)
        .getResultList()
        .forEach(manager::remove);

    manager
        .createQuery("SELECT w FROM UserDevfile w", UserDevfileImpl.class)
        .getResultList()
        .forEach(manager::remove);

    manager
        .createQuery("SELECT u FROM Usr u", UserImpl.class)
        .getResultList()
        .forEach(manager::remove);

    manager.getTransaction().commit();
  }

  @Test
  public void shouldGetTotalWorkspaceCount() throws ServerException {
    assertEquals(dao.getTotalCount(), 3);
  }

  @AfterClass
  public void shutdown() throws Exception {
    tckResourcesCleaner.clean();
  }

  @Test
  public void shouldFindStackByPermissions() throws Exception {
    List<UserDevfileImpl> results =
        dao.getDevfiles(
                users.get(0).getId(), 30, 0, Collections.emptyList(), Collections.emptyList())
            .getItems();
    assertEquals(results.size(), 2);
    assertTrue(results.contains(userDevfiles.get(0)));
    assertTrue(results.contains(userDevfiles.get(1)));
  }
}

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

import java.util.List;
import javax.inject.Singleton;
import org.eclipse.che.api.core.ConflictException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.Page;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.devfile.server.model.impl.UserDevfileImpl;
import org.eclipse.che.api.devfile.server.spi.UserDevfileDao;
import org.eclipse.che.commons.lang.Pair;

/** JPA based implementation of {@link UserDevfileDao}. */
@Singleton
public class MultiuserJpaUserDevfileDao implements UserDevfileDao {

  @Override
  public UserDevfileImpl create(UserDevfileImpl devfile) throws ServerException, ConflictException {
    return null;
  }

  @Override
  public UserDevfileImpl update(UserDevfileImpl devfile)
      throws NotFoundException, ConflictException, ServerException {
    return null;
  }

  @Override
  public void remove(String id) throws ServerException {}

  @Override
  public UserDevfileImpl getById(String id) throws NotFoundException, ServerException {
    return null;
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
    return 0;
  }
}

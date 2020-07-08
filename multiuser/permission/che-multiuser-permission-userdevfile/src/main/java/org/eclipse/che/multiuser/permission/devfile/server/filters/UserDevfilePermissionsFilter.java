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
package org.eclipse.che.multiuser.permission.devfile.server.filters;

import org.eclipse.che.api.core.ForbiddenException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.devfile.server.UserDevfileManager;
import org.eclipse.che.api.devfile.server.UserDevfileService;
import org.eclipse.che.commons.env.EnvironmentContext;
import org.eclipse.che.commons.subject.Subject;
import org.eclipse.che.everrest.CheMethodInvokerFilter;
import org.eclipse.che.multiuser.permission.devfile.server.UserDevfileDomain;
import org.everrest.core.Filter;
import org.everrest.core.resource.GenericResourceMethod;

import javax.inject.Inject;
import javax.ws.rs.Path;

/**
 * Restricts access to methods of {@link UserDevfileService} by users' permissions.
 *
 * <p>Filter contains rules for protecting of all methods of {@link UserDevfileService}.<br>
 * In case when requested method is unknown filter throws {@link ForbiddenException}
 */
@Filter
@Path("/userdevfile{path:(/.*)?}")
public class UserDevfilePermissionsFilter extends CheMethodInvokerFilter {
  private final UserDevfileManager userDevfileManager;

  @Inject
  public UserDevfilePermissionsFilter(UserDevfileManager userDevfileManager) {
    this.userDevfileManager = userDevfileManager;
  }

  @Override
  public void filter(GenericResourceMethod genericResourceMethod, Object[] arguments)
      throws ForbiddenException {
    final String methodName = genericResourceMethod.getMethod().getName();
    final Subject currentSubject = EnvironmentContext.getCurrent().getSubject();
    switch (methodName) {
      case "getById":
        currentSubject.checkPermission(
            UserDevfileDomain.DOMAIN_ID, ((String) arguments[0]), UserDevfileDomain.READ);
        break;
      case "update":
        currentSubject.checkPermission(
            UserDevfileDomain.DOMAIN_ID, ((String) arguments[0]), UserDevfileDomain.UPDATE);
        break;
      case "delete":
        currentSubject.checkPermission(
            UserDevfileDomain.DOMAIN_ID, ((String) arguments[0]), UserDevfileDomain.DELETE);
        break;
      case "create":
      case "getUserDevfiles":
        return;
      default:
        throw new ForbiddenException("The user does not have permission to perform this operation");
    }
  }
}

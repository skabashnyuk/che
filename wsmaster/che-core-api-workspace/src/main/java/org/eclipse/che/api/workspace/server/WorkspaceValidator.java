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
package org.eclipse.che.api.workspace.server;

import static java.lang.String.format;

import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.eclipse.che.api.core.ValidationException;
import org.eclipse.che.api.core.model.workspace.Workspace;

/**
 * Validator for {@link Workspace}.
 *
 * @author Yevhenii Voevodin
 */
@Singleton
public class WorkspaceValidator {

  /**
   * Must contain [3, 100] characters, first and last character is letter or digit, available
   * characters {A-Za-z0-9.-_}.
   */
  private static final Pattern WS_NAME =
      Pattern.compile("[a-zA-Z0-9][-_.a-zA-Z0-9]{1,98}[a-zA-Z0-9]");

  private static final Pattern VOLUME_NAME =
      Pattern.compile("[a-zA-Z][a-zA-Z0-9-_.]{0,18}[a-zA-Z0-9]");
  private static final Pattern VOLUME_PATH = Pattern.compile("/.+");

  private final Set<WorkspaceAttributeValidator> attributeValidators;

  @Inject
  public WorkspaceValidator(Set<WorkspaceAttributeValidator> attributeValidators) {
    this.attributeValidators = attributeValidators;
  }

  /**
   * Checks whether workspace attributes are valid. The attribute is valid if it's key is not null &
   * not empty & is not prefixed with 'codenvy'.
   *
   * @param attributes the map to check
   * @throws ValidationException when attributes are not valid
   */
  public void validateAttributes(Map<String, String> attributes) throws ValidationException {
    for (String attributeName : attributes.keySet()) {
      // attribute name should not be empty and should not start with codenvy
      check(
          attributeName != null
              && !attributeName.trim().isEmpty()
              && !attributeName.toLowerCase().startsWith("codenvy"),
          "Attribute name '%s' is not valid",
          attributeName);
    }

    for (WorkspaceAttributeValidator attributeValidator : attributeValidators) {
      attributeValidator.validate(attributes);
    }
  }

  /**
   * Checks whether workspace attributes are valid on updating.
   *
   * @param existing actual attributes
   * @param update new attributes that are going to be stored instead of existing
   * @throws ValidationException when attributes are not valid
   */
  public void validateUpdateAttributes(Map<String, String> existing, Map<String, String> update)
      throws ValidationException {
    for (WorkspaceAttributeValidator attributeValidator : attributeValidators) {
      attributeValidator.validateUpdate(existing, update);
    }
  }

  private void validateLongAttribute(
      String attributeName, String attributeValue, String machineName) throws ValidationException {
    if (attributeValue != null) {
      try {
        Long.parseLong(attributeValue);
      } catch (NumberFormatException e) {
        throw new ValidationException(
            format(
                "Value '%s' of attribute '%s' in machine '%s' is illegal",
                attributeValue, attributeName, machineName));
      }
    }
  }

  /**
   * Checks that object reference is not null, throws {@link ValidationException} in the case of
   * null {@code object} with given {@code message}.
   */
  private static void checkNotNull(Object object, String message) throws ValidationException {
    if (object == null) {
      throw new ValidationException(message);
    }
  }

  /**
   * Checks that expression is true, throws {@link ValidationException} otherwise.
   *
   * <p>Exception uses error message built from error message template and error message parameters.
   */
  private static void check(boolean expression, String fmt, Object... args)
      throws ValidationException {
    if (!expression) {
      throw new ValidationException(format(fmt, args));
    }
  }

  /**
   * Checks that expression is true, throws {@link ValidationException} otherwise.
   *
   * <p>Exception uses error message built from error message template and error message parameters.
   */
  private static void check(boolean expression, String message) throws ValidationException {
    if (!expression) {
      throw new ValidationException(message);
    }
  }
}

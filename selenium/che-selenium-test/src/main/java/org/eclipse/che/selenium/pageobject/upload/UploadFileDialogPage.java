/*
 * Copyright (c) 2012-2018 Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Red Hat, Inc. - initial API and implementation
 */
package org.eclipse.che.selenium.pageobject.upload;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.eclipse.che.selenium.core.SeleniumWebDriver;
import org.eclipse.che.selenium.core.webdriver.SeleniumWebDriverHelper;
import org.eclipse.che.selenium.core.webdriver.WebDriverWaitFactory;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

@Singleton
public class UploadFileDialogPage extends AbstractUploadDialogPage {
  private static final String UPLOAD_BUTTON_ID = "file-uploadFile-upload";
  private static final String TITLE_XPATH = "//div[text()='Upload File']";
  private static final String OVERWRITE_FILE_CHECKBOX_ID =
      "gwt-debug-file-uploadFile-overwrite-input";
  private static final String OVERWRITE_FILE_CHECKBOX_LABEL_ID =
      "gwt-debug-file-uploadFile-overwrite-label";

  @FindBy(id = UPLOAD_BUTTON_ID)
  private WebElement uploadButton;

  @FindBy(xpath = TITLE_XPATH)
  private WebElement title;

  @FindBy(id = OVERWRITE_FILE_CHECKBOX_ID)
  private WebElement overwriteIfFileExistsCheckbox;

  @FindBy(id = OVERWRITE_FILE_CHECKBOX_LABEL_ID)
  private WebElement overwriteIfFileExistsCheckboxLabel;

  @Inject
  public UploadFileDialogPage(
      SeleniumWebDriver seleniumWebDriver,
      SeleniumWebDriverHelper seleniumWebDriverHelper,
      WebDriverWaitFactory webDriverWaitFactory) {
    super(seleniumWebDriver, seleniumWebDriverHelper, webDriverWaitFactory);
  }

  @Override
  public WebElement getTitle() {
    return title;
  }

  @Override
  public WebElement getUploadButton() {
    return uploadButton;
  }

  @Override
  WebElement getOverwriteIfExistsCheckbox() {
    return overwriteIfFileExistsCheckbox;
  }

  @Override
  WebElement getOverwriteIfExistsCheckboxLabel() {
    return overwriteIfFileExistsCheckboxLabel;
  }
}

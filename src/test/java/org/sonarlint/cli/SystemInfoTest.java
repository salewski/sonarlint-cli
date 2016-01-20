/*
 * SonarLint CLI
 * Copyright (C) 2016 SonarSource
 * sonarqube@googlegroups.com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonarlint.cli;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

import static org.mockito.Mockito.verifyNoMoreInteractions;

import org.sonar.runner.api.RunnerVersion;
import org.junit.Before;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class SystemInfoTest {
  System2 mockSystem;
  Logger logger;

  @Before
  public void setUp() {
    mockSystem = mock(System2.class);
    logger = mock(Logger.class);
    SystemInfo.setSystem(mockSystem);
  }

  @Test
  public void test_java() {
    mockJava();
    assertThat(SystemInfo.java()).isEqualTo("Java 1.9 oracle (64-bit)");

    when(mockSystem.getProperty("sun.arch.data.model")).thenReturn("32");
    assertThat(SystemInfo.java()).isEqualTo("Java 1.9 oracle (32-bit)");

    when(mockSystem.getProperty("sun.arch.data.model")).thenReturn(null);
    assertThat(SystemInfo.java()).isEqualTo("Java 1.9 oracle");
  }

  @Test
  public void test_os() {
    mockOs();

    assertThat(SystemInfo.os()).isEqualTo("linux 2.5 x64");
  }

  private void mockJava() {
    when(mockSystem.getProperty("java.version")).thenReturn("1.9");
    when(mockSystem.getProperty("java.vendor")).thenReturn("oracle");
    when(mockSystem.getProperty("sun.arch.data.model")).thenReturn("64");
  }

  private void mockOs() {
    when(mockSystem.getProperty("os.version")).thenReturn("2.5");
    when(mockSystem.getProperty("os.arch")).thenReturn("x64");
    when(mockSystem.getProperty("os.name")).thenReturn("linux");
  }

  @Test
  public void should_print() {
    mockOs();
    mockJava();
    when(mockSystem.getenv("SONARLINT_OPTS")).thenReturn("arg");

    SystemInfo.print(logger);

    verify(mockSystem).getProperty("java.version");
    verify(mockSystem).getProperty("os.version");
    verify(mockSystem).getenv("SONARLINT_OPTS");

    verify(logger).info("SonarLint: unknown engine: " + RunnerVersion.version());
    verify(logger).info("Java 1.9 oracle (64-bit)");
    verify(logger).info("linux 2.5 x64");
    verify(logger).info("SONARLINT_OPTS=arg");
    verifyNoMoreInteractions(logger);
  }
}
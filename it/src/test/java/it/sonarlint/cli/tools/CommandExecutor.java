/*
 * SonarSource :: IT :: SonarLint CLI
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
package it.sonarlint.cli.tools;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteStreamHandler;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.commons.io.output.TeeOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class CommandExecutor {
  private static final Logger LOG = LoggerFactory.getLogger(CommandExecutor.class);
  private static final int TIMEOUT = 60_000;
  private Path file;

  private ByteArrayOutputStream out;
  private ByteArrayOutputStream err;
  private OutputStream in;

  public CommandExecutor(Path file) {
    this.file = file;
  }

  public int execute(String[] args) throws IOException {
    return execute(args, null, Collections.<String, String>emptyMap());
  }

  public int execute(String[] args, @Nullable Path workingDir, Map<String, String> addEnv) throws IOException {
    if (!Files.isExecutable(file)) {
      Set<PosixFilePermission> perms = new HashSet<PosixFilePermission>();
      perms.add(PosixFilePermission.OWNER_READ);
      perms.add(PosixFilePermission.OWNER_EXECUTE);
      Files.setPosixFilePermissions(file, perms);
    }

    ExecuteWatchdog watchdog = new ExecuteWatchdog(TIMEOUT);
    CommandLine cmd = new CommandLine(file.toFile());
    cmd.addArguments(args);
    DefaultExecutor exec = new DefaultExecutor();
    exec.setWatchdog(watchdog);
    exec.setStreamHandler(createStreamHandler());
    exec.setExitValues(null);
    if (workingDir != null) {
      exec.setWorkingDirectory(workingDir.toFile());
    }
    in.close();
    LOG.info("Executing: {}", cmd.toString());
    Map<String, String> env = new HashMap<>(System.getenv());
    env.putAll(addEnv);
    return exec.execute(cmd, env);
  }

  public String getStdOut() {
    return getLogs(out);
  }

  public String getStdErr() {
    return getLogs(err);
  }

  private static String getLogs(ByteArrayOutputStream stream) {
    return new String(stream.toByteArray(), Charset.defaultCharset());
  }

  private ExecuteStreamHandler createStreamHandler() throws IOException {
    out = new ByteArrayOutputStream();
    err = new ByteArrayOutputStream();
    PipedOutputStream outPiped = new PipedOutputStream();
    InputStream inPiped = new PipedInputStream(outPiped);
    in = outPiped;

    TeeOutputStream teeOut = new TeeOutputStream(out, System.out);
    TeeOutputStream teeErr = new TeeOutputStream(err, System.err);

    return new PumpStreamHandler(teeOut, teeErr, inPiped);
  }
}

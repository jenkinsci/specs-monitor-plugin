package io.jenkins.plugins.specsmonitor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Runs short-lived system commands on the node to read hardware information.
 */
final class CommandRunner {

    private static final Logger LOGGER = Logger.getLogger(CommandRunner.class.getName());
    private static final long COMMAND_TIMEOUT_SECONDS = 10;
    private static final long READ_TIMEOUT_SECONDS = 2;

    private CommandRunner() {}

    /**
     * Runs a command and returns its trimmed output, or an empty string if it
     * fails.
     */
    static String tryRun(String... cmd) {
        try {
            return run(cmd);
        } catch (IOException e) {
            LOGGER.log(Level.FINE, e, () -> "Command failed: " + cmd[0]);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.log(Level.FINE, e, () -> "Interrupted running: " + cmd[0]);
        }
        return "";
    }

    private static String run(String... cmd) throws IOException, InterruptedException {
        ProcessBuilder builder = new ProcessBuilder(cmd).redirectErrorStream(true);
        // Make tool output independent of the node's locale (e.g. "Model name:" in
        // lscpu).
        builder.environment().put("LC_ALL", "C");
        builder.environment().put("LANG", "C");
        Process p = builder.start();
        try {
            p.getOutputStream().close(); // PowerShell can wait on stdin otherwise
            CompletableFuture<String> output = CompletableFuture.supplyAsync(() -> readAll(p));
            if (!p.waitFor(COMMAND_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                throw new IOException("Timed out running " + cmd[0]);
            }
            return output.get(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (ExecutionException | TimeoutException e) {
            throw new IOException("Failed to read output of " + cmd[0], e);
        } finally {
            p.destroyForcibly();
        }
    }

    private static String readAll(Process p) {
        try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
            return r.lines().collect(Collectors.joining("\n")).trim();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}

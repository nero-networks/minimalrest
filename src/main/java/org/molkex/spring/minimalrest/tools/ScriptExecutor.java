package org.molkex.spring.minimalrest.tools;

import org.apache.commons.io.IOUtils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class ScriptExecutor {
    public static boolean isWindows = System.getProperty("os.name").toLowerCase().startsWith("windows");

    private static ExecutorService executor = Executors.newSingleThreadExecutor();

    public static int execute(Path workdir, String script, Consumer<String> consumer) throws Exception {
        ProcessBuilder builder = new ProcessBuilder();
        if (isWindows) {
            builder.command("cmd.exe", script);
        } else {
            builder.command("sh", script);
        }
        builder.directory(workdir.toFile());
        Process process = builder.start();
        executor.submit(()->
                new BufferedReader(new InputStreamReader(process.getInputStream()))
                        .lines()
                        .forEach(consumer));
        int err = process.waitFor();
        if (err != 0) {
            consumer.accept("Error while executing script: " + script);
            consumer.accept(IOUtils.toString(process.getErrorStream()));
        }
        return err;
    }
}

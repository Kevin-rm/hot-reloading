package mg.matsd;

import mg.matsd.exception.PathRegistrationTentativeException;
import mg.matsd.javaframework.core.utils.Assert;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static java.nio.file.StandardWatchEventKinds.*;

public final class FileWatcher implements Runnable {
    private static final Logger LOGGER = LogManager.getLogger(FileWatcher.class);
    private final List<Path> paths;
    private volatile boolean running;

    public FileWatcher() {
        paths   = new ArrayList<>();
        running = false;
    }

    public List<Path> getPaths() {
        return Collections.unmodifiableList(paths);
    }

    public FileWatcher addPath(final String pathString) {
        Assert.notNull(pathString, "L'argument pathString ne peut pas être \"null\"");

        paths.add(Path.of(pathString));
        return this;
    }

    public boolean isRunning() {
        return running;
    }

    @Override
    public void run() {
        running = true;

        try (WatchService watchService = FileSystems.getDefault().newWatchService()) {
            for (Path path : paths) registerPath(path, watchService);

            while (running) {
                WatchKey watchKey = watchService.take();
                watchKey.pollEvents().forEach(event -> {
                    WatchEvent.Kind<?> eventKind = event.kind();
                    if (eventKind == OVERFLOW) return;

                    Path watchable = (Path) watchKey.watchable();
                    Path p = watchable.resolve((Path) event.context());

                    if (eventKind == ENTRY_CREATE && Files.isDirectory(p))
                        registerPath(p, watchService);
                });

                if (!watchKey.reset()) break;
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally { running = false; }
    }

    private static void registerPath(final Path path, final WatchService watchService)
        throws PathRegistrationTentativeException {
        try (Stream<Path> pathStream = Files.walk(path)) {
            pathStream.filter(Files::isDirectory)
                .forEach(p -> {
                    try {
                        p.register(watchService, ENTRY_MODIFY, ENTRY_CREATE, ENTRY_DELETE);
                        LOGGER.debug("Enregistrement réussi du path: {}", p);
                    } catch (Exception e) {
                        LOGGER.error("Échec lors de l'enregistrement du path \"{}\"", p, e);
                    }
                });
        } catch (IOException | SecurityException e) {
            throw new PathRegistrationTentativeException(e);
        }
    }
}

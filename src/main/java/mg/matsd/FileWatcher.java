package mg.matsd;

import mg.matsd.exception.PathRegistrationTentativeException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.stream.Stream;

import static java.nio.file.StandardWatchEventKinds.*;

public class FileWatcher implements Runnable {
    private static final Logger LOGGER = LogManager.getLogger(FileWatcher.class);
    private final List<Path> paths;

    public FileWatcher(final List<Path> paths) {
        this.paths = paths;
    }

    @Override
    public void run() {
        try (WatchService watchService = FileSystems.getDefault().newWatchService()) {
            for (Path path : paths) registerPath(path, watchService);

            while (true) {
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

        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
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

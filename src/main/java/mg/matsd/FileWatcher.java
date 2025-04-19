package mg.matsd;

import mg.matsd.exception.PathRegistrationTentativeException;
import mg.matsd.javaframework.core.utils.Assert;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

import static java.nio.file.StandardWatchEventKinds.*;

public final class FileWatcher implements Runnable {
    private static final Logger LOGGER = LogManager.getLogger(FileWatcher.class);
    private static final WatchEvent.Kind<?>[] WATCH_EVENT_KINDS = new WatchEvent.Kind[]{
        ENTRY_CREATE, ENTRY_MODIFY, ENTRY_DELETE};

    private final Set<Path> paths;
    private volatile boolean running;

    public FileWatcher() {
        paths   = new HashSet<>();
        running = false;
    }

    public Set<Path> getPaths() {
        return Collections.unmodifiableSet(paths);
    }

    public FileWatcher addPath(final String pathString) {
        Assert.notNull(pathString, "L'argument pathString ne peut pas être \"null\"");
        Assert.state(!running, "Impossible d'ajouter un chemin lorsque le FileWatcher est déjà en cours d'exécution");

        Path path = Path.of(pathString).toAbsolutePath().normalize();
        Assert.isTrue(Files.exists(path), String.format("Le chemin \"%s\" n'existe pas", pathString));
        Assert.isTrue(Files.isDirectory(path), String.format("Le chemin \"%s\" n'est pas un répertoire", pathString));

        paths.add(path);
        return this;
    }

    public boolean isRunning() {
        return running;
    }

    @Override
    public void run() {
        try (WatchService watchService = FileSystems.getDefault().newWatchService()) {
            for (Path path : paths) registerPath(path, watchService);

            running = true;
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
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally { stop(); }
    }

    public void stop() {
        if (running) running = false;
    }

    private static void registerPath(final Path path, final WatchService watchService) {
        Deque<Path> deque = new LinkedList<>();
        deque.push(path);

        while (!deque.isEmpty()) {
            try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(deque.pop())) {
                for (Path p : directoryStream) if (Files.isDirectory(p)) try {
                    p.register(watchService, WATCH_EVENT_KINDS);
                    LOGGER.debug("Surveillance du chemin \"{}\" enregistrée", p);

                    deque.push(p);
                } catch (IOException e) {
                    LOGGER.error("Échec de surveillance du chemin \"{}\"", p, e);
                }
            } catch (IOException | SecurityException e) {
                throw new PathRegistrationTentativeException(e);
            }
        }
    }
}

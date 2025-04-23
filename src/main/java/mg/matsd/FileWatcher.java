package mg.matsd;

import mg.matsd.exception.PathRegistrationTentativeException;
import mg.matsd.javaframework.core.utils.Assert;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

import static java.nio.file.StandardWatchEventKinds.*;

public class FileWatcher implements Runnable {
    private static final Logger LOGGER = LogManager.getLogger(FileWatcher.class);
    private static final WatchEvent.Kind<?>[] WATCH_EVENT_KINDS = new WatchEvent.Kind[]{
        ENTRY_CREATE, ENTRY_MODIFY, ENTRY_DELETE};

    private final DynamicClassLoader dynamicClassLoader;
    private final Set<Path> paths;
    private volatile boolean running;

    FileWatcher(DynamicClassLoader dynamicClassLoader) {
        this.dynamicClassLoader = dynamicClassLoader;

        paths = new HashSet<>();
        paths.add(dynamicClassLoader.getClassOutputPath());
        running = false;
    }

    public Set<Path> getPaths() {
        return Collections.unmodifiableSet(paths);
    }

    public FileWatcher addPath(final String pathString) {
        Assert.state(!running, "Impossible d'ajouter un chemin lorsque le FileWatcher est déjà en cours d'exécution");

        paths.add(Utils.stringToPath(pathString));
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

                    final Path resolvedContext = ((Path) watchKey.watchable()).resolve((Path) event.context());
                    if (eventKind == ENTRY_CREATE && Files.isDirectory(resolvedContext)) {
                        registerPath(resolvedContext, watchService);
                        return;
                    }

                    final String s = resolvedContext.toString();
                    if (eventKind == ENTRY_MODIFY && s.endsWith(".class"))
                        dynamicClassLoader.reload(resolvedContext);
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

package mg.matsd;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.List;

public class FileWatcher implements Runnable {
    private final List<Path> paths;

    public FileWatcher(List<Path> paths) {
        this.paths = paths;
    }

    @Override
    public void run() {
        try (WatchService watchService = FileSystems.getDefault().newWatchService()) {
            while (true) {
                WatchKey watchKey = watchService.take();
                watchKey.pollEvents().forEach(event -> {
                    event.kind();
                });

                if (!watchKey.reset()) break;
            }

        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}

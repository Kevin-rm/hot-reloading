package mg.matsd;

import java.nio.file.Path;

public class Main {

    public static void main(String[] args) {

        FileWatcher fileWatcher = new FileWatcher()
            .addPath("target/classes");

        Thread thread = new Thread(fileWatcher);
        thread.start();
    }
}

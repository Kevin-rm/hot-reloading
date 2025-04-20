package mg.matsd;

import java.lang.instrument.Instrumentation;

public class Agent {
    static Instrumentation instrumentation;

    public static Instrumentation getInstrumentation() {
        return instrumentation;
    }

    public static void premain(String args, Instrumentation instrumentation) {
        Agent.instrumentation = instrumentation;

        FileWatcher fileWatcher = new FileWatcher();
        fileWatcher.addPath("/target/classes");

        Thread thread = new Thread(fileWatcher, "FileWatcher-Thread");
        thread.setDaemon(true);
        thread.start();
    }
}

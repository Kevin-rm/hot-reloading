package mg.matsd;

import java.lang.instrument.Instrumentation;

public class Agent {

    public static void premain(String args, Instrumentation instrumentation) {
        System.out.println("Initialisation de l'agent HotReloading");
    }
}

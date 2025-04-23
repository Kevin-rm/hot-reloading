package mg.matsd;

import mg.matsd.javaframework.core.utils.Assert;

import java.nio.file.Files;
import java.nio.file.Path;

public final class Utils {

    private Utils() { }

    public static Path stringToPath(final String string) {
        Assert.notNull(string, "La chaîne de caractères passée en argument ne peut pas être \"null\"");

        Path path = Path.of(string).toAbsolutePath().normalize();
        Assert.isTrue(Files.exists(path), String.format("Le chemin \"%s\" n'existe pas", string));
        Assert.isTrue(Files.isDirectory(path), String.format("Le chemin \"%s\" n'est pas un répertoire", string));

        return path;
    }
}

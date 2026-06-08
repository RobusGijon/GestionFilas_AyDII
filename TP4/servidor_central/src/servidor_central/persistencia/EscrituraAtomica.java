package servidor_central.persistencia;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Escritura atomica de archivos de estado. Como primario y secundario comparten
 * un unico archivo por combinacion cifrado+formato (ver {@code Main}), un lector
 * podria leerlo justo mientras el escritor lo vuelca. {@link Files#writeString}
 * trunca-y-escribe (no es atomico), de modo que ese lector veria un archivo vacio
 * o a medias.
 *
 * Para evitarlo se escribe primero a un archivo temporal y luego se hace un rename
 * atomico del SO al destino: cualquier lector ve el archivo viejo completo o el
 * nuevo completo, nunca uno a medio escribir.
 */
public final class EscrituraAtomica {

    private EscrituraAtomica() {
    }

    /** Escribe {@code contenido} en {@code destino} de forma atomica (UTF-8). */
    public static void escribir(Path destino, String contenido) throws IOException {
        if (destino.getParent() != null) {
            Files.createDirectories(destino.getParent());
        }
        Path tmp = destino.resolveSibling(destino.getFileName() + ".tmp");
        Files.writeString(tmp, contenido, StandardCharsets.UTF_8);
        try {
            Files.move(tmp, destino,
                    StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e) {
            // Fallback si el sistema de archivos no soporta el move atomico.
            Files.move(tmp, destino, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}

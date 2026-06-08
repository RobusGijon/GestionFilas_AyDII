package servidor_central.persistencia.txt;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import servidor_central.datos.ClienteAtendido;
import servidor_central.datos.PuestoInfo;
import servidor_central.datos.Turno;
import servidor_central.interfaces.IEstrategiaEncriptacion;
import servidor_central.interfaces.factory.IPersistenciaLector;
import servidor_central.persistencia.SelloPersistencia;
import servidor_central.persistencia.dto.ServerDTO;

public class LectorTXT implements IPersistenciaLector {

    private final IEstrategiaEncriptacion encriptacion;

    public LectorTXT() {
        this(null);
    }

    public LectorTXT(IEstrategiaEncriptacion encriptacion) {
        this.encriptacion = encriptacion;
    }

    @Override
    public ServerDTO Leer(String nombreArchivo) {
        Path path = Path.of("data", nombreArchivo);
        ServerDTO dto = new ServerDTO();
        if (!Files.exists(path)) {
            return dto;
        }

        String contenido;
        try {
            contenido = Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new UncheckedIOException("No se pudo leer el estado de " + nombreArchivo, ex);
        }
        if (contenido.isBlank()) {
            return dto;
        }
        // Descifra (si hay) y verifica el sello: clave/cifrado incorrecto -> aborta.
        contenido = SelloPersistencia.descifrarYVerificar(contenido, encriptacion);

        for (String linea : contenido.split("\n", -1)) {
            if (linea.isBlank()) {
                continue;
            }
            String[] c = linea.split("\\|", -1);
            switch (c[0]) {
                case "PUESTO":
                    dto.agregarPuesto(new PuestoInfo(
                            Integer.parseInt(c[1]),
                            c[2].isEmpty() ? null : c[2]));
                    break;
                case "FILA":
                    dto.agregarTurnoEnEspera(new Turno(
                            c[1], Integer.parseInt(c[2]), null));
                    break;
                case "LLAMADO":
                    int idPuesto = Integer.parseInt(c[1]);
                    dto.asignarTurnoAPuesto(idPuesto, new Turno(
                            c[2], Integer.parseInt(c[3]), idPuesto));
                    break;
                case "HIST":
                    dto.agregarAlHistorial(new Turno(
                            c[2], 1, Integer.parseInt(c[1])));
                    break;
                case "ATENDIDO": {
                    PuestoInfo p = dto.buscarPuesto(Integer.parseInt(c[1]));
                    if (p != null) {
                        p.agregarAtendido(new ClienteAtendido(c[2], c[3]));
                    }
                    break;
                }
                default:
                    // linea desconocida: se ignora
                    break;
            }
        }
        return dto;
    }
}

package servidor_central.persistencia.json;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import servidor_central.datos.ClienteAtendido;
import servidor_central.datos.PuestoInfo;
import servidor_central.datos.Turno;
import servidor_central.interfaces.IEstrategiaEncriptacion;
import servidor_central.interfaces.factory.IPersistenciaLector;
import servidor_central.persistencia.SelloPersistencia;
import servidor_central.persistencia.dto.ServerDTO;

public class LectorJSON implements IPersistenciaLector {

    private final IEstrategiaEncriptacion encriptacion;

    public LectorJSON() {
        this(null);
    }

    public LectorJSON(IEstrategiaEncriptacion encriptacion) {
        this.encriptacion = encriptacion;
    }

    @Override
    @SuppressWarnings("unchecked")
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
        // Descifra (si hay) y verifica el sello: si la clave/cifrado no coincide,
        // lanza ClaveInvalidaException y el arranque se aborta.
        contenido = SelloPersistencia.descifrarYVerificar(contenido, encriptacion);
        if (contenido.isBlank()) {
            return dto;
        }

        Map<String, Object> raiz = (Map<String, Object>) new JsonParser(contenido).parse();

        for (Object o : lista(raiz.get("puestosActivos"))) {
            Map<String, Object> p = (Map<String, Object>) o;
            String host = texto(p.get("hostRemoto"));
            PuestoInfo puesto = new PuestoInfo(
                    entero(p.get("idPuesto")),
                    host.isEmpty() ? null : host);
            for (Object a : lista(p.get("historialAtendidos"))) {
                Map<String, Object> at = (Map<String, Object>) a;
                puesto.agregarAtendido(new ClienteAtendido(
                        texto(at.get("dni")), texto(at.get("hora"))));
            }
            dto.agregarPuesto(puesto);
        }
        for (Object o : lista(raiz.get("colaEspera"))) {
            Map<String, Object> t = (Map<String, Object>) o;
            dto.agregarTurnoEnEspera(new Turno(
                    texto(t.get("dni")),
                    entero(t.get("intentosLlamados")),
                    null));
        }
        for (Object o : lista(raiz.get("turnosEnPuesto"))) {
            Map<String, Object> t = (Map<String, Object>) o;
            int idPuesto = entero(t.get("idPuesto"));
            dto.asignarTurnoAPuesto(idPuesto, new Turno(
                    texto(t.get("dni")),
                    entero(t.get("intentosLlamados")),
                    idPuesto));
        }
        for (Object o : lista(raiz.get("historialLlamados"))) {
            Map<String, Object> t = (Map<String, Object>) o;
            dto.agregarAlHistorial(new Turno(
                    texto(t.get("dni")),
                    1,
                    entero(t.get("idPuesto"))));
        }
        return dto;
    }

    @SuppressWarnings("unchecked")
    private static List<Object> lista(Object o) {
        return o instanceof List ? (List<Object>) o : new ArrayList<>();
    }

    private static int entero(Object o) {
        return ((Number) o).intValue();
    }

    private static String texto(Object o) {
        return o == null ? "" : o.toString();
    }

    /** Parser JSON recursivo minimo (objetos, arrays, strings, numeros, bool, null). */
    private static final class JsonParser {
        private final String s;
        private int i;

        JsonParser(String s) {
            this.s = s;
        }

        Object parse() {
            skipWs();
            return parseValue();
        }

        private Object parseValue() {
            skipWs();
            char c = s.charAt(i);
            switch (c) {
                case '{': return parseObject();
                case '[': return parseArray();
                case '"': return parseString();
                case 't': expect("true");  return Boolean.TRUE;
                case 'f': expect("false"); return Boolean.FALSE;
                case 'n': expect("null");  return null;
                default:  return parseNumber();
            }
        }

        private Map<String, Object> parseObject() {
            Map<String, Object> m = new LinkedHashMap<>();
            i++; // '{'
            skipWs();
            if (s.charAt(i) == '}') { i++; return m; }
            while (true) {
                skipWs();
                String key = parseString();
                skipWs();
                i++; // ':'
                m.put(key, parseValue());
                skipWs();
                char c = s.charAt(i++);
                if (c == '}') break; // si no, era ','
            }
            return m;
        }

        private List<Object> parseArray() {
            List<Object> l = new ArrayList<>();
            i++; // '['
            skipWs();
            if (s.charAt(i) == ']') { i++; return l; }
            while (true) {
                l.add(parseValue());
                skipWs();
                char c = s.charAt(i++);
                if (c == ']') break; // si no, era ','
            }
            return l;
        }

        private String parseString() {
            StringBuilder sb = new StringBuilder();
            i++; // comilla de apertura
            while (true) {
                char c = s.charAt(i++);
                if (c == '"') break;
                if (c == '\\') {
                    char e = s.charAt(i++);
                    switch (e) {
                        case '"':  sb.append('"');  break;
                        case '\\': sb.append('\\'); break;
                        case '/':  sb.append('/');  break;
                        case 'n':  sb.append('\n'); break;
                        case 't':  sb.append('\t'); break;
                        case 'r':  sb.append('\r'); break;
                        case 'b':  sb.append('\b'); break;
                        case 'f':  sb.append('\f'); break;
                        case 'u':
                            sb.append((char) Integer.parseInt(s.substring(i, i + 4), 16));
                            i += 4;
                            break;
                        default:   sb.append(e);
                    }
                } else {
                    sb.append(c);
                }
            }
            return sb.toString();
        }

        private Object parseNumber() {
            int start = i;
            while (i < s.length() && "+-0123456789.eE".indexOf(s.charAt(i)) >= 0) {
                i++;
            }
            String num = s.substring(start, i);
            if (num.indexOf('.') >= 0 || num.indexOf('e') >= 0 || num.indexOf('E') >= 0) {
                return Double.parseDouble(num);
            }
            return Long.parseLong(num);
        }

        private void expect(String w) {
            if (!s.startsWith(w, i)) {
                throw new RuntimeException("JSON invalido: se esperaba '" + w + "' en posicion " + i);
            }
            i += w.length();
        }

        private void skipWs() {
            while (i < s.length() && Character.isWhitespace(s.charAt(i))) {
                i++;
            }
        }
    }
}

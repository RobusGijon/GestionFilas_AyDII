package servidor_central.persistencia.xml;

import java.io.IOException;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import servidor_central.datos.ClienteAtendido;
import servidor_central.datos.PuestoInfo;
import servidor_central.datos.Turno;
import servidor_central.interfaces.IEstrategiaEncriptacion;
import servidor_central.interfaces.factory.IPersistenciaLector;
import servidor_central.persistencia.SelloPersistencia;
import servidor_central.persistencia.dto.ServerDTO;

public class LectorXML implements IPersistenciaLector {

    private final IEstrategiaEncriptacion encriptacion;

    public LectorXML() {
        this(null);
    }

    public LectorXML(IEstrategiaEncriptacion encriptacion) {
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
        if (contenido.isBlank()) {
            return dto;
        }

        try {
            Document doc = DocumentBuilderFactory.newInstance()
                    .newDocumentBuilder()
                    .parse(new InputSource(new StringReader(contenido)));
            doc.getDocumentElement().normalize();

            for (Element e : hijos(doc, "puestosActivos", "puesto")) {
                String host = e.getAttribute("hostRemoto");
                PuestoInfo puesto = new PuestoInfo(
                        Integer.parseInt(e.getAttribute("idPuesto")),
                        host.isEmpty() ? null : host);
                NodeList ats = e.getElementsByTagName("atendido");
                for (int i = 0; i < ats.getLength(); i++) {
                    Element a = (Element) ats.item(i);
                    puesto.agregarAtendido(new ClienteAtendido(
                            a.getAttribute("dni"), a.getAttribute("hora")));
                }
                dto.agregarPuesto(puesto);
            }
            for (Element e : hijos(doc, "colaEspera", "turno")) {
                dto.agregarTurnoEnEspera(new Turno(
                        e.getAttribute("dni"),
                        Integer.parseInt(e.getAttribute("intentosLlamados")),
                        null));
            }
            for (Element e : hijos(doc, "turnosEnPuesto", "turno")) {
                int idPuesto = Integer.parseInt(e.getAttribute("idPuesto"));
                dto.asignarTurnoAPuesto(idPuesto, new Turno(
                        e.getAttribute("dni"),
                        Integer.parseInt(e.getAttribute("intentosLlamados")),
                        idPuesto));
            }
            for (Element e : hijos(doc, "historialLlamados", "llamado")) {
                dto.agregarAlHistorial(new Turno(
                        e.getAttribute("dni"),
                        1,
                        Integer.parseInt(e.getAttribute("idPuesto"))));
            }
        } catch (Exception ex) {
            throw new RuntimeException("No se pudo leer el estado XML de " + nombreArchivo, ex);
        }
        return dto;
    }

    /** Devuelve los elementos &lt;item&gt; contenidos en el primer &lt;contenedor&gt;. */
    private static java.util.List<Element> hijos(Document doc, String contenedor, String item) {
        java.util.List<Element> out = new java.util.ArrayList<>();
        NodeList conts = doc.getElementsByTagName(contenedor);
        if (conts.getLength() == 0) {
            return out;
        }
        NodeList items = ((Element) conts.item(0)).getElementsByTagName(item);
        for (int i = 0; i < items.getLength(); i++) {
            Node n = items.item(i);
            if (n.getNodeType() == Node.ELEMENT_NODE) {
                out.add((Element) n);
            }
        }
        return out;
    }
}

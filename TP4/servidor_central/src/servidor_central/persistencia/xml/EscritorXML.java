package servidor_central.persistencia.xml;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import servidor_central.datos.ClienteAtendido;
import servidor_central.datos.PuestoInfo;
import servidor_central.datos.Turno;
import servidor_central.interfaces.IEstrategiaEncriptacion;
import servidor_central.interfaces.factory.IPersistenciaEscritor;
import servidor_central.persistencia.EscrituraAtomica;
import servidor_central.persistencia.SelloPersistencia;
import servidor_central.persistencia.dto.ServerDTO;

/**
 * Persiste el {@link ServerDTO} en XML usando el DOM del JDK:
 * <pre>
 * &lt;server&gt;
 *   &lt;puestosActivos&gt;
 *     &lt;puesto idPuesto="1" hostRemoto="127.0.0.1:5000"/&gt;
 *   &lt;/puestosActivos&gt;
 *   &lt;colaEspera&gt;
 *     &lt;turno dni="123" intentosLlamados="1"/&gt;
 *   &lt;/colaEspera&gt;
 *   &lt;turnosEnPuesto&gt;
 *     &lt;turno idPuesto="2" dni="456" intentosLlamados="2"/&gt;
 *   &lt;/turnosEnPuesto&gt;
 * &lt;/server&gt;
 * </pre>
 *
 * Con encriptacion el contenido se cifra y se guarda en {@code data/encriptada};
 * sin encriptacion se guarda en claro en {@code data/default}.
 */
public class EscritorXML implements IPersistenciaEscritor {

    private final IEstrategiaEncriptacion encriptacion;

    public EscritorXML() {
        this(null);
    }

    public EscritorXML(IEstrategiaEncriptacion encriptacion) {
        this.encriptacion = encriptacion;
    }

    @Override
    public void guardar(String nombreArchivo, ServerDTO estado) {
        try {
            Document doc = DocumentBuilderFactory.newInstance()
                    .newDocumentBuilder().newDocument();

            Element raiz = doc.createElement("server");
            // Sello de integridad embebido como atributo: el archivo sigue siendo
            // XML valido y el lector lo ignora (solo verifica su presencia).
            raiz.setAttribute("sello", SelloPersistencia.SELLO);
            doc.appendChild(raiz);

            Element puestos = doc.createElement("puestosActivos");
            raiz.appendChild(puestos);
            for (PuestoInfo p : estado.getPuestosActivos()) {
                Element e = doc.createElement("puesto");
                e.setAttribute("idPuesto", String.valueOf(p.getIdPuesto()));
                e.setAttribute("hostRemoto", p.getHostRemoto() == null ? "" : p.getHostRemoto());
                for (ClienteAtendido c : p.getHistorialAtendidos()) {
                    Element a = doc.createElement("atendido");
                    a.setAttribute("dni", c.getDni());
                    a.setAttribute("hora", c.getHora());
                    e.appendChild(a);
                }
                puestos.appendChild(e);
            }

            Element cola = doc.createElement("colaEspera");
            raiz.appendChild(cola);
            for (Turno t : estado.getColaEspera()) {
                Element e = doc.createElement("turno");
                e.setAttribute("dni", t.getDni());
                e.setAttribute("intentosLlamados", String.valueOf(t.getIntentosLlamados()));
                cola.appendChild(e);
            }

            Element llamados = doc.createElement("turnosEnPuesto");
            raiz.appendChild(llamados);
            for (Map.Entry<Integer, Turno> entry : estado.getTurnosEnPuesto().entrySet()) {
                Turno t = entry.getValue();
                Element e = doc.createElement("turno");
                e.setAttribute("idPuesto", String.valueOf(entry.getKey()));
                e.setAttribute("dni", t.getDni());
                e.setAttribute("intentosLlamados", String.valueOf(t.getIntentosLlamados()));
                llamados.appendChild(e);
            }

            Element historial = doc.createElement("historialLlamados");
            raiz.appendChild(historial);
            for (Turno t : estado.getHistorialLlamados()) {
                Element e = doc.createElement("llamado");
                e.setAttribute("idPuesto",
                        String.valueOf(t.getIdPuestoAsignado() == null ? 0 : t.getIdPuestoAsignado()));
                e.setAttribute("dni", t.getDni());
                historial.appendChild(e);
            }

            // Serializamos a String para poder cifrarlo antes de escribir.
            Transformer tr = TransformerFactory.newInstance().newTransformer();
            tr.setOutputProperty(OutputKeys.INDENT, "yes");
            tr.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            StringWriter sw = new StringWriter();
            tr.transform(new DOMSource(doc), new StreamResult(sw));

            String contenido = SelloPersistencia.cifrar(sw.toString(), encriptacion);

            EscrituraAtomica.escribir(Path.of("data", nombreArchivo), contenido);
        } catch (IOException ex) {
            throw new UncheckedIOException("No se pudo guardar el estado en " + nombreArchivo, ex);
        } catch (Exception ex) {
            throw new RuntimeException("Error serializando el estado a XML en " + nombreArchivo, ex);
        }
    }
}

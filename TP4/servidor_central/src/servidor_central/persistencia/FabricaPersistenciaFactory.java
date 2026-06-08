package servidor_central.persistencia;

import servidor_central.interfaces.factory.IFabricaPersistencia;
import servidor_central.persistencia.json.FabricaJSON;
import servidor_central.persistencia.txt.FabricaTXT;
import servidor_central.persistencia.xml.FabricaXML;

/**
 * Factory Method que selecciona la fabrica concreta de persistencia (Abstract
 * Factory) segun el formato configurado: JSON, XML o texto plano. Centraliza la
 * decision para que {@code Main} no dependa de las fabricas concretas y permite
 * cambiar el formato de almacenamiento sin recompilar (via flag {@code --formato}).
 */
public final class FabricaPersistenciaFactory {

    private FabricaPersistenciaFactory() {}

    public static final String FORMATO_POR_DEFECTO = "txt";

    /**
     * Crea la fabrica de persistencia para el formato indicado.
     *
     * @param formato uno de {@code json}, {@code xml} o {@code txt} (case-insensitive).
     * @throws IllegalArgumentException si el formato no es reconocido.
     */
    public static IFabricaPersistencia crear(String formato) {
        if (formato == null) {
            formato = FORMATO_POR_DEFECTO;
        }
        switch (formato.toLowerCase()) {
            case "json": return new FabricaJSON();
            case "xml":  return new FabricaXML();
            case "txt":  return new FabricaTXT();
            default:
                throw new IllegalArgumentException(
                        "formato de persistencia invalido: " + formato + " (use json|xml|txt)");
        }
    }
}

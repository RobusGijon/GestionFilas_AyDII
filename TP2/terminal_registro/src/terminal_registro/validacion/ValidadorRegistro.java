package terminal_registro.validacion;

public class ValidadorRegistro {

    public static boolean validaDNI(String dni) {
        if (dni == null || dni.isEmpty()) {
            return false;
        }
        return dni.matches("\\d{8}");
    }
}
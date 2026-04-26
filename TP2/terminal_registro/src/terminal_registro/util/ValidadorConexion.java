package terminal_registro.util;

import java.util.regex.Pattern;

public class ValidadorConexion {

    private static final String IPV4_REGEX =
        "^(([0-9]|[1-9][0-9]|1[0-9][0-9]|2[0-4][0-9]|25[0-5])(\\.(?!$)|$)){4}$";
    private static final Pattern IPV4_PATTERN = Pattern.compile(IPV4_REGEX);

    public static boolean isValidIPv4(String ip) {
        return ip != null && IPV4_PATTERN.matcher(ip).matches();
    }

    public static boolean isValidPuerto(String puerto) {
        if (puerto == null || puerto.isEmpty()) {
            return false;
        }
        try {
            int p = Integer.parseInt(puerto);
            return p >= 0 && p <= 65535;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
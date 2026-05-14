package servidor_central.disponibilidad;

public final class DireccionPar {

    private final String ip;
    private final int puerto;

    public DireccionPar(String ip, int puerto) {
        if (ip == null || ip.isEmpty()) {
            throw new IllegalArgumentException("ip vacia");
        }
        if (puerto < 0 || puerto > 65535) {
            throw new IllegalArgumentException("puerto fuera de rango: " + puerto);
        }
        this.ip = ip;
        this.puerto = puerto;
    }

    public String getIp()     { return ip; }
    public int    getPuerto() { return puerto; }

    public static DireccionPar parse(String s) {
        if (s == null || s.isEmpty()) {
            throw new IllegalArgumentException("--par vacio");
        }
        int idx = s.lastIndexOf(':');
        if (idx <= 0 || idx == s.length() - 1) {
            throw new IllegalArgumentException("--par debe tener formato ip:puerto, recibido: " + s);
        }
        String ip = s.substring(0, idx);
        int puerto;
        try {
            puerto = Integer.parseInt(s.substring(idx + 1));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("puerto invalido en --par: " + s);
        }
        return new DireccionPar(ip, puerto);
    }

    @Override
    public String toString() {
        return ip + ":" + puerto;
    }
}

package servidor_central.protocolo.mensajes;

import java.util.Objects;

public final class Direccion {

    private final String ip;
    private final int puerto;

    public Direccion(String ip, int puerto) {
        if (ip == null || ip.isEmpty()) {
            throw new IllegalArgumentException("ip vacia");
        }
        if (puerto < 0 || puerto > 65535) {
            throw new IllegalArgumentException("puerto fuera de rango: " + puerto);
        }
        this.ip = ip;
        this.puerto = puerto;
    }

    public String getIp() {
        return ip;
    }

    public int getPuerto() {
        return puerto;
    }

    /** Parsea una direccion con formato {@code ip:puerto}. */
    public static Direccion parse(String s) {
        if (s == null || s.isEmpty()) {
            throw new IllegalArgumentException("direccion vacia");
        }
        int idx = s.lastIndexOf(':');
        if (idx <= 0 || idx == s.length() - 1) {
            throw new IllegalArgumentException("debe tener formato ip:puerto, recibido: " + s);
        }
        String ip = s.substring(0, idx);
        int puerto;
        try {
            puerto = Integer.parseInt(s.substring(idx + 1));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("puerto invalido en direccion: " + s);
        }
        return new Direccion(ip, puerto);
    }

    @Override
    public String toString() {
        return ip + ":" + puerto;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof Direccion))
            return false;
        Direccion d = (Direccion) o;
        return this.puerto == d.puerto && this.ip.equals(d.ip);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ip, puerto);
    }

}

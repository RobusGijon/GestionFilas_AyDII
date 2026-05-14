package pantalla.disponibilidad;

import java.util.Objects;

public final class Direccion {

    private final String ip;
    private final int puerto;

    public Direccion(String ip, int puerto) {
        this.ip = ip;
        this.puerto = puerto;
    }

    public String getIp()     { return ip; }
    public int    getPuerto() { return puerto; }

    @Override
    public String toString() {
        return ip + ":" + puerto;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Direccion)) return false;
        Direccion d = (Direccion) o;
        return this.puerto == d.puerto && this.ip.equals(d.ip);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ip, puerto);
    }
}

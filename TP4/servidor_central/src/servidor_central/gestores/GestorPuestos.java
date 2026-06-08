package servidor_central.gestores;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import servidor_central.datos.ClienteAtendido;
import servidor_central.datos.PuestoInfo;
import servidor_central.interfaces.IGestorPuestos;

public class GestorPuestos implements IGestorPuestos {

    private final List<PuestoInfo> activos = new ArrayList<>();
    private int proximoId = 1;

    /**
     * Conexiones vivas por puesto (request/response + suscripcion). Un puesto se
     * considera ASIGNADO mientras tenga al menos una conexion viva; DESASIGNADO
     * cuando llega a cero. Es estado de runtime: NO se persiste (decision D1), por
     * eso al restaurar todos los puestos arrancan en cero -> DESASIGNADO.
     */
    private final Map<Integer, Integer> conexionesVivas = new HashMap<>();

    @Override
    public PuestoInfo registrarPuesto(String hostRemoto) {
        PuestoInfo p = new PuestoInfo(proximoId++, hostRemoto);
        activos.add(p);
        return p;
    }

    @Override
    public void eliminarPuesto(int idPuesto) {
        Iterator<PuestoInfo> it = activos.iterator();
        while (it.hasNext()) {
            if (it.next().getIdPuesto() == idPuesto) {
                it.remove();
                conexionesVivas.remove(idPuesto);
                return;
            }
        }
    }

    @Override
    public boolean existe(int idPuesto) {
        return obtener(idPuesto) != null;
    }

    @Override
    public PuestoInfo obtener(int idPuesto) {
        for (PuestoInfo p : activos) {
            if (p.getIdPuesto() == idPuesto) return p;
        }
        return null;
    }

    @Override
    public List<PuestoInfo> snapshot() {
        return new ArrayList<>(activos);
    }

    @Override
    public int cantidad() {
        return activos.size();
    }

    @Override
    public void registrarPuestoConId(int idPuesto, String hostRemoto) {
        activos.add(new PuestoInfo(idPuesto, hostRemoto));
        if (idPuesto >= proximoId) {
            proximoId = idPuesto + 1;
        }
    }

    // -------- Estado de conexion (ASIGNADO / DESASIGNADO) --------

    @Override
    public void marcarConexion(int idPuesto) {
        conexionesVivas.merge(idPuesto, 1, Integer::sum);
    }

    @Override
    public void marcarDesconexion(int idPuesto) {
        Integer n = conexionesVivas.get(idPuesto);
        if (n == null) {
            return;
        }
        if (n <= 1) {
            conexionesVivas.remove(idPuesto);
        } else {
            conexionesVivas.put(idPuesto, n - 1);
        }
    }

    @Override
    public boolean estaAsignado(int idPuesto) {
        return conexionesVivas.getOrDefault(idPuesto, 0) > 0;
    }

    @Override
    public PuestoInfo reclamarPorId(int idPuesto) {
        PuestoInfo p = obtener(idPuesto);
        if (p != null && !estaAsignado(idPuesto)) {
            return p;
        }
        return null;
    }

    @Override
    public PuestoInfo reclamarDesasignado(Set<Integer> idsConTurno) {
        PuestoInfo mejor = null;
        boolean mejorConTurno = false;
        for (PuestoInfo p : activos) {
            if (estaAsignado(p.getIdPuesto())) {
                continue;
            }
            boolean conTurno = idsConTurno != null && idsConTurno.contains(p.getIdPuesto());
            boolean elegir = mejor == null
                    || (conTurno && !mejorConTurno)
                    || (conTurno == mejorConTurno && p.getIdPuesto() < mejor.getIdPuesto());
            if (elegir) {
                mejor = p;
                mejorConTurno = conTurno;
            }
        }
        return mejor;
    }

    // -------- Historial de atendidos por puesto --------

    @Override
    public void agregarAtendido(int idPuesto, ClienteAtendido c) {
        PuestoInfo p = obtener(idPuesto);
        if (p != null) {
            p.agregarAtendido(c);
        }
    }

    @Override
    public List<ClienteAtendido> historialDe(int idPuesto) {
        PuestoInfo p = obtener(idPuesto);
        return p == null ? new ArrayList<>() : new ArrayList<>(p.getHistorialAtendidos());
    }

    @Override
    public int getProximoId() {
        return proximoId;
    }

    @Override
    public void setProximoId(int proximoId) {
        this.proximoId = proximoId;
    }

    @Override
    public void limpiar() {
        activos.clear();
        conexionesVivas.clear();
        proximoId = 1;
    }
}

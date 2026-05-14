package servidor_central.gestores;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import servidor_central.datos.PuestoInfo;
import servidor_central.interfaces.IGestorPuestos;

public class GestorPuestos implements IGestorPuestos {

    private final List<PuestoInfo> activos = new ArrayList<>();
    private int proximoId = 1;

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
                return;
            }
        }
    }

    @Override
    public boolean existe(int idPuesto) {
        for (PuestoInfo p : activos) {
            if (p.getIdPuesto() == idPuesto) return true;
        }
        return false;
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
        proximoId = 1;
    }
}

package variables;

import sincronizador.SincronizadorSocket;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Servidor {
    public static final int SERVIDOR_OFFLINE = 0;
    public static final int SERVIDOR_ONLINE = 1;
    public static final int SERVIDOR_SAVING = 2;
    private final int _puerto;
    private final int _id;
    private final Map<String, Integer> _ips = new ConcurrentHashMap<>();
    private int _estado;
    private String _IP = "127.0.0.1";
    private SincronizadorSocket _conector;
    private int _conectados;
    private int _prioridad;

    public Servidor(final int id, final int puerto, final int estado) {
        _id = id;
        _puerto = puerto;
        _estado = estado;
    }

    public void setCantidadIp(String ip, int cant) {
        _ips.put(ip, cant);
    }

    public int getCantidadPorIP(String ip) {
        if (_ips.get(ip) == null) {
            return 0;
        }
        return _ips.get(ip);
    }

    public String getIP() {
        return _IP;
    }

    public void setIP(String ip) {
        _IP = ip;
    }

    public int getID() {
        return _id;
    }

    public int getPuerto() {
        return _puerto;
    }

    public int getEstado() {
        return _estado;
    }

    public void setEstado(final int estado) {
        _estado = estado;
    }

    public SincronizadorSocket getConector() {
        return _conector;
    }

    public void setConector(final SincronizadorSocket con) {
        _conector = con;
    }

    public int getConectados() {
        return _conectados;
    }

    public void setConectados(final int conectados) {
        _conectados = conectados;
    }

    public void setPrioridad(int prioridad) {
        _prioridad = prioridad;
    }

    public String getStringParaAH() {
        boolean _puedeLoguear = true;
        return _id + ";" + _estado + ";" + _prioridad + ";" + (_puedeLoguear ? 1 : 0);
    }
}

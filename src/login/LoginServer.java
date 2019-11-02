package login;

import estaticos.GestorSQL;
import estaticos.GestorSalida;
import estaticos.MainMultiservidor;
import estaticos.Mundo;
import variables.Servidor;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class LoginServer implements Runnable {
    public static final Map<String, Long> Tiempos = new TreeMap<>();
    private static final CopyOnWriteArrayList<LoginSocket> _clientesEscogerServer = new CopyOnWriteArrayList<>();
    private static final CopyOnWriteArrayList<LoginSocket> _clientes = new CopyOnWriteArrayList<>();
    private static final Map<String, ArrayList<LoginSocket>> _IpsClientes = new ConcurrentHashMap<>();
    private static final int[] _ataques = new int[3];
    private static ServerSocket _serverSocket;
    private static byte _j = 0;
    private static int _alterna = 0;
    private static long _tiempoBan1 = 0, _tiempoBan2 = 0, _segundosON = 0;
    private static String _primeraIp = "", _segundaIp = "", _terceraIp = "";
    private static boolean _ban = true;
    private static boolean BLOQUEADO;

    public LoginServer() {
        try {
            if (MainMultiservidor.PARAM_ANTI_DDOS) {
                Timer cuentaRegresiva = new Timer();
                cuentaRegresiva.schedule(new TimerTask() {
                    public void run() {
                        _segundosON += 1;
                        new AntiDDos();
                    }
                }, 1000, 1000);
            }
            Timer autoSelect = new Timer();
            autoSelect.schedule(new TimerTask() {
                public void run() {
                    GestorSQL.ES_IP_BANEADA("111.222.333.444");// para usar el sql y q no se crashee
                }
            }, 300000, 300000);
            if (MainMultiservidor.SEGUNDOS_INFO_STATUS > 0) {
                Timer infoStatus = new Timer();
                infoStatus.schedule(new TimerTask() {
                    public void run() {
                        MainMultiservidor.infoStatus();
                    }
                }, MainMultiservidor.SEGUNDOS_INFO_STATUS * 1000, MainMultiservidor.SEGUNDOS_INFO_STATUS * 1000);
            }
            if (MainMultiservidor.SEGUNDOS_ESTADISTICAS > 0) {
                Timer estadisticas = new Timer();
                estadisticas.schedule(new TimerTask() {
                    public void run() {
                        MainMultiservidor.escribirEstadisticas();
                    }
                }, MainMultiservidor.SEGUNDOS_ESTADISTICAS * 1000, MainMultiservidor.SEGUNDOS_ESTADISTICAS * 1000);
            }
            Timer estadisticas = new Timer();
            estadisticas.schedule(new TimerTask() {
                public void run() {
                    refreshServersEstado();
                }
            }, 30 * 1000, 30 * 1000);
            _serverSocket = new ServerSocket(MainMultiservidor.PUERTO_MULTISERVIDOR);
            Thread _thread = new Thread(this);
            _thread.setDaemon(true);
            _thread.start();
            System.out.println("\n ----- Multiservidor Abierto Puerto " + MainMultiservidor.PUERTO_MULTISERVIDOR
                    + " ----- \n");
        } catch (final IOException e) {
            e.printStackTrace();
            MainMultiservidor.escribirLog("ERROR AL CREAR EL SERVIDOR GENERAL" + e.toString());
            System.exit(1);
        }
    }

    public static void enviarPacketConexionServidor(Servidor servidor) {
        for (LoginSocket cliente : _clientes) {
            new PacketConexion(cliente, servidor);
        }
    }

    public static void refreshServersEstado() {
        for (final LoginSocket cliente : _clientesEscogerServer) {
            try {
                GestorSalida.ENVIAR_AH_ESTADO_SERVIDORES(cliente.getOut());
            } catch (final Exception e) {
                _clientesEscogerServer.remove(cliente);
            }
        }
    }

    public static void addCliente(LoginSocket cliente) {
        if (cliente == null) {
            return;
        }
        addIPsClientes(cliente);
        _clientes.add(cliente);
    }

    public static void borrarCliente(final LoginSocket cliente) {
        if (cliente == null) {
            return;
        }
        borrarIPsClientes(cliente);
        _clientes.remove(cliente);
    }

    public static void borrarEscogerServer(final LoginSocket cliente) {
        _clientesEscogerServer.remove(cliente);
    }

    public static void addEscogerServer(final LoginSocket cliente) {
        if (!_clientesEscogerServer.contains(cliente)) {
            _clientesEscogerServer.add(cliente);
        }
    }

    public static int getCantidadIps(String ip) {
        int cant = getIPsClientes(ip);
        for (final Servidor server : Mundo.Servidores.values()) {
            cant += server.getCantidadPorIP(ip);
        }
        return cant;
    }

    private static void addIPsClientes(LoginSocket s) {
        String ip = s.getActualIP();
        _IpsClientes.computeIfAbsent(ip, k -> new ArrayList<>());
        if (!_IpsClientes.get(ip).contains(s)) {
            _IpsClientes.get(ip).add(s);
        }
    }

    private static void borrarIPsClientes(LoginSocket s) {
        String ip = s.getActualIP();
        if (_IpsClientes.get(ip) == null) {
            return;
        }
        _IpsClientes.get(ip).remove(s);
    }

    private static int getIPsClientes(String ip) {
        if (_IpsClientes.get(ip) == null) {
            return 0;
        }
        return _IpsClientes.get(ip).size();
    }

    public static void cerrarServidorGeneral() {
        try {
            _serverSocket.close();
        } catch (final Exception ignored) {
        }
    }

    public void run() {
        try {
            while (true) {
                final Socket socket = _serverSocket.accept();
                final String ip = socket.getInetAddress().getHostAddress();
                if (MainMultiservidor.PARAM_MOSTRAR_IP) {
                    System.out.println("SE ESTA CONECTANDO LA IP " + ip);
                }
                if (BLOQUEADO || GestorSQL.ES_IP_BANEADA(ip)) {
                    try {
                        socket.close();
                    } catch (final Exception ignored) {
                    }
                    continue;
                }
                if (Tiempos.get(ip) != null
                        && Tiempos.get(ip) + (MainMultiservidor.MILISEGUNDOS_SIG_CONEXION) > System.currentTimeMillis()) {
                    try {
                        socket.close();
                    } catch (final Exception ignored) {
                    }
                    continue;
                }
                Tiempos.put(ip, System.currentTimeMillis());
                if (MainMultiservidor.PARAM_ANTI_DDOS) {
                    _ataques[_j]++;
                    _alterna += 1;
                    if (_alterna == 1) {
                        _primeraIp = ip;
                        if (_ban) {
                            _tiempoBan1 = System.currentTimeMillis();
                        } else {
                            _tiempoBan2 = System.currentTimeMillis();
                        }
                        _ban = !_ban;
                    } else if (_alterna == 2) {
                        _segundaIp = ip;
                        if (_ban) {
                            _tiempoBan1 = System.currentTimeMillis();
                        } else {
                            _tiempoBan2 = System.currentTimeMillis();
                        }
                        _ban = !_ban;
                    } else {
                        _terceraIp = ip;
                        _alterna = 0;
                        if (_ban) {
                            _tiempoBan1 = System.currentTimeMillis();
                        } else {
                            _tiempoBan2 = System.currentTimeMillis();
                        }
                        _ban = !_ban;
                    }
                    if (_primeraIp.equals(ip) && _segundaIp.equals(ip) && _terceraIp.equals(ip)
                            && Math.abs(_tiempoBan1 - _tiempoBan2) < 200) {
                        GestorSQL.INSERT_BAN_IP(ip);
                        try {
                            socket.close();
                        } catch (final Exception ignored) {
                        }
                        continue;
                    }
                }
                new LoginSocket(socket);
            }
        } catch (final IOException e) {
            MainMultiservidor.escribirLog("ERROR EN EL LOGIN SERVER");
            e.printStackTrace();
        } finally {
            try {
                MainMultiservidor.escribirLog("CIERRE DEL LOGIN SERVER");
                if (!_serverSocket.isClosed()) {
                    _serverSocket.close();
                }
            } catch (final IOException ignored) {
            }
        }
    }

    static class PacketConexion extends Thread {
        private final LoginSocket _cliente;
        private final Servidor _servidor;

        PacketConexion(LoginSocket cliente, Servidor servidor) {
            _cliente = cliente;
            _servidor = servidor;
            this.setDaemon(true);
            this.start();
        }

        public void run() {
            try {
                if (_servidor.getConector() == null) {
                    return;
                }
                _servidor.getConector().sendPacket(_cliente.getPacketConexion());
                Thread.sleep(1000);
                if (_cliente.getCuenta() == null) {
                    return;
                }
                GestorSalida.ENVIAR_AxK_TIEMPO_ABONADO_NRO_PJS(_cliente.getOut(), _cliente.getCuenta());
            } catch (Exception ignored) {
            }
        }
    }

    static class AntiDDos extends Thread {
        AntiDDos() {
            this.setDaemon(true);
            this.start();
        }

        public void run() {
            if (MainMultiservidor.PARAM_ANTI_DDOS) {
                int _minAtaque = 25;
                if (!BLOQUEADO && _ataques[0] > _minAtaque && _ataques[1] > _minAtaque && _ataques[2] > _minAtaque) {
                    BLOQUEADO = true;
                    System.err.println("EL SERVIDOR ESTA SIENDO ATACADO EN UNOS MINUTOS SE RESTABLECERA A SU ESTADO NORMAL");
                } else if (BLOQUEADO && _ataques[0] < _minAtaque && _ataques[1] < _minAtaque && _ataques[2] < _minAtaque) {
                    BLOQUEADO = false;
                    System.err.println("EL SERVIDOR HA SIDO RESTABLECIDO, ATAQUE TERMINADO");
                }
                _j = (byte) (_segundosON % 3);
                _ataques[_j] = 0;
            }
        }
    }
}

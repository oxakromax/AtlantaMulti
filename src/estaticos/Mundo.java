package estaticos;

import login.LoginSocket;
import variables.Cuenta;
import variables.Servidor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Mundo {
    public static final Map<Integer, Servidor> Servidores = new ConcurrentHashMap<>();
    private static final Map<Integer, Cuenta> Cuentas = new ConcurrentHashMap<>();

    public static void crearMultiServer() {
        System.out.println("Creación del MultiServidor:");
        System.out.print("Cargando las cuentas: ");
        GestorSQL.CARGAR_CUENTAS();
        System.out.println(Cuentas.size() + " cuentas cargadas");
    }

    public static synchronized void addCuenta(final Cuenta cuenta) {
        Cuentas.put(cuenta.getID(), cuenta);
    }

    public static Cuenta getCuenta(final int id) {
        return Cuentas.get(id);
    }

    public static byte getComunidadIdioma(String idioma) {
        switch (idioma.toLowerCase()) {
            case "fr":
            case "ch":
            case "be":
            case "lu":
                return 0;
            case "uk":
            case "ie":
            case "gb":
                return 1;
            case "xx":
                return 2;
            case "de":
            case "at":
            case "li":
                return 3;
            case "es":
            case "ad":
            case "ar":
            case "ck":
            case "mx":
                return 4;
            case "ru":
                return 5;
            case "pt":
            case "br":
                return 6;
            case "nl":
                return 7;
            case "it":
                return 9;
            case "jp":
                return 10;
            default:
                return 99;
        }
    }

    public static String packetParaAH() {
        final StringBuilder str = new StringBuilder();
        for (final Servidor servidor : Servidores.values()) {
            if (str.length() > 0) {
                str.append("|");
            }
            str.append(servidor.getStringParaAH());
        }
        return str.toString();
    }

    public static void enviarPacketsAServidores(final LoginSocket cliente) {
        String packet = cliente.getPacketConexion();
        for (final Servidor servidor : Servidores.values()) {
            try {
                if (servidor.getConector() == null) {
                    continue;
                }
                servidor.getConector().sendPacket(packet);
            } catch (final Exception ignored) {
            }
        }
    }

    public static void enviarCantidadIps(final String IP) {
        String packet = "I" + IP;
        for (final Servidor servidor : Servidores.values()) {
            try {
                if (servidor.getConector() == null) {
                    continue;
                }
                servidor.getConector().sendPacket(packet);
            } catch (final Exception ignored) {
            }
        }
    }

    public static String infoStatus() {
        StringBuilder str = new StringBuilder();
        for (final Servidor server : Servidores.values()) {
            try {
                String s = "\n$server_" + server.getID();
                str.append(s + "_status = " + server.getEstado() + ";");
                str.append(s + "_onlines = " + server.getConectados() + ";\n");
            } catch (final Exception e) {
                e.printStackTrace();
            }
        }
        return str.toString();
    }
}

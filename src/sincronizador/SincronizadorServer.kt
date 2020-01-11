package sincronizador;

import estaticos.MainMultiservidor;

import java.net.ServerSocket;
import java.net.Socket;

public class SincronizadorServer implements Runnable {
    private static ServerSocket _serverSocket;

    public SincronizadorServer() {
        try {
            _serverSocket = new ServerSocket(MainMultiservidor.PUERTO_SINCRONIZADOR);
            Thread _thread = new Thread(this);
            _thread.setDaemon(true);
            _thread.start();
        } catch (Exception e) {
            if (MainMultiservidor.MOSTRAR_SINCRONIZACION) {
                System.out.println("NO SE PUEDE CREAR EL SINCRONIZADOR SERVER");
            }
            e.printStackTrace();
        }
    }

    public void run() {
        try {
            while (true) {
                Socket socket = _serverSocket.accept();
                new SincronizadorSocket(socket);
            }
        } catch (Exception ignored) {
        }
    }
}

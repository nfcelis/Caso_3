import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;

public class BuzonCuarentena {
    private final Map<Integer, MensajeCuarentena> mensajesPendientes = new HashMap<>();
    private boolean sistemaActivo = true;
    private int idContador = 0;
    private boolean finRecibido = false; 

    // Clase auxiliar
    private static class MensajeCuarentena {
        Mensaje mensaje;
        int tiempoRestante;
        MensajeCuarentena(Mensaje mensaje, int tiempo) {
            this.mensaje = mensaje;
            this.tiempoRestante = tiempo;
        }
    }

    public synchronized void depositar(Mensaje mensaje, int tiempoSegundos) {
        if (!sistemaActivo) return;

        // Solo aceptar el primer FIN
        if (mensaje.isEsFin()) {
            if (finRecibido) return;
            finRecibido = true;
            tiempoSegundos = 0;
        }

        MensajeCuarentena mc = new MensajeCuarentena(mensaje, tiempoSegundos);
        mensajesPendientes.put(idContador++, mc);
        System.out.println("[Cuarentena] Depositado " + mensaje +
                " con tiempo=" + tiempoSegundos + "s (total: " + mensajesPendientes.size() + ")");
        notifyAll();
    }

    public synchronized Mensaje extraer() {
        Iterator<Map.Entry<Integer, MensajeCuarentena>> it = mensajesPendientes.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, MensajeCuarentena> e = it.next();
            MensajeCuarentena mc = e.getValue();
            if (mc.tiempoRestante <= 0) {
                it.remove();
                Mensaje msg = mc.mensaje;
                System.out.println("[Cuarentena] Extraído " + msg + " (listo para entrega)");
                return msg;
            }
        }
        return null;
    }

    public synchronized void decrementarTiempos() {
        for (MensajeCuarentena mc : mensajesPendientes.values()) {
            mc.tiempoRestante--;
        }
    }

    // No eliminar mensajes FIN
    public synchronized int eliminarMaliciosos(Random random) {
        int eliminados = 0;
        Iterator<Map.Entry<Integer, MensajeCuarentena>> it = mensajesPendientes.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, MensajeCuarentena> entry = it.next();
            Mensaje msg = entry.getValue().mensaje;
            if (!msg.isEsFin() && (random.nextInt(21) % 7 == 0)) {
                System.out.println("[Cuarentena] Eliminado mensaje MALICIOSO: " + msg);
                it.remove();
                eliminados++;
            }
        }
        return eliminados;
    }

    public synchronized boolean isEmpty() {
        return mensajesPendientes.isEmpty();
    }

    public synchronized void cerrar() {
        sistemaActivo = false;
        notifyAll();
    }
}

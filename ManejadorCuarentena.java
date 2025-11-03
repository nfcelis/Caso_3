import java.util.Random;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;


public class ManejadorCuarentena extends Thread {
    private final int id;
    private final BuzonCuarentena buzonCuarentena;
    private final BuzonEntrega buzonEntrega;
    private final Random random = new Random();
    private final CyclicBarrier barrier;

    private static final int INTERVALO_REVISION = 1000; // Frecuencia para revisar mensajes, (cada 1 segundo)

    public ManejadorCuarentena(int id, BuzonCuarentena cuarentena,
                               BuzonEntrega entrega, CyclicBarrier barrier) {
        super("ManejadorCuarentena-" + id);
        this.id = id;
        this.buzonCuarentena = cuarentena;
        this.buzonEntrega = entrega;
        this.barrier = barrier;
        
    }

    @Override
    public void run() {
        try {
            //Espera a que lleguen todos los hilos, para que esten sincronizados
            barrier.await();
            System.out.println("[Manejador-" + id + "] Iniciando revisión de cuarentena");

            boolean finSolicitado= false;
            while (true) {
                // Avanza el tiempo de cuarentena
                buzonCuarentena.decrementarTiempos();

                // Intenta extraer un mensaje listo
                Mensaje msgListo = buzonCuarentena.extraer();

            if (msgListo != null) {
                     if (msgListo.isEsFin()) {
            
            finSolicitado = true;
            System.out.println("[Manejador-" + id + "] FIN recibido. Drenando cuarentena…");
            } else {
                Thread.yield();
            
            buzonEntrega.depositar(msgListo);
            System.out.println("[Manejador-" + id + "] Liberado de cuarentena: " + msgListo);
            }
            }
                // Eliminar maliciosos (múltiplos de 7)
                int eliminados = buzonCuarentena.eliminarMaliciosos(random);
                if (eliminados > 0) {
                    System.out.println("[Manejador-" + id + "] Eliminados " + eliminados + " mensajes maliciosos");
                }

                if (finSolicitado && buzonCuarentena.isEmpty()) {

                    if (BuzonEntrega.marcarFinGlobalSiNoMarcado()) {
                    System.out.println("[Manejador-" + id + "] FIN  Entrega (Cuarentena vacia)");
                    buzonEntrega.depositar(new Mensaje(99999, -1, false, false, true));
                    }

                System.out.println("[Manejador-" + id + "] Finalizó revisión de cuarentena");
                break;
                }


                // Pausa de 1s (semiactiva)
                Thread.sleep(INTERVALO_REVISION);
            }

            

        } catch (InterruptedException e) {
            System.err.println("[Manejador-" + id + "] Interrumpido: " + e.getMessage());
        } catch (BrokenBarrierException e) {
            System.err.println("[Manejador-" + id + "] Barrera rota: " + e.getMessage());
        }
    }
}

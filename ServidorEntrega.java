import java.util.Random;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;


public class ServidorEntrega extends Thread {
    private final int id;
    private final BuzonEntrega buzonEntrega;
    private final Random random = new Random();
    private final CyclicBarrier barrier;


    public ServidorEntrega(int id, BuzonEntrega buzonEntrega, CyclicBarrier barrier) {
        super("Servidor-" + id); 
        this.id = id;
        this.buzonEntrega = buzonEntrega;
        this.barrier = barrier;
        
    }

    @Override
    public void run() {
        try {
            //Espera a que lleguen todos los hilos
            barrier.await();
            System.out.println("[Servidor-" + id + "] Esperando mensajes...");

            while (true) {
                // Espera activa de lectura
                Mensaje msg = buzonEntrega.extraerNoBloqueante();
                if (msg == null) {
                    if (BuzonEntrega.finGlobalYaEnviado() && buzonEntrega.isEmpty()) {
                    System.out.println("[Servidor-" + id + "] Cola vacía tras FIN global. Cerrando.");
                    break;
                }
                Thread.sleep(10);
                continue;
                }
                if (msg.isEsFin()) {
                    System.out.println("[Servidor-" + id + "] FIN recibido. Cerrando.");
                    break;
                }
                procesarMensaje(msg);
            }

            System.out.println("[Servidor-" + id + "] Finalizó procesamiento");

        } catch (InterruptedException e) {
            System.err.println("[Servidor-" + id + "] Interrumpido: " + e.getMessage());
        } catch (BrokenBarrierException e) {
            System.err.println("[Servidor-" + id + "] Barrera rota: " + e.getMessage());
        }
    }

    //Procesamiento del mensaje
    private void procesarMensaje(Mensaje msg) throws InterruptedException {
        if (msg.isEsInicio()) {
            System.out.println("[Servidor-" + id + "] INICIO de cliente " + msg.getClienteId());
            return;
        }

        // Mensaje normal,se procesa en un tiempo aleatorio
        System.out.println("[Servidor-" + id + "] Procesando " + msg);
        int ciclos = random.nextInt(1000) + 500;
        int suma = 0;
        for (int i = 0; i < ciclos; i++) {
            suma += i * (1 + (i % 7));
            if (suma > 1_000_000) suma %= 1_000_000;
        }
        // Se usa para evitar condiciones de carrera
        Thread.sleep(random.nextInt(50) + 20);
        System.out.println("[Servidor-" + id + "] OK " + msg + " (ciclos=" + ciclos + ")");
    }
}

import java.util.Random;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;


public class ClienteEmisor extends Thread {
    private final int id;
    private final int numMensajes;
    private final BuzonEntrada buzonEntrada;
    private final Random random = new Random();
    private final CyclicBarrier barrier; 
    //CyclicBarrier se usa como recurso utilizado para coordinar etapas del sistema, permite que todos los clientesEmisores lleguen al mismo punto antes de la siguiente fase, es para sincronizar los hilos de los clientes emisores

    public ClienteEmisor(int id, int numMensajes, BuzonEntrada buzonEntrada, CyclicBarrier barrier) {
        super("Cliente-" + id);
        this.id = id;
        this.numMensajes = numMensajes;
        this.buzonEntrada = buzonEntrada;
        this.barrier = barrier;
        
    }

    @Override
    public void run() {
        try {
            //Se detiene hasta que lleguen todos los hilos
            barrier.await();
            System.out.println("[Cliente-" + id + "]  Iniciando envío de mensajes");

            // Se envia el mensaje de inicio
            buzonEntrada.depositar(new Mensaje(id * 10000, id, false, true, false));

            // Se envian n mensajes del Cliente
            for (int i = 0; i < numMensajes; i++) {
                boolean esSpam = random.nextDouble() < 0.3;
                Mensaje m = new Mensaje(id * 10000 + i + 1, id, esSpam, false, false);
                buzonEntrada.depositar(m);
                Thread.sleep(random.nextInt(100) + 50); // Se utiliza para evitar problemas de carrera
            }

            // Se envia el mensaje de fin
            buzonEntrada.depositar(new Mensaje(id * 10000 + 9999, id, false, false, true));

            System.out.println("[Cliente-" + id + "] Finalizó envío de todos los mensajes");

        } catch (InterruptedException e) {
            System.err.println("[Cliente-" + id + "] Interrumpido: " + e.getMessage());
        } catch (BrokenBarrierException e) {
            System.err.println("[Cliente-" + id + "] Barrera rota: " + e.getMessage()); 
    }
}
}
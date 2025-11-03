import java.util.Random;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;


public class FiltroSpam extends Thread {
    private final int id;
    private final BuzonEntrada buzonEntrada;
    private final BuzonCuarentena buzonCuarentena;
    private final BuzonEntrega buzonEntrega;
    private final int numClientesEsperados;
    private final Random random = new Random();
    private final CyclicBarrier barrier;
    private static boolean finCuarentenaEnviado = false; 

    public FiltroSpam(int id, BuzonEntrada entrada, BuzonCuarentena cuarentena,
                      BuzonEntrega entrega, int numClientes, CyclicBarrier barrier) {
        super("Filtro-" + id);
        this.id = id;
        this.buzonEntrada = entrada;
        this.buzonCuarentena = cuarentena;
        this.buzonEntrega = entrega;
        this.numClientesEsperados = numClientes;
        this.barrier = barrier;
        
    }

    @Override
    public void run() {
        try {
            //Espera a que todos los hilos lleguen.
            barrier.await();
            System.out.println("[Filtro-" + id + "] Iniciando análisis de mensajes");

            while (true) {
                //Revisa si ya se recibieron todos los mensajes FIN, y si es asi emite el FIN global
                if (BuzonEntrega.todosLosFinesDeClientes(numClientesEsperados)) {
                    // Si se cumplen condiciones, emitir FIN global (solo una vez)
                    intentarEmitirFinGlobal();

                
                Mensaje msg = buzonEntrada.extraerNoBloqueante();
                if (msg != null) {
                    procesarMensaje(msg);
                    intentarEmitirFinGlobal();
                    Thread.sleep(random.nextInt(50) + 20);
                    continue;
                }

                    // Si FIN global ya salió y la entrada está vacía, este filtro termina.
                    if (BuzonEntrega.finGlobalYaEnviado() && buzonEntrada.isEmpty()) {
                        break;
                    }

                    Thread.sleep(20);
                    continue;
                }

               
                // Espera pasiva: bloquea si no hay mensajes (hasta que lleguen de clientes)
                Mensaje msg = buzonEntrada.extraer();
                if (msg == null) { // cierre inesperado de entrada
                    break;
                }

                procesarMensaje(msg);

                // Semiactiva 
                Thread.yield();
            }

            System.out.println("[Filtro-" + id + "] Finalizó procesamiento");

        } catch (InterruptedException | BrokenBarrierException e) {
            System.err.println("[Filtro-" + id + "] Interrumpido: " + e.getMessage());
        }
    }

    //Comproba si todos los clientes enviaron su fin, y si es asi, manda el FIN global
  private void intentarEmitirFinGlobal() throws InterruptedException {
    if (BuzonEntrega.todosLosFinesDeClientes(numClientesEsperados)
        && buzonEntrada.isEmpty()
        && !finCuarentenaEnviado) {

        finCuarentenaEnviado = true; // Solo el primero lo hace
        System.out.println("[Filtro-" + id + "] FIN global  Cuarentena (una sola vez)");

        buzonCuarentena.depositar(new Mensaje(100000, -1, false, false, true), 0);
        buzonEntrada.cerrar();
    }
}


    //Logica de que hacer con cada mensaje que llegue segun sus condiciones
    private void procesarMensaje(Mensaje msg) throws InterruptedException {
        if (msg.isEsFin()) {
            BuzonEntrega.registrarFinCliente();
            System.out.println("[Filtro-" + id + "]  Visto FIN de cliente " + msg.getClienteId());
        }
        else if (msg.isEsInicio()) {
            System.out.println("[Filtro-" + id + "]  INICIO de cliente " + msg.getClienteId());
        }
        else if (msg.isEsSpam()) {
            int ticks = random.nextInt(11) + 10; //Crea retraso en los hilos, reduce condiciones de carrera
            buzonCuarentena.depositar(msg, ticks);
            System.out.println("[Filtro-" + id + "] A cuarentena: " + msg + " (tiempo=" + ticks + "s)");
        }
        else {
           
            for (int i = 0; i < 50; i++) { /*Espera semiactiva corta para no bloquear el hilo*/ }
            buzonEntrega.depositar(msg);
            System.out.println("[Filtro-" + id + "] A entrega: " + msg);
        }
    }
}

import java.util.LinkedList;
import java.util.Queue;


public class BuzonEntrega {
    
    private final Queue<Mensaje> mensajes = new LinkedList<>();
    private final int capacidadMaxima;
    private boolean sistemaActivo = true;

    // Mensaje de tipo FIN, que indica el fin del sistema 
    private final int numServidoresEsperados;
    private boolean finReplicado = false;

    //Todos los servidores reciben FIN
    private static int  finsDeClientesVistos = 0;
    private static boolean finGlobalEnviado  = false;

    public BuzonEntrega(int capacidadMaxima, int numServidores) {
        this.capacidadMaxima = capacidadMaxima;
        this.numServidoresEsperados = numServidores;
    }
    
    //Metodos estaticos para todos los servidores
    public static synchronized void resetCoordinacion() {
        finsDeClientesVistos = 0;
        finGlobalEnviado = false;
    }
    public static synchronized void registrarFinCliente() {
        finsDeClientesVistos++;
    }
    public static synchronized boolean todosLosFinesDeClientes(int totalClientes) {
        return finsDeClientesVistos >= totalClientes;
    }
  
    public static synchronized boolean marcarFinGlobalSiNoMarcado() {
        if (!finGlobalEnviado) { 
            finGlobalEnviado = true; 
            return true; }
        return false;
    }
    public static synchronized boolean finGlobalYaEnviado() {
        return finGlobalEnviado;
    }

   
    public synchronized void depositar(Mensaje mensaje) throws InterruptedException {
        //Si el buzon esta lleno, el filtro entra en espera
        while (mensajes.size() >= capacidadMaxima && sistemaActivo) {
            wait();
        }
        //Si el sistema no esta activo, retorna porque ya se acabo el programa.
        if (!sistemaActivo) return;

        //Si el mensaje es de FIN, se actualiza la variable y se replica el mensaje para todos los servidores faltantes.
        if (mensaje.isEsFin() && !finReplicado) {
            finReplicado = true;
            mensajes.offer(mensaje);
            for (int i = 1; i < numServidoresEsperados; i++) {
                mensajes.offer(new Mensaje(99999 + i, -1, false, false, true));
            }
            System.out.println("[BuzonEntrega] FIN replicado x" + numServidoresEsperados);
        //Si no es fin, se agrega el mensaje normal
        } else {
            mensajes.offer(mensaje);
            System.out.println("[BuzonEntrega] Depositado " + mensaje + " (total: " + mensajes.size() + ")");
        }
        notifyAll();
    }

    //Lee y si no encuentra,retorna y "revisa" mas tarde o sigue trabajando (No Bloqueante)
    public synchronized Mensaje extraerNoBloqueante() {
        if (mensajes.isEmpty()) return null;
        Mensaje msg = mensajes.poll();
        notifyAll();
        return msg;
    }

    //Lectura normal (Espera pasiva)
    public synchronized Mensaje extraer() throws InterruptedException {
        while (mensajes.isEmpty() && sistemaActivo) {
            wait();
        }
        if (!sistemaActivo && mensajes.isEmpty()) return null;
        Mensaje msg = mensajes.poll();
        notifyAll();
        return msg;
    }

    public synchronized boolean isEmpty() { 
        return mensajes.isEmpty(); 
    }

    public synchronized boolean finReplicado() { 
        return finReplicado; 
    }

    public synchronized void cerrar() {
        sistemaActivo = false;
        notifyAll();
    }
}

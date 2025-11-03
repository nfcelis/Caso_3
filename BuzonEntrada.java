import java.util.LinkedList;
import java.util.Queue;


public class BuzonEntrada {
    //Mensajes implementados con cola FIFO
    private final Queue<Mensaje> mensajes = new LinkedList<>();
    private final int capacidadMaxima;
    private boolean sistemaActivo = true;

    public BuzonEntrada(int capacidadMaxima) {
        this.capacidadMaxima = capacidadMaxima;
    }

    public synchronized void depositar(Mensaje mensaje) throws InterruptedException {
        //Mientras el buzon este lleno y el sistema este activo, el Thread espera
        while (mensajes.size() >= capacidadMaxima && sistemaActivo) {
            wait();
        }
        //Si el sistema ya se cerro, no permite mas, se usa para el final 
        if (!sistemaActivo) return;
        //Notifica el mensaje, y notifica a todos los filtros
        mensajes.offer(mensaje);
        System.out.println("[BuzonEntrada] Cliente " + mensaje.getClienteId() +
                " depositó " + mensaje + " (total: " + mensajes.size() + ")");
        notifyAll();
    }

    public synchronized Mensaje extraer() throws InterruptedException {
        //Mientas que el buzon este vacio y el sistema este activo, el Thread espera
        while (mensajes.isEmpty() && sistemaActivo) {
            wait();
        }
        //Si el sistema ya no esta activo y ya no hay mensajes por sacar, retorna null para salir
        if (!sistemaActivo && mensajes.isEmpty()) return null;

        //Saca mensaje de la cola,notifica para despertar a los clientes que estan esperando, y retorna el mensaje
        Mensaje msg = mensajes.poll();
        notifyAll();
        return msg;
    }

    public synchronized Mensaje extraerNoBloqueante() {
    if (mensajes.isEmpty()) return null;
    Mensaje msg = mensajes.poll();
    notifyAll();
    return msg;
    }

    //Metodos para que otros componentes puedan validar si el buzon es vacio
    public synchronized boolean isEmpty() { 
        return mensajes.isEmpty(); 
    }
    //Metodo para cerrar el buzon
    public synchronized void cerrar() {
        sistemaActivo = false;
        notifyAll();
    }
}

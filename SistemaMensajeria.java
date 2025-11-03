import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CyclicBarrier;


public class SistemaMensajeria {

    //Parametros configurables
    private int numClientes;
    private int numMensajesPorCliente;
    private int numFiltros;
    private int numServidores;
    private int capacidadBuzonEntrada;
    private int capacidadBuzonEntrega;

    //recursos compartidos
    private BuzonEntrada buzonEntrada;
    private BuzonCuarentena buzonCuarentena;
    private BuzonEntrega buzonEntrega;

    // hilos (threads) del sistema
    private List<ClienteEmisor> clientes;
    private List<FiltroSpam> filtros;
    private ManejadorCuarentena manejadorCuarentena;
    private List<ServidorEntrega> servidores;

    //Recurso utilizado para la sincronización de etapas
    private CyclicBarrier barrier;

    public SistemaMensajeria(String archivoConfig) {
        leerConfiguracion(archivoConfig);
    }

    /**
     * Se utiliza un archivo del siguiente tipo para configurar los paranetros
     * Formato:
     * numClientes=4
     * numMensajes=50
     * numFiltros=2
     * numServidores=2
     * capacidadEntrada=10
     * capacidadEntrega=15
     */

    private void leerConfiguracion(String archivo) {
        try (BufferedReader br = new BufferedReader(new FileReader(archivo))) {
            String linea;
            while ((linea = br.readLine()) != null) {
                if (linea.trim().isEmpty() || linea.startsWith("#")) continue;
                String[] partes = linea.split("=");
                if (partes.length != 2) continue;

                String clave = partes[0].trim();
                int valor = Integer.parseInt(partes[1].trim());

                switch (clave) {
                    case "numClientes":        numClientes = valor; break;
                    case "numMensajes":        numMensajesPorCliente = valor; break;
                    case "numFiltros":         numFiltros = valor; break;
                    case "numServidores":      numServidores = valor; break;
                    case "capacidadEntrada":   capacidadBuzonEntrada = valor; break;
                    case "capacidadEntrega":   capacidadBuzonEntrega = valor; break;
                }
            }

            System.out.println("Configuración cargada:");
            System.out.println("   Clientes: " + numClientes);
            System.out.println("   Mensajes por cliente: " + numMensajesPorCliente);
            System.out.println("   Filtros: " + numFiltros);
            System.out.println("   Servidores: " + numServidores);
            System.out.println("   Capacidad buzón entrada: " + capacidadBuzonEntrada);
            System.out.println("   Capacidad buzón entrega: " + capacidadBuzonEntrega);
            System.out.println();

        } catch (IOException e) {
            System.err.println(" Error leyendo configuración: " + e.getMessage());
            System.exit(1);
        }
    }

    public void iniciar() {
        System.out.println(" ========== INICIANDO SISTEMA DE MENSAJERÍA ==========\n");

        // 1) RECURSOS
        System.out.println("Creando buzones compartidos...");
        buzonEntrada    = new BuzonEntrada(capacidadBuzonEntrada);
        buzonCuarentena = new BuzonCuarentena();
        buzonEntrega    = new BuzonEntrega(capacidadBuzonEntrega, numServidores);

        BuzonEntrega.resetCoordinacion();

        // Barrera(CyclicBarrier utilizado para sincronizar las etapas)
        int totalThreads = numClientes + numFiltros + 1 + numServidores;
        barrier = new CyclicBarrier(totalThreads);
        System.out.println("Barrera creada para " + totalThreads + " threads\n");

        // Clientes
        System.out.println("Creando " + numClientes + " clientes emisores...");
        clientes = new ArrayList<>();
        for (int i = 0; i < numClientes; i++) {
            clientes.add(new ClienteEmisor(i, numMensajesPorCliente, buzonEntrada, barrier));
        }

        // Filtros
        System.out.println("Creando " + numFiltros + " filtros de spam...");
        filtros = new ArrayList<>();
        for (int i = 0; i < numFiltros; i++) {
            filtros.add(new FiltroSpam(i, buzonEntrada, buzonCuarentena, buzonEntrega, numClientes, barrier));
        }

        // Manejador de cuarentena
        System.out.println("Creando manejador de cuarentena...");
        manejadorCuarentena = new ManejadorCuarentena(0, buzonCuarentena, buzonEntrega, barrier);

        // Servidores
        System.out.println("Creando " + numServidores + " servidores de entrega...");
        servidores = new ArrayList<>();
        for (int i = 0; i < numServidores; i++) {
            servidores.add(new ServidorEntrega(i, buzonEntrega, barrier));
        }

        System.out.println("\n Todos los componentes creados\n");

        // Inicio
        System.out.println(" INICIANDO TODOS LOS THREADS...\n");
        clientes.forEach(Thread::start);
        filtros.forEach(Thread::start);
        manejadorCuarentena.start();
        servidores.forEach(Thread::start);

        System.out.println("Sistema en ejecución. Esperando finalización...\n");

        try {
            //clientes
            for (ClienteEmisor c : clientes) c.join();
            System.out.println("\n Todos los clientes finalizaron\n");

            // filtros
            for (FiltroSpam f : filtros) f.join();
            System.out.println(" Todos los filtros finalizaron\n");

            // manejador
            manejadorCuarentena.join();
            System.out.println("Manejador de cuarentena finalizado\n");

            //servidores
            for (ServidorEntrega s : servidores) s.join();
            System.out.println("Todos los servidores finalizaron\n");

            // cerrar los buzones
            buzonEntrada.cerrar();
            buzonCuarentena.cerrar();
            buzonEntrega.cerrar();

            System.out.println(" ========== SISTEMA FINALIZADO EXITOSAMENTE ==========");

        } catch (InterruptedException e) {
            System.err.println(" Error durante la ejecución: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Uso: java SistemaMensajeria <archivo_configuracion>");
            System.err.println("Ejemplo: java SistemaMensajeria config.txt");
            System.exit(1);
        }
        SistemaMensajeria sistema = new SistemaMensajeria(args[0]);
        sistema.iniciar();
    }
}

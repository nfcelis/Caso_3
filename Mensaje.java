
//Representa cada mensaje en el sistema y toda su información 
public class Mensaje {
    private int id;
    private int clienteId;
    private boolean esSpam;
    private boolean esInicio;
    private boolean esFin;
    private long timestamp;

    public Mensaje(int id, int clienteId, boolean esSpam, boolean esInicio, boolean esFin) {
        this.id = id;
        this.clienteId = clienteId;
        this.esSpam = esSpam;
        this.esInicio = esInicio;
        this.esFin = esFin;
        this.timestamp = System.currentTimeMillis();
    }

    public int getId() { 
        return id; 
    }
    public int getClienteId() { 
        return clienteId; 
    }
    public boolean isEsSpam() { 
        return esSpam; 
    }
    public boolean isEsInicio() { 
        return esInicio; 
    }
    public boolean isEsFin() { 
        return esFin; 
    }
    public long getTimestamp() { return timestamp; }

    //Metodo para imprimir logs 
    @Override
    public String toString() {
        String tipo;
        if (esInicio)      tipo = "INICIO";
        else if (esFin)    tipo = "FIN";
        else if (esSpam)   tipo = "SPAM";
        else               tipo = "VALIDO";
        return String.format("Msg[id=%d, cliente=%d, tipo=%s]", id, clienteId, tipo);
    }
}

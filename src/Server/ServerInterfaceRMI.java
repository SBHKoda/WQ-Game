package Server;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ServerInterfaceRMI extends Remote {
    //Registrazione al Gioco
    boolean registra_utente(String username, String password) throws RemoteException;

}

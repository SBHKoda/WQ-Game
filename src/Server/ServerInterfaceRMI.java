package Server;

import Client.NotifyInterfaceRMI;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ServerInterfaceRMI extends Remote {
    //Registrazione al Gioco
    boolean registra_utente(String username, String password) throws RemoteException;
    //Registrazione ala callback per le notifiche online
    void registerForCallback(NotifyInterfaceRMI ClientInterface, String username) throws RemoteException;

}

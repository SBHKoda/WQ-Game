package Server;

import Client.NotifyInterfaceRMI;

import java.net.InetAddress;

public class User {
    private String username;
    private String password;
    private boolean isOnline;

    private InetAddress address;

    private boolean isRegisteredForCallback = false;

    private NotifyInterfaceRMI client;

    public User(String username, String password) {
        this.username = username;
        this.password = password;

        //Appena creato un utente e` nello stato offline
        this.isOnline = false;

    }

    public boolean checkPassword(String password){
        return this.password.equals(password);
    }
    public boolean checkOnlineStatus(){
        return this.isOnline;
    }
    public void setOnline(){
        this.isOnline = true;
    }
    public void setOffline(){
        this.isOnline = false;
    }
    public NotifyInterfaceRMI getClientCallback(){
        return client;
    }
    public void setClient(NotifyInterfaceRMI client){
        this.client = client;
    }
    public boolean getRegisteredForCallback(){
        return this.isRegisteredForCallback;
    }
    public void setRegisteredForCallback(){
        this.isRegisteredForCallback = true;
    }

    public void setAddress(InetAddress multicastAddress){
        this.address = multicastAddress;
    }
    public InetAddress getAddress(){
        return this.address;
    }
}

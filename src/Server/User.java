package Server;

import Client.NotifyInterfaceRMI;

public class User {
    private String username;
    private String password;
    private boolean isOnline;
    private int punteggioTotale = 0;
    private int punteggioUltimaPartita = 0;

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
    public void setPunteggio(int puntiOttenuti){
        punteggioTotale += puntiOttenuti;
        punteggioUltimaPartita = puntiOttenuti;
    }
    public int getPunteggioTotale(){
        return this.punteggioTotale;
    }
    public int getPunteggioUltimaPartita(){
        return this.punteggioUltimaPartita;
    }

}

package Server;

public class User {
    //Informazioni necessarie per il giocatore
    private String username;
    private String password;
    //Flag necessari per far sapere al server se il giocatore e` online e se e` gia impegnato in una sfida
    private boolean isOnline;
    private boolean inSfida;
    //Punteggi
    private int punteggioTotale = 0;
    private int punteggioPartita = 0;

    public User(String username, String password) {
        this.username = username;
        this.password = password;

        //Appena creato un utente e` nello stato offline e non in sfida
        this.isOnline = false;
        this.inSfida = false;
    }
    //Metodo per controllare se la password coincide
    public boolean checkPassword(String password){
        return this.password.equals(password);
    }
    //Metodo che ritorna true se il giocatore e` online false altrimenti
    public boolean checkOnlineStatus(){
        return this.isOnline;
    }
    //Metodi per impostare lo stato di un giocatore (online-offline)
    public void setOnline(){
        this.isOnline = true;
    }
    public void setOffline(){
        this.isOnline = false;
    }

    //Metodi per settare, ripristinare(punteggio ottenuto dal file JSON), il punteggio totale del giocatore
    public void setPunteggio(int puntiOttenuti){
        punteggioTotale += puntiOttenuti;
        punteggioPartita = puntiOttenuti;
    }
    public void ripristinaPunteggioTotale(int punteggio){
        this.punteggioTotale = punteggio;
    }
    public int getPunteggioTotale(){
        return this.punteggioTotale;
    }
    public void addPunteggioBonus(){
        this.punteggioTotale += ServerConfig.Z;
    }

    //Metodi per ottenere e resettare il punteggio ottenuto durante l'ultima sfida dal giocatore
    public int getPunteggioPartita(){
        return this.punteggioPartita;
    }
    public void resetPunteggioPartita(){
        this.punteggioPartita = 0;
    }

    //Metodi per impostare e ottenere lo stato di un giocatore (se impegnato in una sfida)
    public void setInSfida(boolean inSfida) {
        this.inSfida = inSfida;
    }
    public boolean getInSfida(){
        return this.inSfida;
    }

    public String getUsername(){
        return this.username;
    }
}

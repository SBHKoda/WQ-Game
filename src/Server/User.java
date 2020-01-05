package Server;

public class User {
    private String username;
    private String password;
    private boolean isOnline;

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

}

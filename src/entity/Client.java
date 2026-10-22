package entity;

/**
 * Entity class representing the Client table in the database.
 */
public class Client {
    private int clientID;
    private String firstName;
    private String lastName;
    private String mobilePhone;

    public Client() {}

    public Client(int clientID, String firstName, String lastName, String mobilePhone) {
        this.clientID = clientID;
        this.firstName = firstName;
        this.lastName = lastName;
        this.mobilePhone = mobilePhone;
    }

    public int getClientID() { return clientID; }
    public void setClientID(int clientID) { this.clientID = clientID; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getMobilePhone() { return mobilePhone; }
    public void setMobilePhone(String mobilePhone) { this.mobilePhone = mobilePhone; }

    public String getFullName() {
        return firstName + " " + lastName;
    }
}

package entity;

import java.time.LocalDate;

/**
 * Entity class representing the ClubMember table in the database.
 */
public class ClubMember {
    private int clientID;
    private String firstName;
    private String lastName;
    private String mobilePhone;
    private LocalDate membershipDate;
    private boolean isActive;

    public ClubMember() {}

    public ClubMember(int clientID, String firstName, String lastName, String mobilePhone, LocalDate membershipDate, boolean isActive) {
        this.clientID = clientID;
        this.firstName = firstName;
        this.lastName = lastName;
        this.mobilePhone = mobilePhone;
        this.membershipDate = membershipDate;
        this.isActive = isActive;
    }

    public int getClientID() { return clientID; }
    public void setClientID(int clientID) { this.clientID = clientID; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getMobilePhone() { return mobilePhone; }
    public void setMobilePhone(String mobilePhone) { this.mobilePhone = mobilePhone; }

    public LocalDate getMembershipDate() { return membershipDate; }
    public void setMembershipDate(LocalDate membershipDate) { this.membershipDate = membershipDate; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public String getFullName() {
        return firstName + " " + lastName;
    }
}

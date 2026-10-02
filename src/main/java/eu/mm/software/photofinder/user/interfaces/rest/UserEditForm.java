package eu.mm.software.photofinder.user.interfaces.rest;


import eu.mm.software.photofinder.user.domain.UserFormAttributes;
import lombok.Setter;

import java.util.Set;

@Setter
public class UserEditForm implements UserFormAttributes {

    private String id;
    private String userName;
    private String userEmail;
    private String passwordForm1;
    private String passwordForm2;
    private Boolean active;
    private Set<String> roles;

    public UserEditForm(String id,
                        String userName,
                        String userEmail,
                        String passwordForm1,
                        String passwordForm2,
                        boolean active,
                        Set<String> roles) {

        this.id = id;
        this.userName = userName;
        this.userEmail = userEmail;
        this.passwordForm1 = passwordForm1;
        this.passwordForm2 = passwordForm2;
        this.active = active;
        this.roles = roles;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getUserName() {
        return userName;
    }

    @Override
    public String getUserEmail() {
        return userEmail;
    }

    @Override
    public String getPasswordForm1() {
        return passwordForm1;
    }

    @Override
    public String getPasswordForm2() {
        return passwordForm2;
    }

    @Override
    public Boolean getActive() {
        return active;
    }

    @Override
    public Set<String> getRoles() {
        return roles;
    }


}

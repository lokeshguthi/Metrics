package de.tukl.softech.exclaim.data;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.sql.ResultSet;
import java.sql.SQLException;

public class User {

    public static class RowMapper implements org.springframework.jdbc.core.RowMapper<User> {

        @Override
        public User mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new User(rs.getInt("userid"), rs.getString("username"), rs.getString("firstname"),
                    rs.getString("lastname"), rs.getString("studentid"), rs.getString("email"), rs.getString("password"),
                    rs.getBoolean("verified"), rs.getBoolean("admin"), rs.getString("code"));
        }
    }

    private int userid;

    @NotBlank(message = "Der Benutzername kann nicht leer sein.")
    @Size(min = 2, max = 50, message = "Der Benutzername muss zwischen 2 und 50 Zeichen enthalten.")
    @Pattern(regexp = "^[0-9a-zA-Z_\\-]*", message = "Der Benutzername darf nur die Buchstaben A-Z, Ziffern, sowie Bindestriche und Unterstriche enthalten.")
    private String username;

    @NotBlank(message = "Der Vorname kann nicht leer sein.")
    @Size(max = 50, message = "Der Vorname kann maximal 50 Zeichen enthalten.")
    private String firstname;

    @NotBlank(message = "Der Nachname kann nicht leer sein.")
    @Size(max = 50, message = "Der Nachnahme kann maximal 50 Zeichen enthalten.")
    private String lastname;


    @Size(min=6, max = 6, message = "Die Matrikelnummer muss 6 Stellen haben.")
    @Pattern(regexp = "^[0-9]*", message = "Die Matrikelnummer darf nur Ziffern enthalten.")
    private String studentid;


    @NotBlank(message = "Die Email-Addresse kann nicht leer sein.")
    @Email(message = "Die Email-Addresse ist nicht gültig.")
    private String email;

    //only used in register-form
    @NotBlank(message = "Die Email-Addresse kann nicht leer sein.")
    @Email(message = "Die Email-Addresse ist nicht gültig.")
    private String email2;

    @NotBlank(message = "Das Passwort kann nicht leer sein.")
    @Size(min = 6, max = 50, message = "Das Passwort muss zwischen 6 und 50 Zeichen enthalten.")
    private String password;

    //only used in register-form
    @NotBlank(message = "Das Passwort kann nicht leer sein.")
    @Size(min = 6, max = 50, message = "Das Passwort muss zwischen 6 und 50 Zeichen enthalten.")
    private String password2;

    private boolean verified = false;
    private boolean admin = false;
    private String code;

    public User() {
    }

    //used for transfer
    public User(String username, String firstname, String lastname, String email, String password) {
        this.username = username;
        this.firstname = firstname;
        this.lastname = lastname;
        this.email = email;
        this.password = password;
    }

    public User(int userid, String username, String firstname, String lastname, String studentid, String email, String password, boolean verified, boolean admin, String code) {
        this.userid = userid;
        this.username = username;
        this.firstname = firstname;
        this.lastname = lastname;
        this.email = email;
        this.password = password;
        this.studentid = studentid;
        this.verified = verified;
        this.admin = admin;
        this.code = code;
    }



    public int getUserid() {
        return userid;
    }

    public void setUserid(int userid) {
        this.userid = userid;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getFirstname() {
        return firstname;
    }

    public void setFirstname(String firstname) {
        this.firstname = firstname;
    }

    public String getLastname() {
        return lastname;
    }

    public void setLastname(String lastname) {
        this.lastname = lastname;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRealname() {
        return firstname + " " + lastname;
    }

    public String getStudentid() {
        return studentid;
    }

    public void setStudentid(String studentid) {
        this.studentid = studentid;
    }

    public String getPassword2() {
        return password2;
    }

    public void setPassword2(String password2) {
        this.password2 = password2;
    }

    public String getEmail2() {
        return email2;
    }

    public void setEmail2(String email2) {
        this.email2 = email2;
    }

    public boolean isVerified() {
        return verified;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }

    public boolean isAdmin() {
        return admin;
    }

    public void setAdmin(boolean admin) {
        this.admin = admin;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    @Override
    public String toString() {
        return firstname + " " + lastname;
    }
}

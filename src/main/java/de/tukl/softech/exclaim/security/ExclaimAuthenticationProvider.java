package de.tukl.softech.exclaim.security;

import de.tukl.softech.exclaim.controllers.UserController;
import de.tukl.softech.exclaim.dao.ExerciseDao;
import de.tukl.softech.exclaim.dao.UserDao;
import de.tukl.softech.exclaim.data.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Component
public class ExclaimAuthenticationProvider implements AuthenticationProvider {

    @Value("${exclaim.admin.pw}")
    private String adminpw;

    @Value("${exclaim.admin.email}")
    private String adminMail;

    private ExerciseDao exerciseDao;
    private UserDao userDao;
    private UserController userController;
    private AccessChecker accessChecker;

    public ExclaimAuthenticationProvider(ExerciseDao exerciseDao, UserDao userDao, UserController userController, AccessChecker accessChecker) {
        this.exerciseDao = exerciseDao;
        this.userDao = userDao;
        this.userController = userController;
        this.accessChecker = accessChecker;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String name = authentication.getName();
        String password = authentication.getCredentials().toString();

        if (name.equals("admin")) {
            if (adminpw == null || adminpw.length() < 5) {
                throw new BadCredentialsException("Admin account is not activated.");
            }
            if (!password.equals(adminpw)) {
                throw new BadCredentialsException("Wrong username or password");
            }
            List<GrantedAuthority> authorities = new ArrayList<>();
            authorities.add(new AssistantRole("admin")); //get initial admin role
            exerciseDao.getAllExercises().forEach(exercise -> authorities.add(new AssistantRole(exercise.getId())));
            return new STATSUserToken(name, password,
                    "Admin",
                    adminMail,
                    null,
                    0,
                    true,
                    authorities);
        }
        User user = userDao.getUser(name);
        if (user == null) {
            throw new BadCredentialsException("Ungültiger Benutzername oder Passwort.");
        } else if (!user.isVerified()) {
            throw new DisabledException("Bitte aktivieren Sie zuerst ihr Konto mit dem Link der Ihnen per Mail zugesendet wurde.");
        } else if (!userController.passwordMatch(password, user.getPassword())) {
            throw new BadCredentialsException("Ungültiger Benutzername oder Passwort.");
        }

        return new STATSUserToken(name, password,
                user.getRealname(),
                user.getEmail(),
                user.getStudentid(),
                user.getUserid(),
                user.isAdmin(),
                accessChecker.getUserAuthorities(user.getUserid()));
    }



    @Override
    public boolean supports(Class<?> authentication) {
        return authentication.equals(UsernamePasswordAuthenticationToken.class);
    }

    public abstract static class ExerciseRole implements GrantedAuthority {
        protected final String exercise;

        public ExerciseRole(String exercise) {
            this.exercise = exercise;
        }

        public String getExercise() {
            return exercise;
        }
    }

    public static class AssistantRole extends ExerciseRole {

        public AssistantRole(String exercise) {
            super(exercise);
        }

        @Override
        public String getAuthority() {
            return "Assistant_" + exercise;
        }
    }

    public static class TutorRole extends ExerciseRole {
        private final String group;

        public TutorRole(String exercise, String group) {
            super(exercise);
            this.group = group;
        }

        public String getGroup() {
            return group;
        }

        @Override
        public String getAuthority() {
            return "Tutor_" + exercise + "_" + group;
        }
    }

    public static class StudentRole extends ExerciseRole {
        private final String group;
        private final String team;

        public StudentRole(String exercise, String group, String team) {
            super(exercise);
            this.group = group;
            this.team = team;
        }

        public String getGroup() {
            return group;
        }

        public String getTeam() {
            return team;
        }

        @Override
        public String getAuthority() {
            return "Student_" + exercise + "_" + group + "_" + team;
        }
    }

    public static class STATSUserToken extends UsernamePasswordAuthenticationToken {
        private String realname;
        private String email;
        private String studentid;
        private int userid;
        private boolean admin;

        public STATSUserToken(String username, String password, String realname, String email, String studentid, int userid, boolean admin, Collection<? extends GrantedAuthority> authorities) {
            super(username, password, authorities);
            this.realname = realname;
            this.email = email;
            this.studentid = studentid;
            this.userid = userid;
            this.admin = admin;
        }

        public STATSUserToken(STATSUserToken statsUserToken, Collection<? extends GrantedAuthority> authorities) {
            super(statsUserToken.getName(), statsUserToken.getCredentials(), authorities);
            this.realname = statsUserToken.realname;
            this.email = statsUserToken.email;
            this.studentid = statsUserToken.studentid;
            this.userid = statsUserToken.userid;
            this.admin = statsUserToken.admin;
        }

        public String getRealname() {
            return realname;
        }

        public String getEmail() {
            return email;
        }

        public String getStudentid() {
            return studentid;
        }

        public int getUserid() {
            return userid;
        }

        public boolean isAdmin() {
            return admin;
        }
    }
}

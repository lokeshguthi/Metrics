package de.tukl.softech.exclaim.controllers;

import de.tukl.softech.exclaim.dao.GroupDao;
import de.tukl.softech.exclaim.dao.UserDao;
import de.tukl.softech.exclaim.data.Exercise;
import de.tukl.softech.exclaim.data.User;
import de.tukl.softech.exclaim.monitoring.MetricsService;
import de.tukl.softech.exclaim.security.AccessChecker;
import de.tukl.softech.exclaim.transferdata.ExerciseRights;
import de.tukl.softech.exclaim.utils.Mail;
import org.jasypt.digest.StandardStringDigester;
import org.jasypt.util.password.rfc2307.RFC2307SMD5PasswordEncryptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSendException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Collections.singletonList;

@Controller
public class UserController {
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    private MetricsService metrics;
    private UserDao userDao;
    private GroupDao groupDao;
    private Mail mail;
    private AccessChecker accessChecker;

    private RFC2307SMD5PasswordEncryptor md5Pe;
    private StandardStringDigester dig;
    private PasswordEncoder pe;

    public UserController(MetricsService metrics, UserDao userDao, GroupDao groupDao, Mail mail, AccessChecker accessChecker) {
        this.metrics = metrics;
        this.userDao = userDao;
        this.groupDao = groupDao;
        this.mail = mail;
        this.accessChecker = accessChecker;
        this.pe = new BCryptPasswordEncoder();

        this.md5Pe = new RFC2307SMD5PasswordEncryptor();
        this.dig = new StandardStringDigester();
        this.dig.setAlgorithm("MD5");
        this.dig.setIterations(17);
        this.dig.setSaltSizeBytes(17);
        this.dig.setStringOutputType("hexadecimal");
    }

    @ModelAttribute("page")
    public String getPage() {
        return "user";
    }

    @GetMapping("/user")
    @PreAuthorize("@accessChecker.isAdmin()")
    public String getUserPage(Model model) {
        metrics.registerAccessUser();

        List<User> users = userDao.getAllUsers();
        model.addAttribute("users", users);
        return "user/user";
    }

    @GetMapping("/register")
    @PreAuthorize("!isAuthenticated()")
    public String getUserRegisteredPage(User user) {
        metrics.registerAccessUser();
        return "user/user-register";
    }

    @PostMapping("/register")
    @PreAuthorize("!isAuthenticated()")
    public String registeredUser(RedirectAttributes redirectAttributes, @Valid User user, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return "user/user-register";
        }
        if (user.getUsername().equalsIgnoreCase("admin")) {
            bindingResult.rejectValue("username", "username.exists", "Es existiert bereits ein Nutzer mit diesem Namen.");
            return "user/user-register";
        }
        if (!user.getEmail().equals(user.getEmail2())) {
            bindingResult.rejectValue("email", "email.notMatch", "Die angegebenen Email-Addressen stimmen nicht überein.");
            return "user/user-register";
        }
        if (!user.getPassword().equals(user.getPassword2())) {
            bindingResult.rejectValue("password", "password.notMatch", "Die angegebenen Passwörter stimmen nicht überein.");
            return "user/user-register";
        }
        String code = dig.digest(user.getUsername());
        // Create new user to avoid form field injection vulnerability
        user = new User(0, user.getUsername(), user.getFirstname(), user.getLastname(), user.getStudentid(), user.getEmail(), pe.encode(user.getPassword()), false, false, code);
        try {
            userDao.createUser(user);
        } catch (DuplicateKeyException e) {
            //check duplicate username
            User usernameUser = userDao.getUser(user.getUsername());
            if (usernameUser != null) {
                if (usernameUser.isVerified()) {
                    bindingResult.rejectValue("username", "username.exists", "Es existiert bereits ein Nutzer mit diesem Namen.");
                    return "user/user-register";
                } else {
                    userDao.deleteUser(usernameUser.getUserid());
                }

            }
            //check duplicate studentid
            if (user.getStudentid() != null) {
                User studentUser = userDao.getUserByStudentId(user.getStudentid());
                if (studentUser != null) {
                    if (studentUser.isVerified()) {
                        bindingResult.rejectValue("studentid", "studentid.exists", "Es existiert bereits ein Nutzer mit dieser Matrikelnummer.");
                        return "user/user-register";
                    } else {
                        userDao.deleteUser(studentUser.getUserid());
                    }
                }
            }
            try {
                userDao.createUser(user);
            } catch (DuplicateKeyException dke) {
                redirectAttributes.addFlashAttribute("errors",
                        singletonList("Das Benutzerkonto konnte nicht angelegt werden. Wenden Sie sich an den Übungsleiter."));
            }
        }

        try {
            mail.sendActivationLink(user.getEmail(), user.getUsername(), user.getLastname(), user.getFirstname(), code);
        } catch (MailException e) {
            logger.error("Exception while sending mail: {}", e.getLocalizedMessage());
            redirectAttributes.addFlashAttribute("mailerror",
                    "Das Benutzerkonto wurde angelegt. Die E-Mail mit dem Aktivierungslink konnte nicht verschickt werden. Wenden Sie sich an den Übungsleiter.");
            return "redirect:/registered";
        }

        return "redirect:/registered";
    }

    @GetMapping("/registered")
    public String getUserRegisteredPage() {
        return "user/user-registered";
    }

    @GetMapping("/activate")
    public String getActivationPage(@RequestParam("user") String username,
                                    @RequestParam("code") String code,
            Model model) {
        metrics.registerAccessActivation();

        String message;
        User user = userDao.getUser(username);
        if (user == null) {
            message = "Es existiert kein passender Nutzer.";
        } else if (user.isVerified()) {
            message = "Der Account ist bereits aktiviert.";
        } else if (user.getCode().equals(code)) {
            user.setVerified(true);
            user.setCode(null);
            userDao.activate(user.getUsername());
            message = "Account erfolgreich aktiviert. Sie können sich nun anmelden.";
        } else {
            message = "Aktivierung nicht möglich. Versuchen Sie sich erneut zu registrieren oder wenden Sie sich an den Übungsleiter.";
        }
        model.addAttribute("message", message);
        return "user/activate";
    }

    @GetMapping("/settings")
    public String getSettingsPage(Model model) {
        metrics.registerAccessUser();

        User user;
        if (accessChecker.getAuthentication().getName().equals("admin")) {
            user = new User("admin", "", "", accessChecker.getAuthentication().getEmail(), "");
        } else {
            user = userDao.getUser(accessChecker.getAuthentication().getName());
        }
        model.addAttribute("user", user);
        if (!model.containsAttribute("passwordChange")) {
            model.addAttribute("passwordChange", new PasswordChange());
        }
        return "user/user-settings";
    }

    @PostMapping("/settings/password")
    public String changePassword(RedirectAttributes redirectAttributes, @Valid PasswordChange passwordChange, BindingResult bindingResult) {
        metrics.registerAccessUser();

        if (accessChecker.getAuthentication().getName().equals("admin")) {
            redirectAttributes.addFlashAttribute("errors",
                    singletonList("Das Passwort des Admin-Users kann nur in der Config geändert werden."));

            return "redirect:/settings";
        }

        User user = userDao.getUser(accessChecker.getAuthentication().getName());

        if (!passwordChange.getPassword().equals(passwordChange.getPassword2())) {
            bindingResult.rejectValue("password", "password.notMatch", "Die angegebenen Passwörter stimmen nicht überein.");
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.passwordChange", bindingResult);
            redirectAttributes.addFlashAttribute("passwordChange", passwordChange);
            return "redirect:/settings";
        }
        if (!passwordMatch(passwordChange.oldPassword, user.getPassword())) {
            bindingResult.rejectValue("oldPassword", "oldPassword.notCorrect", "Das angegebene Passwort ist nicht korrekt.");
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.passwordChange", bindingResult);
            redirectAttributes.addFlashAttribute("passwordChange", passwordChange);
            return "redirect:/settings";
        }

        userDao.setPassword(user.getUserid(), pe.encode(passwordChange.password));
        logger.info("Password changed for user {}", user.getUsername());

        redirectAttributes.addFlashAttribute("passwordchanged", "Passwort erfolgreich geändert.");

        return "redirect:/settings";
    }

    @PostMapping("/settings/changename")
    public String changePassword(RedirectAttributes redirectAttributes, @Valid User user, BindingResult bindingResult) {
        metrics.registerAccessUser();

        if (accessChecker.getAuthentication().getName().equals("admin")) {
            redirectAttributes.addFlashAttribute("errors",
                    singletonList("Der Name des Admin-Users kann nicht geändert werden."));

            return "redirect:/settings";
        }

        userDao.changeName(accessChecker.getAuthentication().getUserid(), user.getFirstname(), user.getLastname());
        redirectAttributes.addFlashAttribute("namechanged", "Name erfolgreich geändert.");
        return "redirect:/settings";
    }

    @GetMapping("/requestPassword")
    @PreAuthorize("!isAuthenticated()")
    public String getRequestPasswordPage() {
        return "user/request-password";
    }

    @PostMapping("/requestPassword")
    @PreAuthorize("!isAuthenticated()")
    public String requestPassword(@RequestParam("email") String email, RedirectAttributes redirectAttributes) {
        metrics.registerAccessUser();

        User user = userDao.getUserByMail(email);
        if (user == null) {
            redirectAttributes.addFlashAttribute("result", "E-Mailaddresse nicht gefunden.");
        } else if (!user.isVerified()) {
            redirectAttributes.addFlashAttribute("result", "Das Konto wurde nicht aktiviert.");
        } else {
            String code = dig.digest(user.getUsername() + "reset");
            userDao.setCode(user.getUserid(), code);
            try {
                mail.sendResetLink(email, user.getUsername(), code);
                redirectAttributes.addFlashAttribute("result", "E-Mail erfolgreich versendet.");
            } catch (MailSendException e) {
                logger.info("Mail could not be send: {}", e.getMessage());
                redirectAttributes.addFlashAttribute("result",
                        "Es wurde keine E-Mail versandt. Stellen Sie sicher, dass ein Konto für diese E-Mailadresse existiert.");
            } catch (MailException e) {
                logger.warn("Exception while sending mail: {}", e.getMessage());
                redirectAttributes.addFlashAttribute("result",
                        "Die E-Mail konnte nicht versandt werden. Bitte wenden Sie sich an den Verantwortlichen.");
            }
        }
        return "redirect:/requestPassword";
    }

    @GetMapping("/resetPassword")
    @PreAuthorize("!isAuthenticated()")
    public String getResetPasswordPage(Model model, @RequestParam("user") String username, @RequestParam("reset") String reset) {
        User user = userDao.getUser(username);
        if (user == null || !user.isVerified() || user.getCode() == null || user.getCode().isEmpty() || !reset.equals(user.getCode())) {
            model.addAttribute("invalid", true);
        } else {
            PasswordChange passwordChange = new PasswordChange();
            passwordChange.username = username;
            passwordChange.code = reset;
            model.addAttribute("passwordChange", passwordChange);
        }
        return "user/reset-password";
    }

    @PostMapping("/resetPassword")
    @PreAuthorize("!isAuthenticated()")
    public String resetPassword(@Valid PasswordChange passwordChange, BindingResult bindingResult) {
        metrics.registerAccessUser();

        User user = userDao.getUser(passwordChange.username);
        if (user == null) {
            bindingResult.rejectValue(null, "username.notfound", "Das Konto konnte nicht gefunden werden.");
            return "user/reset-password";
        }
        if (!user.isVerified() || user.getCode() == null || user.getCode().isEmpty() || !passwordChange.code.equals(user.getCode())) {
            bindingResult.rejectValue(null, "code.invalid", "Ungültiger Reset-Code. Bitte lassen Sie sich einen neuen Code schicken.");
            return "user/reset-password";
        }

        if (!passwordChange.getPassword().equals(passwordChange.getPassword2())) {
            bindingResult.rejectValue("password", "password.notMatch", "Die angegebenen Passwörter stimmen nicht überein.");
            return "user/reset-password";
        }

        userDao.setCode(user.getUserid(), null);
        userDao.setPassword(user.getUserid(), pe.encode(passwordChange.password));
        logger.info("Password changed for user {}", user.getUsername());

        return "user/password-changed";
    }

    @GetMapping("/user/{uid}")
    @PreAuthorize("@accessChecker.isAdmin()")
    public String getUserAdminPage(Model model, @PathVariable int uid) {
        metrics.registerAccessUser();

        User user = userDao.getUserById(uid);
        model.addAttribute("user", user);
        if (!model.containsAttribute("passwordChange")) {
            PasswordChange passwordChange = new PasswordChange();
            passwordChange.username = user.getUsername();
            model.addAttribute("passwordChange", passwordChange);
        }

        List<ExerciseRights> exerciseRights = userDao.getUserRights(user.getUserid()).stream()
                .sorted(Comparator.comparing(ExerciseRights::getRole).reversed().thenComparing(ExerciseRights::getExerciseId)).collect(Collectors.toList());
        model.addAttribute("exerciseRights", exerciseRights);
        if (!user.isVerified()) {

            model.addAttribute("verificationLink", mail.createVerificationLink(user.getUsername(), user.getCode()));
        }

        return "user/user-admin";
    }

    @PostMapping("/user/password")
    @PreAuthorize("@accessChecker.isAdmin()")
    public String setPassword(RedirectAttributes redirectAttributes, @Valid PasswordChange passwordChange, BindingResult bindingResult) {
        metrics.registerAccessUser();

        User user = userDao.getUser(passwordChange.username);
        if (user == null) {
            redirectAttributes.addFlashAttribute("errors",
                    singletonList("Das Konto zum Ändern des Passworts konnte nicht gefunden werden."));
            return "redirect:/user";
        }

        if (!passwordChange.getPassword().equals(passwordChange.getPassword2())) {
            bindingResult.rejectValue("password", "password.notMatch", "Die angegebenen Passwörter stimmen nicht überein.");
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.passwordChange", bindingResult);
            redirectAttributes.addFlashAttribute("passwordChange", passwordChange);
            return "redirect:/user/" + user.getUserid();
        }

        userDao.setPassword(user.getUserid(), pe.encode(passwordChange.password));
        try {
            mail.sendNewLoginData(user.getEmail(), user.getUsername(), passwordChange.password);
            redirectAttributes.addFlashAttribute("passwordchanged", "Passwort erfolgreich geändert.");
        } catch (MailSendException e) {
            logger.info("Mail could not be send: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("passwordchanged",
                    "Fehler beim Versenden der Login-Daten.");
        } catch (MailException e) {
            logger.warn("Exception while sending mail: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("passwordchanged",
                    "Fehler beim Versenden der Login-Daten.");
        }

        logger.info("Password changed for user {} by {}", user.getUsername(), accessChecker.getAuthentication().getName());

        return "redirect:/user/" + user.getUserid();
    }

    @PostMapping("/user/changedata")
    @PreAuthorize("@accessChecker.isAdmin()")
    public String changeUserData(RedirectAttributes redirectAttributes, @Valid User user, BindingResult bindingResult) {
        metrics.registerAccessUser();

        User dbUser = userDao.getUserById(user.getUserid());
        if (dbUser == null) {
            redirectAttributes.addFlashAttribute("datachanged", "Fehler! Benutzer nicht gefunden.");
            return "redirect:/user/" + user.getUserid();
        }

        if (!dbUser.getFirstname().equals(user.getFirstname())
                || !dbUser.getLastname().equals(user.getLastname())) {
            userDao.changeName(user.getUserid(), user.getFirstname(), user.getLastname());
        }
        if (!dbUser.getUsername().equals(user.getUsername())) {
            userDao.changeUsername(user.getUserid(), user.getUsername());
        }

        if (!dbUser.getEmail().equals(user.getEmail())) {
            userDao.setEmail(user.getUserid(), user.getEmail());
        }
        if (dbUser.isAdmin() != user.isAdmin()) {
            userDao.setAdmin(user.getUserid(), user.isAdmin());
        }

        redirectAttributes.addFlashAttribute("datachanged", "Benutzerdaten erfolgreich geändert.");
        return "redirect:/user/" + user.getUserid();
    }

    @GetMapping("/exercise/{eid}/admin/assistants")
    @PreAuthorize("@accessChecker.isAdmin()")
    public String getExerciseAssistantPage(Model model, @PathVariable String eid) {
        metrics.registerAccessAdmin();

        List<User> assistants = userDao.getExerciseAssistants(eid);
        model.addAttribute("exercise", eid);
        model.addAttribute("assistants", assistants);
        return "exercise-assistants";
    }

    @PostMapping("/exercise/{eid}/admin/assistants")
    @PreAuthorize("@accessChecker.isAdmin()")
    public String postAddAssistant(@PathVariable String eid,
                                     @RequestParam String username,
                                     RedirectAttributes redirectAttributes) {
        metrics.registerAccessAdmin();
        logger.info("adding assistant {} to exercise {}", username, eid);
        User user = userDao.getUser(username);
        if (user == null) {
            redirectAttributes.addFlashAttribute("errors", "Benutzer mit Nutzername " + username + " nicht gefunden.");
        } else {
            ExerciseRights exerciseRights = new ExerciseRights();
            exerciseRights.setExerciseId(eid);
            exerciseRights.setRole(ExerciseRights.Role.assistant);
            userDao.addUserRights(user.getUserid(), exerciseRights);
        }

        return "redirect:/exercise/{eid}/admin/assistants";
    }

    @PostMapping("/exercise/{eid}/admin/assistants/{uid}/delete")
    @PreAuthorize("@accessChecker.isAdmin()")
    public String postDeleteAssistant(@PathVariable("eid") String exercise,
                                      @PathVariable("uid") int userid) {
        metrics.registerAccessAdmin();

        userDao.deleteUserRights(userid, exercise);

        return "redirect:/exercise/{eid}/admin/assistants";
    }

    @GetMapping("/exercise/{eid}/admin/tutors")
    @PreAuthorize("@accessChecker.hasAdminRight(#eid)")
    public String getExerciseTutorsPage(Model model, @PathVariable String eid) {
        metrics.registerAccessAdmin();

        List<Tutor> tutors = userDao.getExerciseTutors(eid);
        model.addAttribute("exercise", eid);
        model.addAttribute("tutors", tutors);
        return "exercise-tutors";
    }

    @PostMapping("/exercise/{eid}/admin/tutors")
    @PreAuthorize("@accessChecker.hasAdminRight(#eid)")
    public String postAddTutor(@PathVariable String eid,
                               @RequestParam String username,
                               @RequestParam String group,
                               RedirectAttributes redirectAttributes) {
        metrics.registerAccessAdmin();
        logger.info("adding tutor {} to exercise {}", username, eid);
        User user = userDao.getUser(username);
        if (user == null) {
            redirectAttributes.addFlashAttribute("errors", "Benutzer mit Nutzername " + username + " nicht gefunden.");
        } else if (groupDao.getGroup(eid, group) == null) {
            redirectAttributes.addFlashAttribute("errors", "Gruppe " + group + " nicht gefunden.");
        } else {
            ExerciseRights exerciseRights = new ExerciseRights();
            exerciseRights.setExerciseId(eid);
            exerciseRights.setRole(ExerciseRights.Role.tutor);
            exerciseRights.setGroupId(group);
            userDao.addUserRights(user.getUserid(), exerciseRights);
        }

        return "redirect:/exercise/{eid}/admin/tutors";
    }

    @PostMapping("/exercise/{eid}/admin/tutors/{uid}/delete")
    @PreAuthorize("@accessChecker.hasAdminRight(#exercise)")
    public String postDeleteTutor(@PathVariable("eid") String exercise,
                                      @PathVariable("uid") int userid,
                                  @RequestParam String group) {
        metrics.registerAccessAdmin();

        userDao.deleteUserRights(userid, exercise, group);

        return "redirect:/exercise/{eid}/admin/tutors";
    }

    @PostMapping("/user/delete")
    @PreAuthorize("@accessChecker.isAdmin()")
    public String postDeleteUser(@RequestParam("userid") int userid,
                                 RedirectAttributes redirectAttributes) {
        metrics.registerAccessUser();

        try {
            userDao.deleteUser(userid);
        } catch (DataAccessException e) {
            redirectAttributes.addFlashAttribute("errors",
                    singletonList("Nutzer konnte nicht gelöscht werden, da noch Informationen mit diesem Konto verknüpft sind!"));
            return "redirect:/user/" + userid;
        }
        return "redirect:/user";
    }


    public boolean passwordMatch(String password, String encryptedPassword) {
        if (encryptedPassword.startsWith("{SMD5}")) {
            return md5Pe.checkPassword(password, encryptedPassword);
        }
        return pe.matches(password, encryptedPassword);
    }

    public static class Tutor {
        public User user;
        public String group;
    }


    public static class PasswordChange {

        @NotBlank(message = "Das Passwort kann nicht leer sein.")
        @Size(min = 6, max = 50, message = "Das Passwort muss zwischen 6 und 50 Zeichen enthalten.")
        private String password;

        //only used in register-form
        @NotBlank(message = "Das Passwort kann nicht leer sein.")
        @Size(min = 6, max = 50, message = "Das Passwort muss zwischen 6 und 50 Zeichen enthalten.")
        private String password2;

        private String oldPassword;

        private String username;

        private String code;

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getPassword2() {
            return password2;
        }

        public void setPassword2(String password2) {
            this.password2 = password2;
        }

        public String getOldPassword() {
            return oldPassword;
        }

        public void setOldPassword(String oldPassword) {
            this.oldPassword = oldPassword;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }
    }

}

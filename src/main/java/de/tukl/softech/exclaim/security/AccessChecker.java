package de.tukl.softech.exclaim.security;

import de.tukl.softech.exclaim.dao.ResultsDao;
import de.tukl.softech.exclaim.dao.UserDao;
import de.tukl.softech.exclaim.data.Team;
import de.tukl.softech.exclaim.security.ExclaimAuthenticationProvider.ExerciseRole;
import de.tukl.softech.exclaim.security.ExclaimAuthenticationProvider.STATSUserToken;
import de.tukl.softech.exclaim.security.ExclaimAuthenticationProvider.StudentRole;
import de.tukl.softech.exclaim.security.ExclaimAuthenticationProvider.TutorRole;
import de.tukl.softech.exclaim.transferdata.ExerciseRights;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.stereotype.Component;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static de.tukl.softech.exclaim.security.ExclaimAuthenticationProvider.AssistantRole;

@Component("accessChecker")
public class AccessChecker {
    private ResultsDao resultsDao;
    private UserDao userDao;

    private SessionRegistry sessionRegistry;

    public AccessChecker(ResultsDao resultsDao, UserDao userDao, SessionRegistry sessionRegistry) {
        this.resultsDao = resultsDao;
        this.userDao = userDao;
        this.sessionRegistry = sessionRegistry;
    }

    public @Nullable STATSUserToken getAuthentication() {
        return (STATSUserToken) SecurityContextHolder.getContext().getAuthentication();
    }

    public void updateRights() {
        STATSUserToken user = getAuthentication();
        if (user != null) {
            SecurityContextHolder.getContext().setAuthentication(new STATSUserToken(user, getUserAuthorities(user.getUserid())));
        }
    }

    public void expireUserSession(String username) {
        List<Object> loggedUsers = sessionRegistry.getAllPrincipals();
        for (Object principal : loggedUsers) {
            if(principal instanceof STATSUserToken) {
                final STATSUserToken loggedUser = (STATSUserToken) principal;
                if(username.equals(loggedUser.getName())) {
                    List<SessionInformation> sessionsInfo = sessionRegistry.getAllSessions(principal, false);
                    if(null != sessionsInfo && sessionsInfo.size() > 0) {
                        for (SessionInformation sessionInformation : sessionsInfo) {
                            sessionInformation.expireNow();
                            sessionRegistry.removeSessionInformation(sessionInformation.getSessionId());
                        }
                    }
                }
            }
        }
    }

    public boolean isAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof STATSUserToken) {
            return  ((STATSUserToken) auth).isAdmin();
        }
        return false;
    }

    public boolean isAssistant() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth!=null) {
            for (GrantedAuthority authority : auth.getAuthorities()) {
                if (authority instanceof AssistantRole)
                    return true;
            }
        }
        return false;
    }

    public boolean isStudent() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof STATSUserToken) {
            String studentId = ((STATSUserToken) auth).getStudentid();
            return studentId != null && !studentId.isEmpty();
        }
        return false;
    }

    public boolean isOnlyStudent(String exercise) {
        STATSUserToken auth = getAuthentication();
        if (auth != null) {
            for (GrantedAuthority authority : auth.getAuthorities()) {
                if (authority instanceof AssistantRole && Objects.equals(((AssistantRole) authority).getExercise(), exercise)) {
                    return false;
                } else if (authority instanceof TutorRole && Objects.equals(((TutorRole) authority).getExercise(), exercise)) {
                    return false;
                }
            }
            return true;
        } else {
            return false;
        }
    }

    public boolean hasAdminRight(String exercise) {
        STATSUserToken auth = getAuthentication();
        if (auth != null) {
            for (GrantedAuthority authority : auth.getAuthorities()) {
                if (authority instanceof AssistantRole) {
                    AssistantRole assistantRole = (AssistantRole) authority;
                    if (Objects.equals(assistantRole.getExercise(), exercise))
                        return true;
                }
            }
        }
        return false;
    }

    public boolean canAccess(String exercise) {
        STATSUserToken auth = getAuthentication();
        if (auth != null) {
            for (GrantedAuthority authority : auth.getAuthorities()) {
                if (authority instanceof ExerciseRole && Objects.equals(((ExerciseRole) authority).getExercise(), exercise))
                    return true;
            }
        }
        return false;
    }

    public boolean hasUploadRight(String exercise) {
        STATSUserToken auth = getAuthentication();
        if (auth != null) {
            for (GrantedAuthority authority : auth.getAuthorities()) {
                if (authority instanceof AssistantRole && Objects.equals(((AssistantRole) authority).getExercise(), exercise))
                    return true;
                else if (authority instanceof StudentRole && Objects.equals(((StudentRole) authority).getExercise(), exercise))
                    return true;
            }
        }
        return false;
    }

    public boolean hasAssessRight(String exercise) {
        STATSUserToken auth = getAuthentication();
        if (auth != null) {
            for (GrantedAuthority authority : auth.getAuthorities()) {
                if (authority instanceof AssistantRole && Objects.equals(((AssistantRole) authority).getExercise(), exercise))
                    return true;
                else if (authority instanceof TutorRole && Objects.equals(((TutorRole) authority).getExercise(), exercise))
                    return true;
            }
        }
        return false;
    }

    public boolean hasUploadRight(String exercise, String group, String team) {
        STATSUserToken auth = getAuthentication();
        if (auth != null) {
            for (GrantedAuthority authority : auth.getAuthorities()) {
                if (authority instanceof AssistantRole && Objects.equals(((AssistantRole) authority).getExercise(), exercise))
                    return true;
                else if (authority instanceof StudentRole &&
                        Objects.equals(((StudentRole) authority).getExercise(), exercise) &&
                        Objects.equals(((StudentRole) authority).getGroup(), group) &&
                        Objects.equals(((StudentRole) authority).getTeam(), team))
                    return true;
            }
        }
        return false;
    }

    public boolean hasAssessRight(String exercise, String group) {
        STATSUserToken auth = getAuthentication();
        if (auth != null) {
            for (GrantedAuthority authority : auth.getAuthorities()) {
                if (authority instanceof AssistantRole && Objects.equals(((AssistantRole) authority).getExercise(), exercise))
                    return true;
                else if (authority instanceof TutorRole &&
                        Objects.equals(((TutorRole) authority).getExercise(), exercise) &&
                        Objects.equals(((TutorRole) authority).getGroup(), group))
                    return true;
            }
        }
        return false;
    }

    public boolean hasShowUploadRight(String exercise, String group, String team, String sheet) {
        return isAccessible(exercise, sheet, new Team(group, team));
    }

    public boolean isAccessible(String exercise, String sheet, Team team) {
        STATSUserToken auth = getAuthentication();
        if (auth != null) {
            for (GrantedAuthority authority : auth.getAuthorities()) {
                if (authority instanceof AssistantRole && Objects.equals(((AssistantRole) authority).getExercise(), exercise))
                    return true;
                else if (authority instanceof TutorRole &&
                        Objects.equals(((TutorRole) authority).getExercise(), exercise) &&
                        Objects.equals(((TutorRole) authority).getGroup(), team.getGroup()))
                    return true;
                else if (authority instanceof StudentRole &&
                        Objects.equals(((StudentRole) authority).getExercise(), exercise) &&
                        ((StudentRole) authority).getGroup() != null &&
                        Objects.equals(((StudentRole) authority).getGroup(), team.getGroup()) &&
                        Objects.equals(((StudentRole) authority).getTeam(), team.getTeam()))
                    return true;
            }

            String studentid = auth.getStudentid();
            Team resultTeam = resultsDao.getTeamFromResults(exercise, sheet, studentid);
            return resultTeam != null && Objects.equals(resultTeam.getGroup(), team.getGroup()) && Objects.equals(resultTeam.getTeam(), team.getTeam());
        } else {
            return false;
        }
    }

    public boolean hasTestResultTopicRight(Message<?> message) {
        StompHeaderAccessor sha = StompHeaderAccessor.wrap(message);
        String topic = sha.getDestination();
        Pattern topicPattern = Pattern.compile("/topic/testresults/(?<exid>[^/]+)/(?<sheet>[^/]+)/(?<group>[^/]+)/(?<team>[^/]+)");
        Matcher matcher = topicPattern.matcher(topic);
        if (matcher.matches()) {
            String exid = matcher.group("exid");
            String sheet = matcher.group("sheet");
            String group = matcher.group("group");
            String team = matcher.group("team");
            return hasShowUploadRight(exid, group, team, sheet);
        } else {
            return false;
        }
    }

    public String getEmail() {
        return ((STATSUserToken) SecurityContextHolder.getContext().getAuthentication()).getEmail();
    }

    public static String getUsername() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    public List<GrantedAuthority> getUserAuthorities(int userId) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        for (ExerciseRights right : userDao.getUserRights(userId)) {
            switch (right.getRole()) {
                case assistant:
                    authorities.add(new AssistantRole(right.getExerciseId()));
                    break;
                case tutor:
                    authorities.add(new TutorRole(right.getExerciseId(), right.getGroupId()));
                    break;
                case student:
                    authorities.add(new StudentRole(right.getExerciseId(), right.getGroupId(), right.getTeamId()));
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported role type");
            }
        }
        return authorities;
    }
}

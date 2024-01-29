package de.tukl.softech.exclaim.security;

import de.tukl.softech.exclaim.dao.ResultsDao;
import de.tukl.softech.exclaim.dao.UserDao;
import de.tukl.softech.exclaim.data.Team;
import de.tukl.softech.exclaim.security.ExclaimAuthenticationProvider.StudentRole;
import de.tukl.softech.exclaim.transferdata.StudentInfo;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;

@Service
public class SecurityTools {
    private AccessChecker accessChecker;
    private ResultsDao resultsDao;
    private UserDao userDao;

    public SecurityTools(AccessChecker accessChecker, ResultsDao resultsDao, UserDao userDao) {
        this.accessChecker = accessChecker;
        this.resultsDao = resultsDao;
        this.userDao = userDao;
    }

    public Team getStudentTeam(String exercise, String sheetid) {
        ExclaimAuthenticationProvider.STATSUserToken userToken = accessChecker.getAuthentication();
        if (userToken == null) {
            return null;
        }
        Team team = resultsDao.getTeamFromResults(exercise, sheetid, userToken.getStudentid());
        if (team != null) {
            return team;
        }

        for (GrantedAuthority authority : userToken.getAuthorities()) {
            if (authority instanceof StudentRole) {
                StudentRole studentRole = ((StudentRole) authority);
                if (studentRole.getExercise().equals(exercise) && studentRole.getTeam() != null && !studentRole.getTeam().isEmpty()) {
                    team = new Team(studentRole.getGroup(), studentRole.getTeam());
                }
            }
        }

        return team;
    }

    public List<String> getAccessibleExercises() {
        ExclaimAuthenticationProvider.STATSUserToken auth = accessChecker.getAuthentication();
        if (auth == null) {
            return Collections.emptyList();
        }

        List<String> exercises = new ArrayList<>();
        for (GrantedAuthority authority : auth.getAuthorities()) {
            if (authority instanceof ExclaimAuthenticationProvider.ExerciseRole) {
                exercises.add(((ExclaimAuthenticationProvider.ExerciseRole) authority).getExercise());
            }
        }

        return exercises;
    }

    public List<Team> getAccessibleTeams(String exercise) {
        ExclaimAuthenticationProvider.STATSUserToken auth = accessChecker.getAuthentication();
        if (auth == null) {
            return Collections.emptyList();
        }

        List<String> groups = new ArrayList<>();
        for (GrantedAuthority authority : auth.getAuthorities()) {
            if (authority instanceof ExclaimAuthenticationProvider.AssistantRole) {
                return userDao.getExerciseTeams(exercise);
            } else if (authority instanceof ExclaimAuthenticationProvider.TutorRole) {
                ExclaimAuthenticationProvider.TutorRole tutorRole = (ExclaimAuthenticationProvider.TutorRole) authority;
                if (tutorRole.exercise.equals(exercise)) {
                    groups.add(tutorRole.getGroup());
                }
            }
        }
        if (groups.isEmpty()) {
            return Collections.emptyList();
        } else {
            return userDao.getExerciseTeams(exercise, groups);
        }
    }

    public List<StudentInfo> getAccessibleStudents(String exercise, String sheet) {
        List<StudentInfo> students;
        if (accessChecker.isOnlyStudent(exercise)) {
            Team studentTeam = getStudentTeam(exercise, sheet);
            if (studentTeam != null) {
                students = getTeamMembers(exercise, sheet, studentTeam);
            } else {
                students = emptyList();
            }
        } else {
            List<StudentInfo> allStudentsInExercise = userDao.getStudentsInExercise(exercise);
            resultsDao.updateStudentInfos(allStudentsInExercise, exercise, sheet);
            students = allStudentsInExercise.stream()
                    .filter(si -> si.getTeam() != null && si.getTeam().getTeam() != null
                            &&!si.getTeam().getTeam().isEmpty() && accessChecker.isAccessible(exercise, sheet, si.getTeam()))
                    .collect(Collectors.toList());
        }
        return students;
    }

    public Map<Team, List<StudentInfo>> getAccessibleStudentsByTeam(String exercise, String sheet) {
        List<StudentInfo> students = getAccessibleStudents(exercise, sheet);
        return students.stream().collect(Collectors.groupingBy(StudentInfo::getTeam));
    }

    public Map<String, List<StudentInfo>> getAccessibleStudentsByGroup(String exercise, String sheet) {
        List<StudentInfo> students = getAccessibleStudents(exercise, sheet);
        return students.stream().collect(Collectors.groupingBy(si -> si.getTeam().getGroup()));
    }

    public List<StudentInfo> getTeamMembers(String exercise, String sheet, Team team) {
        List<StudentInfo> membersInfo;
        List<String> membersOfTeam = resultsDao.getMembersOfTeam(exercise, sheet, team);
        if (membersOfTeam.isEmpty()) {
            membersInfo =  userDao.getStudentsInTeam(exercise, team);
        } else {
            membersInfo = userDao.getStudentInfos(exercise, membersOfTeam);
            membersInfo.forEach(si -> si.setTeam(team));
        }
        return membersInfo;
    }
}

package de.tukl.softech.exclaim.controllers;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import de.tukl.softech.exclaim.dao.ExerciseDao;
import de.tukl.softech.exclaim.dao.GroupDao;
import de.tukl.softech.exclaim.dao.UserDao;
import de.tukl.softech.exclaim.data.*;
import de.tukl.softech.exclaim.data.Exercise.GroupJoin;
import de.tukl.softech.exclaim.monitoring.MetricsService;
import de.tukl.softech.exclaim.security.AccessChecker;
import de.tukl.softech.exclaim.transferdata.ExerciseRights;
import de.tukl.softech.exclaim.transferdata.StudentInfo;
import de.tukl.softech.exclaim.utils.NaturalOrderComparator;
import model.other.PreferenceStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import service.optimize.GenerateLP2;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Collections.singletonList;

@Controller
public class GroupController {
    private static final Logger logger = LoggerFactory.getLogger(GroupController.class);

    private AccessChecker accessChecker;
    private MetricsService metrics;
    private UserDao userDao;
    private GroupDao groupDao;
    private ExerciseDao exerciseDao;

    @Value("${exclaim.csv.separator:;}")
    private char csvSeparator;

    public GroupController(AccessChecker accessChecker, MetricsService metrics,
                           UserDao userDao, GroupDao groupDao, ExerciseDao exerciseDao) {
        this.accessChecker = accessChecker;
        this.metrics = metrics;
        this.userDao = userDao;
        this.groupDao = groupDao;
        this.exerciseDao = exerciseDao;
    }

    @GetMapping("/exercise/{eid}/admin/groups")
    @PreAuthorize("@accessChecker.hasAdminRight(#eid)")
    public String getExerciseAdminGroupsPage(Model model, @PathVariable String eid) {
        metrics.registerAccessGroup();

        Exercise exercise = exerciseDao.getExercise(eid);
        List<Group> groups = groupDao.getGroupsForExercise(eid);
        Map<String, Integer> groupSizes = groupDao.getGroupSizes(eid);
        Map<String, List<User>> tutors = groupDao.getGroupTutors(eid);
        for (Group group : groups) {
            if (groupSizes.containsKey(group.getGroupId())) {
                group.setSize(groupSizes.get(group.getGroupId()));
            }
            if (tutors.containsKey(group.getGroupId())) {
                List<String> tutorNames = new ArrayList<>();
                tutors.get(group.getGroupId()).forEach(tutor -> tutorNames.add(tutor.getFirstname() + " " + tutor.getLastname()));
                group.setTutors(tutorNames);
            } else {
                group.setTutors(new ArrayList<>());
            }
        }
        model.addAttribute("exercise", exercise);
        model.addAttribute("groups", groups);
        return "group/group-admin";
    }

    @PostMapping("/exercise/{eid}/admin/groups")
    @PreAuthorize("@accessChecker.hasAdminRight(#eid)")
    public String postAddGroup(@PathVariable String eid,
                               @RequestParam String groupid,
                               @RequestParam String day,
                               @RequestParam String time,
                               @RequestParam String location,
                               @RequestParam String maxSize,
                               RedirectAttributes redirectAttributes) {
        metrics.registerAccessGroup();

        try {
            int size = Integer.parseInt(maxSize);
            Group group = new Group(eid, groupid, day, time, location, size);
            groupDao.createOrUpdateGroup(group);
            logger.info("adding group {} to exercise {}", groupid, eid);
        } catch (NumberFormatException e) {
            redirectAttributes.addFlashAttribute("errors",
                    singletonList("Ungültige Anzahl für maximale Größe!"));
        }
        return "redirect:/exercise/{eid}/admin/groups";
    }

    @PostMapping("/exercise/{eid}/admin/groups/{gid}/delete")
    @PreAuthorize("@accessChecker.hasAdminRight(#exercise)")
    public String postDeleteGroup(@PathVariable("eid") String exercise,
                                  @PathVariable("gid") String groupid,
                                  RedirectAttributes redirectAttributes) {
        metrics.registerAccessGroup();

        try {
            groupDao.deleteGroup(exercise, groupid);
        } catch (DataAccessException e) {
            redirectAttributes.addFlashAttribute("errors",
                    singletonList("Gruppe konnte nicht gelöscht werden, weil noch Studenten eingetragen sind!"));
        }

        return "redirect:/exercise/{eid}/admin/groups";
    }

    @PostMapping("/exercise/{exid}/admin/registrationStatus")
    @PreAuthorize("@accessChecker.hasAdminRight(#exercise)")
    public String postRegistrationStatus(@PathVariable("exid") String exercise,
                                         @RequestParam("registration") boolean registration) {
        metrics.registerAccessExercise();

        exerciseDao.updateRegistrationStatus(exercise, registration);
        return "redirect:/exercise/{exid}/admin/groups";
    }

    @PostMapping("/exercise/{exid}/admin/groupJoin")
    @PreAuthorize("@accessChecker.hasAdminRight(#exercise)")
    public String postGroupJoin(@PathVariable("exid") String exercise,
                                @RequestParam("groupjoin") String groupjoin) {
        metrics.registerAccessExercise();

        exerciseDao.updateGroupJoin(exercise, Exercise.GroupJoin.valueOf(groupjoin));
        return "redirect:/exercise/{exid}/admin/groups";
    }

    @GetMapping("/exercise/{eid}/admin/groups/friends")
    @PreAuthorize("@accessChecker.hasAdminRight(#eid)")
    public String getFriendsGraph(Model model, RedirectAttributes redirectAttributes, @PathVariable String eid) {
        List<GroupPreferences> groupPreferences = groupDao.getGroupPreferencesForExercise(eid);
        List<User> studentsInExercise = userDao.getStudentsUserInExercise(eid);
        Map<Integer, User> usersById = studentsInExercise.stream().collect(Collectors.toMap(User::getUserid, u -> u));

        StringBuilder graphJs = new StringBuilder();

        for (GroupPreferences preference : groupPreferences) {
            User u = usersById.get(preference.getUserId());
            if (u == null) {
                u = new User();
                u.setUsername("u_" + preference.getUserId());
                u.setFirstname("U" + preference.getUserId());
            }

            List<String> friends = preference.getFriends().stream().filter(s -> !s.isEmpty()).collect(Collectors.toList());
            if (friends.isEmpty())
                continue;

            graphJs.append("graph.addNode('")
                    .append(u.getUsername())
                    .append("', {firstname: '")
                    .append(u.getFirstname())
                    .append("', lastname: '")
                    .append(u.getLastname())
                    .append("'});\n");

            for (String friend : preference.getFriends()) {
                graphJs.append("graph.addLink('")
                        .append(u.getUsername())
                        .append("', '")
                        .append(friend)
                        .append("');\n");
            }

        }

        model.addAttribute("graphJs", graphJs);
        model.addAttribute("exercise", eid);
        return "friends-graph";
    }

    @GetMapping("/exercise/{eid}/admin/groups/preferences")
    @PreAuthorize("@accessChecker.hasAdminRight(#eid)")
    public String getExerciseAdminGroupsPreferenceOverviewPage(Model model, RedirectAttributes redirectAttributes, @PathVariable String eid) {
        return showExerciseAdminGroupsPreferenceOverview(model, redirectAttributes, eid, false);
    }

    @GetMapping("/exercise/{eid}/admin/groups/preferences-preview")
    @PreAuthorize("@accessChecker.hasAdminRight(#eid)")
    public String getExerciseAdminGroupsPreferenceOverviewPagePreview(Model model, RedirectAttributes redirectAttributes, @PathVariable String eid) {
        return showExerciseAdminGroupsPreferenceOverview(model, redirectAttributes, eid, true);
    }

    private String showExerciseAdminGroupsPreferenceOverview(Model model, RedirectAttributes redirectAttributes, String eid, boolean previewAssignment) {
        List<GroupPreferences> groupPreferences = groupDao.getGroupPreferencesForExercise(eid);
        List<Group> groups = groupDao.getGroupsForExercise(eid);
        groups.sort(Comparator.comparing(Group::getGroupId, NaturalOrderComparator.instance));

        List<User> studentsInExercise = userDao.getStudentsUserInExercise(eid);


        Map<Integer, User> usersById = studentsInExercise.stream().collect(Collectors.toMap(User::getUserid, u -> u));

        Map<User, GroupPreferences> preferencesByUser = groupPreferences.stream()
                .filter(x -> usersById.containsKey(x.getUserId()))
                .collect(Collectors.toMap(x -> usersById.get(x.getUserId()), x -> x));

        Map<String, Team> studentId2Team = new HashMap<>();
        for (Group group : groups) {
            List<StudentInfo> studentsInGroup = userDao.getStudentsInGroup(eid, group.getGroupId());
            for (StudentInfo si : studentsInGroup) {
                String studentId = si.getId();
                studentId2Team.put(studentId, si.getTeam());
            }
        }


        List<UserPreferenceInfo> prefInfos = studentsInExercise.stream()
                .map(user -> {
                    GroupPreferences pref = preferencesByUser.get(user);
                    if (pref == null) {
                        pref = new GroupPreferences(eid, user.getUserid(), Collections.emptyMap(), Collections.emptyList());
                    }
                    Team team = studentId2Team.get(user.getStudentid());
                    return new UserPreferenceInfo(user, pref, team);
                })
                .collect(Collectors.toList());

        if (previewAssignment) {
            try {
                Map<String, List<Integer>> groupAssignment = calculateGroupAssignment(eid);
                Map<Integer, String> userGroup = new HashMap<>();
                for (Map.Entry<String, List<Integer>> e : groupAssignment.entrySet()) {
                    for (Integer uId : e.getValue()) {
                        userGroup.put(uId, e.getKey());
                    }
                }


                prefInfos = prefInfos.stream()
                        .map(p -> p.withGroup(userGroup.get(p.user.getUserid())))
                        .collect(Collectors.toList());


            } catch (IOException | InterruptedException e) {
                redirectAttributes.addFlashAttribute("errors",
                        singletonList(e.getLocalizedMessage()));
                return "redirect:/exercise/{eid}/admin/groups/preferences";
            }
        }


        model.addAttribute("exercise", eid);
        model.addAttribute("groups", groups);
        model.addAttribute("prefInfos", prefInfos);
        model.addAttribute("previewAssignment", previewAssignment);
        return "preferences-overview";
    }

    @GetMapping("/exercise/{eid}/groups")
    @PreAuthorize("@accessChecker.canAccess(#eid)")
    public String getExerciseGroupsPage(Model model, @PathVariable String eid) {
        metrics.registerAccessGroup();

        Exercise exercise = exerciseDao.getExercise(eid);
        boolean isOnlyStudent = accessChecker.isOnlyStudent(eid);
        List<Group> groups = groupDao.getGroupsForExercise(eid).stream().sorted((o1, o2) -> NaturalOrderComparator.instance.compare(o1.getGroupId(), o2.getGroupId())).collect(Collectors.toList());
        Map<String, Integer> groupSizes = groupDao.getGroupSizes(eid);
        Map<String, List<User>> tutors = groupDao.getGroupTutors(eid);
        for (Group group : groups) {
            if (groupSizes.containsKey(group.getGroupId())) {
                group.setSize(groupSizes.get(group.getGroupId()));
            }
            if (tutors.containsKey(group.getGroupId())) {
                List<String> tutorNames = new ArrayList<>();
                tutors.get(group.getGroupId()).forEach(tutor -> tutorNames.add(tutor.getFirstname() + " " + tutor.getLastname()));
                group.setTutors(tutorNames);
            } else {
                group.setTutors(new ArrayList<>());
            }
        }
        int userid = accessChecker.getAuthentication().getUserid();
        List<ExerciseRights> exerciseRights = userDao.getUserRightsForExercise(userid, eid);

        Set<String> joinedGroups = Collections.emptySet();
        if (exerciseRights != null) {
            joinedGroups = exerciseRights.stream().map(ExerciseRights::getGroupId).filter(x -> x != null).collect(Collectors.toSet());
        }
        model.addAttribute("joinedGroups", joinedGroups);
        model.addAttribute("exercise", exercise);
        model.addAttribute("groups", groups);
        model.addAttribute("isOnlyStudent", isOnlyStudent);
        model.addAttribute("isAssistant", accessChecker.hasAdminRight(eid));

        return "group/groups";
    }

    @PostMapping("/exercise/{exid}/groups/join")
    @PreAuthorize("@accessChecker.canAccess(#eid)")
    public String postGroupJoin(@PathVariable("exid") String eid,
                                @RequestParam("join") boolean join,
                                @RequestParam("group") String groupid,
                                RedirectAttributes redirectAttributes) {
        metrics.registerAccessGroup();

        Exercise exercise = exerciseDao.getExercise(eid);
        Group group = groupDao.getGroup(eid, groupid);
        int userid = accessChecker.getAuthentication().getUserid();
        List<ExerciseRights> exerciseRights = userDao.getUserRightsForExercise(userid, eid);
        ExerciseRights exerciseRight;
        if (exerciseRights.size() > 1) {
            redirectAttributes.addFlashAttribute("errors",
                    singletonList("Gruppeneintragung nur für Studenten möglich."));
            return "redirect:/exercise/{exid}/groups";
        } else {
            //size > 0 because user can access
            exerciseRight = exerciseRights.get(0);
        }

        if (exercise == null) {
            redirectAttributes.addFlashAttribute("errors",
                    singletonList("Fehler beim Laden der Übung."));
        } else if (group == null) {
            redirectAttributes.addFlashAttribute("errors",
                    singletonList("Gruppe konnte nicht gefunden werden."));
        } else if (exercise.getGroupJoin() != GroupJoin.GROUP) {
            redirectAttributes.addFlashAttribute("errors",
                    singletonList("Gruppeneintragung aktuell nicht möglich"));
        } else if (!accessChecker.isOnlyStudent(eid)) {
            redirectAttributes.addFlashAttribute("errors",
                    singletonList("Gruppeneintragung nur für Studenten möglich."));
        } else if (exerciseRight.getGroupId() != null && join) {
            redirectAttributes.addFlashAttribute("errors",
                    singletonList("Sie sind bereits in einer Gruppe."));
        } else if (join && group.getMaxSize() <= groupDao.getGroupSize(eid, groupid)) {
            redirectAttributes.addFlashAttribute("errors",
                    singletonList("Die Gruppe ist bereits voll"));
        } else {
            if (join) {
                exerciseRight.setGroupId(groupid);
            } else {
                exerciseRight.setGroupId(null);
                exerciseRight.setTeamId(null);
            }

            userDao.updateUserRights(userid, exerciseRight);
            accessChecker.updateRights();
        }

        return "redirect:/exercise/{exid}/groups";
    }

    @GetMapping("/exercise/{exid}/groups/admin/import")
    @PreAuthorize("@accessChecker.hasAdminRight(#exercise)")
    public String getGroupImport(@PathVariable("exid") String exercise,
                                 Model model) {
        metrics.registerAccessGroup();

        model.addAttribute("exercise", exercise);
        return "group/group-import";
    }

    @PostMapping("/exercise/{exid}/groups/admin/import")
    @PreAuthorize("@accessChecker.hasAdminRight(#exercise)")
    public String postGroupImport(@PathVariable("exid") String exercise,
                                  @RequestParam("rows") String rows,
                                  RedirectAttributes redirectAttributes) {
        metrics.registerAccessGroup();
        List<ExerciseRights> exerciseRights = new ArrayList<>();
        List<UserImport> users = new ArrayList<>();
        StringJoiner notImported = new StringJoiner(", ");

        String[] lines = rows.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.length() == 0) continue;
            String[] columns = line.split(String.valueOf(csvSeparator));
            if (columns.length < 2) {
                notImported.add(columns[0]);
                continue;
            }
            String group = columns[0];
            String username = columns[1];
            String team = null;

            if (columns.length > 2) {
                team = columns[2];
            }
            users.add(new UserImport(username, group, team));
        }

        Map<String, Integer> userIds = userDao.getUserIds(users.stream().map(u -> u.username).collect(Collectors.toList()));
        Set<String> groups = groupDao.getGroupsForExercise(exercise).stream().map(Group::getGroupId).collect(Collectors.toSet());

        for (UserImport user : users) {
            if (userIds.containsKey(user.username) && groups.contains(user.group)) {
                ExerciseRights exerciseRight = new ExerciseRights();
                exerciseRight.setUserId(userIds.get(user.username));
                exerciseRight.setExerciseId(exercise);
                exerciseRight.setGroupId(user.group);
                exerciseRight.setTeamId(user.team);
                exerciseRight.setRole(ExerciseRights.Role.student);
                exerciseRights.add(exerciseRight);
            } else {
                notImported.add(user.username);
            }
        }

        userDao.mergeUserRights(exerciseRights);

        redirectAttributes.addFlashAttribute("resultmessage", exerciseRights.size() + " Studenten importiert!");
        if (notImported.length() > 0) {
            redirectAttributes.addFlashAttribute("errors",
                    singletonList("Folgende Studenten konnten nicht importiert werden: " + notImported.toString()));
        }

        return "redirect:/exercise/{exid}/groups/admin/import";
    }

    @GetMapping("/exercise/{eid}/groups/preferences")
    @PreAuthorize("@accessChecker.canAccess(#eid)")
    public String getExerciseGroupPreferencesPage(Model model, @PathVariable String eid) {
        metrics.registerAccessGroup();

        Exercise exercise = exerciseDao.getExercise(eid);

        if (exercise == null) return "group/group-preferences";

        List<Group> groups = groupDao.getGroupsForExercise(eid).stream().sorted((o1, o2) -> NaturalOrderComparator.instance.compare(o1.getGroupId(), o2.getGroupId())).collect(Collectors.toList());

        int userid = accessChecker.getAuthentication().getUserid();
        GroupPreferences preferences = groupDao.getGroupPreferences(eid, userid);
        if (preferences == null) {
            preferences = new GroupPreferences(eid, userid);
        }

        Map<String, String> usernames = new HashMap<>();
        for (int i = 0; i < preferences.getFriends().size(); i++) {
            usernames.put("user" + (i + 1), preferences.getFriends().get(i));
        }

        model.addAttribute("exercise", exercise);
        model.addAttribute("groups", groups);
        model.addAttribute("usernames", usernames);
        model.addAttribute("preferences", preferences);

        return "group/group-preferences";
    }

    @PostMapping("/exercise/{eid}/groups/preferences")
    @PreAuthorize("@accessChecker.canAccess(#eid)")
    public String postGroupPreferences(@PathVariable("eid") String eid,
                                       @RequestBody MultiValueMap<String, String> formData,
                                       RedirectAttributes redirectAttributes) {
        metrics.registerAccessGroup();

        Exercise exercise = exerciseDao.getExercise(eid);
        int userid = accessChecker.getAuthentication().getUserid();
        if (exercise == null) {
            redirectAttributes.addFlashAttribute("errors",
                    singletonList("Fehler beim Laden der Übung."));
            return "redirect:/exercise/{eid}/groups";
        } else if (exercise.getGroupJoin() != Exercise.GroupJoin.PREFERENCES) {
            redirectAttributes.addFlashAttribute("errors",
                    singletonList("Wählen der Präferenzen aktuell nicht möglich."));
            return "redirect:/exercise/{eid}/groups";
        } else if (!accessChecker.isOnlyStudent(eid)) {
            redirectAttributes.addFlashAttribute("errors",
                    singletonList("Wählen der Präferenzen nur für Studenten möglich."));
            return "redirect:/exercise/{eid}/groups";
        }

        GroupPreferences preferences = new GroupPreferences(eid, userid);
        Map<String, String> usernameErrors = new HashMap<>();

        for (Map.Entry<String, List<String>> entry : formData.entrySet()) {
            if (entry.getKey().equals("_csrf")) continue;
            if (entry.getKey().contains("user")) {
                if (entry.getValue().get(0).isEmpty()) {
                    continue;
                } else if (userDao.getUser(entry.getValue().get(0)) != null) {
                    preferences.getFriends().add(entry.getValue().get(0));
                } else {
                    usernameErrors.put(entry.getKey(), "Unbekannter Nutzername!");
                }
            } else {
                preferences.getPreferences().put(entry.getKey(), PreferenceStatus.fromInternalValue(Integer.valueOf(entry.getValue().get(0))));
            }
        }

        List<Group> groups = groupDao.getGroupsForExercise(eid);
        List<String> errors = new ArrayList<>();
        if (preferences.getPreferences().size() < groups.size()) {
            errors.add("Bitte wählen Sie zu allen Terminen eine Präferenz aus!");
        }
        if (preferences.getPreferences().values().stream().filter(p -> p == PreferenceStatus.IMPOSSIBLE).count() > (groups.size() / 2)) {
            errors.add("Bitte wählen Sie weniger Termine als unmöglich aus!");
        }
        if (errors.size() > 0) {
            redirectAttributes.addFlashAttribute("errors", errors);
        }

        if (!usernameErrors.isEmpty()) {
            redirectAttributes.addFlashAttribute("usernameErrors", usernameErrors);
        }

        groupDao.mergeGroupPreferences(preferences);
        redirectAttributes.addFlashAttribute("success", "Präferenzen erfolgreich aktualisiert!");
        return "redirect:/exercise/{eid}/groups/preferences";
    }

    @GetMapping("/exercise/{exid}/groups/admin/calculateGroups")
    @PreAuthorize("@accessChecker.hasAdminRight(#eid)")
    public String postGroupCalculate(@PathVariable("exid") String eid,
                                     RedirectAttributes redirectAttributes) {
        metrics.registerAccessGroup();
        try {
            Exercise exercise = exerciseDao.getExercise(eid);
            if (exercise == null) {
                redirectAttributes.addFlashAttribute("errors",
                        singletonList("Fehler beim Laden der Übung."));
                return "redirect:/exercise/{exid}/admin/groups";
            }

            Map<String, List<Integer>> assignment = calculateGroupAssignment(eid);

            List<ExerciseRights> exerciseRights = new LinkedList<>();

            for (Map.Entry<String, List<Integer>> entry : assignment.entrySet()) {
                for (Integer user : entry.getValue()) {
                    ExerciseRights exerciseRight = new ExerciseRights();
                    exerciseRight.setExerciseId(eid);
                    exerciseRight.setGroupId(entry.getKey());
                    exerciseRight.setUserId(user);
                    exerciseRight.setRole(ExerciseRights.Role.student);
                    exerciseRights.add(exerciseRight);
                }
            }

            userDao.mergeUserRights(exerciseRights);
            redirectAttributes.addFlashAttribute("success", "Gruppen erfolgreich eingeteilt!");

        } catch (IOException | InterruptedException | IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errors",
                    singletonList(e.getLocalizedMessage()));
            return "redirect:/exercise/{exid}/admin/groups";
        }

        return "redirect:/exercise/{exid}/admin/groups";
    }

    /**
     * Calculates group assignment for the given exercise.
     * Returns a mapping from group-id to list of student ids
     */
    private Map<String, List<Integer>> calculateGroupAssignment(String eid) throws IOException, InterruptedException {
        List<GroupPreferences> groupPreferences = groupDao.getGroupPreferencesForExercise(eid);
        List<Group> groups = groupDao.getGroupsForExercise(eid);

        List<User> studentsInExercise = userDao.getStudentsUserInExercise(eid);

        Map<String, Integer> studentUsernamesWithIds = studentsInExercise.stream().collect(Collectors.toMap(User::getUsername, User::getUserid));
        Set<Integer> studentUserIds = studentsInExercise.stream().map(User::getUserid).collect(Collectors.toSet());

        Map<Integer, Map<String, PreferenceStatus>> preferences = new LinkedHashMap<>();
        Multimap<Integer, Integer> friends = LinkedHashMultimap.create();

        for (GroupPreferences preference : groupPreferences) {
            if (!studentUserIds.contains(preference.getUserId())) continue;

            for (Group group : groups) {
                if (!preference.getPreferences().containsKey(group.getGroupId())) {
                    preference.getPreferences().put(group.getGroupId(), PreferenceStatus.UNDEFINED);
                }
            }

            preferences.put(preference.getUserId(), preference.getPreferences());
            friends.removeAll(preference.getUserId());
            for (String friend : preference.getFriends()) {
                if (studentUsernamesWithIds.containsKey(friend)) {
                    friends.put(preference.getUserId(), studentUsernamesWithIds.get(friend));
                }
            }
        }

        int required = (int) Math.ceil(preferences.size() * 1. / groups.size());
        int minUsersPerGroup = required - 1;
        int maxUsersPerGroup = required + 1;

        Map<String, Integer> groupsPerSlot = new HashMap<>();
        groups.forEach(group -> groupsPerSlot.put(group.getGroupId(), 1));


        Map<String, List<Integer>> assignment = new HashMap<>();
        int maxTries = 3;
        for (int i = 0; i < maxTries; i++) {
            StringBuilder errors = new StringBuilder();
            try {
                GenerateLP2<Integer, String> generateLP = new GenerateLP2<>(preferences, groupsPerSlot, friends, groups.size(), minUsersPerGroup, maxUsersPerGroup);
                assignment = generateLP.calculateSlots();
                break;
            } catch (RuntimeException ex) {
                errors.append("Failed with minUsersPerGroup = ")
                        .append(minUsersPerGroup)
                        .append(" and maxUsersPerGroup = ")
                        .append(maxUsersPerGroup)
                        .append(":\n")
                        .append(ex.toString())
                        .append("\n\n");
                //Problem not solved
                minUsersPerGroup--;
                maxUsersPerGroup++;
            }
            if (i == maxTries - 1) {
                throw new IllegalArgumentException("Could not complete lp_solve:\n\n" + errors);
            }
        }
        return assignment;
    }

    static public class UserPreferenceInfo {
        public final User user;
        public final GroupPreferences groupPreferences;
        public final @Nullable
        Team team;

        public UserPreferenceInfo(User user, GroupPreferences groupPreferences, @Nullable Team team) {
            this.user = user;
            this.groupPreferences = groupPreferences;
            this.team = team;
        }

        public UserPreferenceInfo withGroup(String group) {
            return new UserPreferenceInfo(user, groupPreferences, new Team(group, null));
        }

        public PreferenceStatus getPreference(String groupId) {
            PreferenceStatus res = groupPreferences.getPreferences().get(groupId);
            if (res == null) {
                return PreferenceStatus.UNDEFINED;
            }
            return res;
        }

    }

    private class UserImport {
        String username;
        String group;
        String team;

        UserImport(String username, String group, String team) {
            this.username = username;
            this.group = group;
            this.team = team;
        }
    }

}

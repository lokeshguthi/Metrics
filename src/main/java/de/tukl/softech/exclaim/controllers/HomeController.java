package de.tukl.softech.exclaim.controllers;

import de.tukl.softech.exclaim.dao.ExerciseDao;
import de.tukl.softech.exclaim.data.Exercise;
import de.tukl.softech.exclaim.monitoring.MetricsService;
import de.tukl.softech.exclaim.security.SecurityTools;
import de.tukl.softech.exclaim.utils.NaturalOrderComparator;
import de.tukl.softech.exclaim.utils.Semester;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Controller
public class HomeController {
    private ExerciseDao exerciseDao;
    private SecurityTools securityTools;
    private MetricsService metrics;

    public HomeController(ExerciseDao exerciseDao, SecurityTools securityTools,
                          MetricsService metrics) {
        this.exerciseDao = exerciseDao;
        this.securityTools = securityTools;
        this.metrics = metrics;
    }

    @ModelAttribute("page")
    public String getPage() {
        return "home";
    }

    @GetMapping("/")
    public String getHome(Model model) {
        metrics.registerAccessHome();
        List<Exercise> allExercises = exerciseDao.getAllExercises();
        List<String> accessibleExercises = securityTools.getAccessibleExercises();
        List<Exercise> exercises = allExercises.stream()
                .filter(e -> accessibleExercises.contains(e.getId()))
                .sorted(Comparator.comparing((Exercise e) -> Semester.fromString(e.getTerm())).reversed())
                .collect(Collectors.toList());
        model.addAttribute("exercises", exercises);
        return "home";
    }

    @GetMapping("/login")
    public String getLogin() {
        metrics.registerAccessHome();
        return "login";
    }
}

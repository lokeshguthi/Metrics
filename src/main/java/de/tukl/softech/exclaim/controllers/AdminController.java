package de.tukl.softech.exclaim.controllers;

import com.google.common.collect.ImmutableMap;
import com.opencsv.CSVWriter;
import de.tukl.softech.exclaim.dao.*;
import de.tukl.softech.exclaim.monitoring.MetricsService;
import de.tukl.softech.exclaim.security.AccessChecker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Controller
public class AdminController {
    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);
    private final UserDao userDao;
    private MetricsService metrics;
    private JdbcTemplate jdbcTemplate;
    private TransactionTemplate transactionTemplate;
    private AttendanceDao attendanceDao;
    private DeltapointsDao deltapointsDao;
    private ResultsDao resultsDao;
    private UploadsDao uploadsDao;

    public AdminController(MetricsService metrics, JdbcTemplate jdbcTemplate, TransactionTemplate transactionTemplate, AttendanceDao attendanceDao, DeltapointsDao deltapointsDao, ResultsDao resultsDao, UploadsDao uploadsDao, UserDao userDao) {
        this.metrics = metrics;
        this.jdbcTemplate = jdbcTemplate;
        this.transactionTemplate = transactionTemplate;
        this.attendanceDao = attendanceDao;
        this.deltapointsDao = deltapointsDao;
        this.resultsDao = resultsDao;
        this.uploadsDao = uploadsDao;
        this.userDao = userDao;
    }

    @ModelAttribute("page")
    public String getPage() {
        return "admin";
    }

    @GetMapping("/admin")
    @PreAuthorize("@accessChecker.isAdmin()")
    public String getAdminPage(@RequestParam(value = "query", required = false) String query,
                               Model model) {
        metrics.registerAccessAdmin();
        if (query != null) {
            model.addAttribute("query", query);
        }
        return "sqladmin";
    }

    @PostMapping("/admin/update")
    @PreAuthorize("@accessChecker.isAdmin()")
    public String postUpdate(@RequestParam("query") String query,
                             @RequestParam("expected-updates") int expectedUpdates,
                             RedirectAttributes redirectAttributes,
                             Model model) {
        logger.info("User " + AccessChecker.getUsername() + " executed update:\n" + query);
        metrics.registerAccessAdmin();
        try {


            AtomicBoolean success = new AtomicBoolean(true);
            AtomicInteger affectedRowsC = new AtomicInteger();
            AtomicReference<List<Map<String, Object>>> oldDataR = new AtomicReference<>();
            AtomicReference<List<Map<String, Object>>> newDataR = new AtomicReference<>();
            transactionTemplate.execute((status) -> {
                int flags = Pattern.CASE_INSENSITIVE | Pattern.DOTALL;
                Pattern updatePattern = Pattern.compile("UPDATE\\s+(?<table>\\S+)\\s.*WHERE(?<where>.*)", flags);
                Pattern deletePattern = Pattern.compile("DELETE FROM\\s+(?<table>\\S+)\\s+WHERE(?<where>.*)", flags);
                Pattern insertPattern = Pattern.compile("INSERT INTO.*", flags);

                String selectQuery = null;
                Matcher matcher;
                if ((matcher = updatePattern.matcher(query)).matches()
                        || (matcher = deletePattern.matcher(query)).matches()) {
                    String table = matcher.group("table");
                    String where = matcher.group("where");
                    selectQuery = "SELECT * FROM " + table + " WHERE " + where;
                } else if (insertPattern.matcher(query).matches()) {
                    logger.info("executing insert");
                } else {
                    throw new RuntimeException("This kind of update is not supported.");
                }

                if (selectQuery != null) {
                    List<Map<String, Object>> list = jdbcTemplate.queryForList(selectQuery);
                    oldDataR.set(list);
                    logger.info("Old objects: " + list.size() +
                            "\nselected by " + selectQuery +
                            "\n" + list.stream().map(Object::toString).collect(Collectors.joining("\n")));
                }


                int affectedRows = jdbcTemplate.update(query);
                affectedRowsC.set(affectedRows);
                if (affectedRows != expectedUpdates) {
                    status.setRollbackOnly();
                    success.set(false);
                    return null;
                }

                if (selectQuery != null) {
                    List<Map<String, Object>> list = jdbcTemplate.queryForList(selectQuery);
                    newDataR.set(list);
                    logger.info("New objects: " + list.size() +
                            "\nselected by " + selectQuery +
                            "\n" + list.stream().map(Object::toString).collect(Collectors.joining("\n")));
                }

                return null;
            });

            if (success.get()) {

                List<Map<String, Object>> newData = newDataR.get();
                List<Map<String, Object>> oldData = oldDataR.get();

                List<String> columns = new ArrayList<>();
                List<Map<String, Object>> rows = new ArrayList<>();
                if (oldData != null && !oldData.isEmpty()) {
                    columns.addAll(oldData.get(0).keySet());
                    columns.add("database-version");
                    for (Map<String, Object> d : oldData) {
                        Map<String, Object> m = new LinkedHashMap<>(d);
                        m.put("database-version", "OLD");
                        rows.add(m);
                    }
                    for (Map<String, Object> d : newData) {
                        Map<String, Object> m = new LinkedHashMap<>(d);
                        m.put("database-version", "NEW");
                        rows.add(m);
                    }
                } else {
                    columns.add("Affected Rows");
                    rows.add(ImmutableMap.of("Affected Rows", affectedRowsC.get()));
                }
                model.addAttribute("rows", rows);
                model.addAttribute("columns", columns);
                model.addAttribute("query", query);
                model.addAttribute("message", "Database was updated with " + affectedRowsC.get() + " rows affected.");
                return "sqladmin";
            } else {
                redirectAttributes.addFlashAttribute("message", "ERROR: suggested number of affected rows was not correct (it would have affected " + affectedRowsC.get() + " rows)");
            }

            redirectAttributes.addFlashAttribute("query", query);
            return "redirect:/admin";
        } catch (Exception e) {
            logger.info("Error executing update " + query, e);
            redirectAttributes.addFlashAttribute("message", e.getMessage());
            redirectAttributes.addFlashAttribute("query", query);
            return "redirect:/admin";
        }
    }

    @PostMapping("/admin/query")
    @PreAuthorize("@accessChecker.isAdmin()")
    public String postQuery(@RequestParam("query") String query,
                            RedirectAttributes redirectAttributes,
                            Model model) {
        metrics.registerAccessAdmin();
        try {
            List<String> columns = new ArrayList<>();
            List<Map<String, Object>> rows = new ArrayList<>();
            jdbcTemplate.query(query, (ResultSetExtractor<Void>) (ResultSet rs) -> {
                ResultSetMetaData rsmd = rs.getMetaData();
                for (int i = 1; i <= rsmd.getColumnCount(); i++) {
                    columns.add(rsmd.getColumnName(i));
                }


                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    for (int i = 0; i < columns.size(); i++) {
                        row.put(columns.get(i), rs.getObject(i + 1));
                    }
                    rows.add(row);
                }
                return null;
            });
            model.addAttribute("rows", rows);
            model.addAttribute("columns", columns);
            model.addAttribute("query", query);
            return "sqladmin";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("message", e.getMessage());
            redirectAttributes.addFlashAttribute("query", query);
            return "redirect:/admin";
        }
    }

    @GetMapping("/admin/changeStudentid")
    @PreAuthorize("@accessChecker.isAdmin()")
    public String getChangeStudentIdPage(Model model) {
        metrics.registerAccessAdmin();
        return "changeStudentid";
    }

    @PostMapping("/admin/changeStudentid")
    @PreAuthorize("@accessChecker.isAdmin()")
    public String postChangeStudentIdPage(@RequestParam("oldId") String oldId, @RequestParam("newId") String newId, Model model) {
        metrics.registerAccessAdmin();

        String message;
        try {
            message = transactionTemplate.execute(status -> {
                int count = 0;
                count += userDao.updateStudentId(oldId, newId);
                count += attendanceDao.updateStudentId(oldId, newId);
                count += deltapointsDao.updateStudentId(oldId, newId);
                count += resultsDao.updateStudentId(oldId, newId);
                count += uploadsDao.updateStudentId(oldId, newId);
                return "Matrikelnummer erfolgreich geändert: " + count + " Vorkommen.";
            });
        } catch (IllegalArgumentException e) {
            message = "Matrikelnummer konnte nicht geändert werden: " + e.getMessage();
        }
        model.addAttribute("message", message);
        return "changeStudentid";

    }
}

package de.tukl.softech.exclaim.controllers;

import de.tukl.softech.exclaim.dao.ExerciseDao;
import de.tukl.softech.exclaim.dao.ResultsDao;
import de.tukl.softech.exclaim.dao.SheetDao;
import de.tukl.softech.exclaim.data.Exercise;
import de.tukl.softech.exclaim.transferdata.APIExercise;
import de.tukl.softech.exclaim.transferdata.APIExerciseResult;
import de.tukl.softech.exclaim.transferdata.APIResult;
import de.tukl.softech.exclaim.transferdata.APISheet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class APIController {
    private final ExerciseDao exerciseDao;
    private SheetDao sheetDao;
    private ResultsDao resultsDao;

    @Autowired
    public APIController(ExerciseDao exerciseDao, SheetDao sheetDao, ResultsDao resultsDao) {
        this.exerciseDao = exerciseDao;
        this.sheetDao = sheetDao;
        this.resultsDao = resultsDao;
    }

    @GetMapping("/exercises")
    public List<APIExercise> getExercises() {
        return exerciseDao.getAllExercises().stream()
                .map(e ->new APIExercise(e,
                        sheetDao.getSheetsForExercise(e.getId()).stream()
                                .map(APISheet::new).collect(Collectors.toList())))
                .collect(Collectors.toList());
    }

    @PutMapping("/exercise")
    public void putExercise(@RequestBody APIExercise apiExercise) {
        exerciseDao.createOrUpdateExercise(apiExercise.toExercise());
        apiExercise.getSheets().stream()
                .map(s -> s.toSheet(apiExercise.getId()))
                .forEach(s -> sheetDao.createOrUpdateSheet(s));
    }

    @GetMapping("/exercise/{exid}")
    public ResponseEntity<APIExercise> getExercise(@PathVariable("exid") String exercise) {
        Exercise exer = exerciseDao.getExercise(exercise);
        if (exer == null) {
            return ResponseEntity.notFound().build();
        } else {
            return ResponseEntity.ok(new APIExercise(exer,
                    sheetDao.getSheetsForExercise(exercise).stream()
                            .map(APISheet::new).collect(Collectors.toList())));
        }
    }

    @GetMapping("/exercise/{exid}/results")
    public List<APIExerciseResult> getExerciseResults(@PathVariable("exid") String exercise) {
        Map<String, List<APIResult>> resultsBySheet = resultsDao.getResultsForExercise(exercise);
        Map<String, Double> maxpointsBySheet = sheetDao.getMaxPointsForExercise(exercise);

        return resultsBySheet.entrySet().stream()
                .map(rbs -> new APIExerciseResult(rbs.getKey(), maxpointsBySheet.get(rbs.getKey()), rbs.getValue()))
                .collect(Collectors.toList());
    }
}

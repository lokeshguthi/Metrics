package de.tukl.softech.exclaim.transferdata;

import java.util.ArrayList;
import java.util.List;

public class UserRights {
    private String realname;
    private String email;
    private String studentid;
    private List<ExerciseRights> exerciseRights = new ArrayList<ExerciseRights>();

    public String getRealname() {
        return realname;
    }

    public void setRealname(String realname) {
        this.realname = realname;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getStudentid() {
        return studentid;
    }

    public void setStudentid(String studentid) {
        this.studentid = studentid;
    }

    public List<ExerciseRights> getExerciseRights() {
        return exerciseRights;
    }

    public void setExerciseRights(List<ExerciseRights> exerciseRights) {
        this.exerciseRights = exerciseRights;
    }
}

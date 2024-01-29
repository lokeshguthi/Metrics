package de.tukl.softech.exclaim.transferdata;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ClocResult {

    private String name;
    private int comments_number;
    private int loc_number;


    public ClocResult(String name, int comments_number, int loc_number) {
        this.name = name;
        this.comments_number = comments_number;
        this.loc_number = loc_number;
    }

    public ClocResult(){

    }

    public int getComments_number() {
        return comments_number;
    }

    public void setComments_number(int comments_number) {
        this.comments_number = comments_number;
    }

    public int getLoc_number() {
        return loc_number;
    }

    public void setLoc_number(int loc_number) {
        this.loc_number = loc_number;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}

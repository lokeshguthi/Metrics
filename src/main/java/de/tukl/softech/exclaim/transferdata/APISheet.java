package de.tukl.softech.exclaim.transferdata;

import de.tukl.softech.exclaim.data.Sheet;

public class APISheet {
    private String id;
    private String label;

    public APISheet() {
    }

    public APISheet(Sheet sheet) {
        this.id = sheet.getId();
        this.label = sheet.getLabel();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public Sheet toSheet(String exercise) {
        return new Sheet(id, exercise, label);
    }
}

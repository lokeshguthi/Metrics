package de.tukl.softech.exclaim.utils;

import de.tukl.softech.exclaim.data.Team;

public class TeamConverter {
	private final static char sep;
	
	static {
		if (System.getProperty("os.name").toUpperCase().contains("WINDOWS")) {
			sep = '-';
		} else {
			sep = '|';
		}
	}
	
    public static String convertToString(Team attribute) {
    	
        if (attribute == null)
            return null;
        return attribute.getGroup() + sep + attribute.getTeam();
    }

    public static Team convertToTeam(String dbData) {
        if (dbData == null)
            return null;
        String[] split = dbData.split("[" + sep + "]");
        return new Team(split[0], split[1]);
    }
}

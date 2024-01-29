package de.tukl.softech.exclaim.utils;

import de.tukl.softech.exclaim.data.Team;
import org.joda.time.DateTime;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;

public class BeanParameterSource extends BeanPropertySqlParameterSource {
    /**
     * Create a new BeanParameterSource for the given bean.
     *
     * @param object the bean instance to wrap
     */
    public BeanParameterSource(Object object) {
        super(object);
    }

    @Override
    public Object getValue(String paramName) throws IllegalArgumentException {
        Object result = super.getValue(paramName);
        if (result instanceof DateTime) {
            return ((DateTime) result).toDate();
        } else if (result instanceof Team) {
            return TeamConverter.convertToString((Team) result);
        } else {
            return result;
        }
    }
}

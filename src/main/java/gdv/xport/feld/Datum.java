/*
 * $Id$
 *
 * Copyright (c) 2009 by Oliver Boehm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express orimplied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * (c)reated 04.10.2009 by oliver (ob@oasd.de)
 */

package gdv.xport.feld;

import java.text.*;
import java.util.*;

import net.sf.oval.ConstraintViolation;
import net.sf.oval.constraint.MatchPatternCheck;
import net.sf.oval.context.ClassContext;

import org.apache.commons.logging.*;

/**
 * The Class Datum.
 *
 * @author oliver
 * @since 04.10.2009
 * @version $Revision$
 */
public final class Datum extends Feld {

    private static final Log log = LogFactory.getLog(Feld.class);
    private final DateFormat dateFormat;

    /**
     * Instantiates a new datum.
     *
     * @param name the name
     * @param start the start
     */
    public Datum(final String name, final int start) {
        this(name, 8, start);
    }

    /**
     * Instantiates a new datum.
     *
     * @param name the name
     * @param inhalt Datum der Form "ddmmjjjj" oder "ddjjjj" oder "dd"
     */
    public Datum(final String name, final String inhalt) {
        this(name, inhalt.length(), 1, inhalt);
    }

    /**
     * Instantiates a new datum.
     *
     * @param name the name
     * @param length the length
     * @param start the start
     */
    public Datum(final String name, final int length, final int start) {
        super(name, length, start, Align.RIGHT);
        dateFormat = getDateFormat(length);
    }

    /**
     * Instantiates a new datum.
     *
     * @param name the name
     * @param length the length
     * @param start the start
     * @param inhalt Datum der Form "ddmmjjjj" oder "ddjjjj" oder "dd"
     */
    public Datum(final String name, final int length, final int start, final String inhalt) {
        this(name, length, start);
        this.setInhalt(inhalt);
    }

    /**
     * Instantiates a new datum.
     */
    public Datum() {
        this(1);
    }

    /**
     * Instantiates a new datum.
     *
     * @param start the start
     */
    public Datum(final int start) {
        this(8, start);
    }

    /**
     * Instantiates a new datum.
     *
     * @param length the length
     * @param start the start
     */
    public Datum(final int length, final int start) {
        super(length, start, Align.RIGHT);
        dateFormat = getDateFormat(length);
    }

    private static DateFormat getDateFormat(final int length) {
        switch (length) {
            case 2:
                return new SimpleDateFormat("dd");
            case 4:
                return new SimpleDateFormat("MMyy");
            case 6:
                return new SimpleDateFormat("MMyyyy");
            case 8:
                return new SimpleDateFormat("ddMMyyyy");
            default:
                throw new IllegalArgumentException("length=" + length
                        + " not allowed - only 2, 4, 6 or 8");
        }
    }

    /**
     * Sets the inhalt.
     *
     * @param d the new inhalt
     */
    public void setInhalt(final Datum d) {
        this.setInhalt(d.getInhalt());
    }

    /**
     * Sets the inhalt.
     *
     * @param d the new inhalt
     */
    public void setInhalt(final Date d) {
        this.setInhalt(dateFormat.format(d));
    }

    /**
     * To date.
     *
     * @return the date
     */
    public Date toDate() {
        try {
            return dateFormat.parse(this.getInhalt());
        } catch (ParseException e) {
            throw new IllegalStateException(this + " has an invalid date (\""
                    + this.getInhalt() + "\")");
        }
    }

    /**
     * Heute.
     *
     * @return the datum
     */
    public static Datum heute() {
        Datum d = new Datum();
        d.setInhalt(new Date());
        return d;
    }

    /* (non-Javadoc)
     * @see gdv.xport.feld.Feld#isEmpty()
     */
    @Override
    public boolean isEmpty() {
        if (super.isEmpty()) {
            return true;
        }
        try {
            int n = Integer.parseInt(this.getInhalt());
            return n == 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Aus Performance-Gruenden verwenden wir hier nicht die
     * validate()-Methode.
     *
     * @return true/false
     *
     * @see gdv.xport.feld.Feld#isValid()
     */
    @Override
    public boolean isValid() {
        if (this.isEmpty()) {
            return true;
        }
        if (!super.isValid()) {
            return false;
        }
        return this.hasValidDate();
    }

    /* (non-Javadoc)
     * @see gdv.xport.feld.Feld#isInvalid()
     */
    @Override
    public boolean isInvalid() {
        return !this.isValid();
    }

    private boolean hasValidDate() {
        String orig = this.getInhalt();
        if (orig.startsWith("00")) {
            return true;
        }
        try {
            Date date = this.toDate();
            String conv = this.dateFormat.format(date);
            return conv.equals(orig);
        } catch (RuntimeException e) {
            log.info(e + " -> mapped to false");
            return false;
        }
    }

    /* (non-Javadoc)
     * @see gdv.xport.feld.Feld#validate()
     */
    @Override
    public List<ConstraintViolation> validate() {
        List<ConstraintViolation> violations = super.validate();
        if (this.isEmpty()) {
            return violations;
        }
        try {
            if (!this.hasValidDate()) {
                throw new RuntimeException(this.getInhalt() + " is not a valid date");
            }
        } catch (RuntimeException e) {
            ConstraintViolation cv = new ConstraintViolation(new MatchPatternCheck(), e
                    .getLocalizedMessage(), this, this.getInhalt(), new ClassContext(this
                    .getClass()));
            violations.add(cv);
        }
        return violations;
    }

}

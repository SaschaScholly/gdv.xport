/*
 * Copyright (c) 2017 by Oliver Boehm
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
 * (c)reated 16.02.17 by oliver (ob@oasd.de)
 */
package gdv.xport.service.web;

import gdv.xport.Datenpaket;
import gdv.xport.config.Config;
import net.sf.oval.ConstraintViolation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Integrationstests fuer den {@link DatenpaketController}.
 *
 * @author <a href="ob@aosd.de">oliver</a>
 */
public final class DatenpaketControllerIT extends AbstractControllerIT {

    private static final Logger LOG = LogManager.getLogger(DatenpaketControllerIT.class);

    /**
     * Hier testen wir, ob wir mit dem Musterdatensatz eine leere Liste von
     * Violations zurueckbekommen.
     */
    @Test
    public void testValidateURI() {
        ResponseEntity<String> response = getResponseEntityFor(
                "/Datenpakete/validate?uri=http://www.gdv-online.de/vuvm/musterdatei_bestand/musterdatei_041222.txt", String.class);
        assertThat(response.getBody(), equalTo("[]"));
    }

    /**
     * Hier testen wir ein leeres Dummy-Datenpaket, bei dem die VU-Nummer nicht
     * gesetzt ist. Dies sollte zu einem Validierungs-Fehler fuehren.
     *
     * @throws IOException the io exception
     */
    @Test
    public void testValidatePost() throws IOException {
        String text = createDummyDatenpaketText();
        String response = postResponseObjectFor("/Datenpakete/validate", text, String.class);
        LOG.info("Response of validation is '{}'.", response);
        assertThat(response, containsString("VU-Nummer is not set"));
    }

    private static String createDummyDatenpaketText() throws IOException {
        Datenpaket dummy = new Datenpaket(Config.DUMMY_VU_NUMMER);
        List<ConstraintViolation> violations = dummy.validate();
        StringWriter writer = new StringWriter();
        dummy.export(writer);
        writer.flush();
        writer.close();
        return writer.toString();
    }

}

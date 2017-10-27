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
 * (c)reated 15.02.17 by oliver (ob@oasd.de)
 */
package gdv.xport.srv.web;

import gdv.xport.Datenpaket;
import gdv.xport.srv.service.DatenpaketService;
import gdv.xport.srv.service.DefaultDatenpaketService;
import gdv.xport.util.URLReader;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.ui.Model;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import patterntesting.runtime.log.LogWatch;
import patterntesting.runtime.util.Converter;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static gdv.xport.srv.config.AppConfig.TEXT_CSV;

/**
 * Dieser Controller repraesentiert das REST-Interface zur Datenpaket-Klasse.
 *
 * @author <a href="ob@aosd.de">oliver</a>
 */
@RestController
@RequestMapping("/api/v1")
public final class DatenpaketController {

    private static final Logger LOG = LogManager.getLogger(DatenpaketController.class);

    @Autowired
    private DatenpaketService service;

    @Autowired
    private HttpServletRequest request;

    /**
     * Validiert die uebergebene URI.
     *
     * @param uri     z.B. http://www.gdv-online.de/vuvm/musterdatei_bestand/musterdatei_041222.txt
     * @param request the request
     * @return the response entity
     */
    @GetMapping("/validate")
    @ApiOperation(value = "validiert die uebergebene URI")
    @ApiImplicitParams({
            @ApiImplicitParam(
                    name = "uri",
                    value = "z.B. http://www.gdv-online.de/vuvm/musterdatei_bestand/musterdatei_041222.txt",
                    required = true,
                    dataType = "string",
                    paramType = "query"
            )
    })
    public ResponseEntity<List<Model>> validate(@RequestParam("uri") URI uri, HttpServletRequest request) {
        try {
            String content = readFrom(uri);
            return validate(content, request);
        } catch (IOException ioe) {
            LOG.warn("Cannot validate '{}':", uri, ioe);
            return ResponseEntity.ok(DefaultDatenpaketService.asModelList(ioe));
        }
    }

    /**
     * Validiert die eingelesenen Datenpakete.
     *
     * @param body    Text, der ueber die Leitung reinkommt.
     * @param text    alternativ kann der Text auch als Parameter reinkommen
     * @param request the request
     * @return the response entity
     */
    @PostMapping("/validate")
    public ResponseEntity<List<Model>> validate(@RequestBody(required = false) String body,
                                                @RequestParam(required = false) String text,
                                                HttpServletRequest request) {
        String content = (StringUtils.isBlank(text)) ? body : text;
        return validate(content, request);
    }

    private ResponseEntity<List<Model>> validate(String content, HttpServletRequest request) {
        LogWatch watch = new LogWatch();
        LOG.info("Validating Datenpakete of {}...", Converter.getMemoryAsString(StringUtils.length(content)));
        LOG.debug(content);
        List<Model> violations = service.validate(content);
        LOG.info("Validating Datenpakete finished with {} violation(s) in {}.", violations.size(), watch);
        return ResponseEntity.ok(violations);
    }

    /**
     * Laedt die gewuenschte Datei und validiert die darin enthaltenen
     * Datenpakete. Da hierueber der Inhalt der Datei mit uebertragen wird,
     * wird dieser Service ueber POST angesprochen.
     *
     * @param file    gewuenschte Datei
     * @param request the request
     * @return the response entity
     */
    @PostMapping("/validateUploaded")
    public ResponseEntity<List<Model>> validate(@RequestParam("file") MultipartFile file, HttpServletRequest request) {
        try {
            String text = readFrom(file);
            return validate(text, request);
        } catch (IOException ioe) {
            LOG.warn("Cannot upload and validate {}:", file.getOriginalFilename(), ioe);
            return ResponseEntity.badRequest().body(DefaultDatenpaketService.asModelList(ioe));
        }
    }

    /**
     * Formattiert das Datenpaket, das von der uebergebenen URI abgeholt wird,
     * in das gewuenscht Format wie HTML, XML, JSON oder CSV.
     *
     * @param uri     z.B. http://www.gdv-online.de/vuvm/musterdatei_bestand/musterdatei_041222.txt
     * @param request der urspruengliche Request (zur Format-Bestimmung)
     * @return erzeugtes Format als Text
     * @throws IOException the io exception
     */
    @GetMapping("/format")
    @ApiOperation(value = "formattiert die uebergebene URI")
    @ApiImplicitParams({
            @ApiImplicitParam(
                    name = "uri",
                    value = "z.B. http://www.gdv-online.de/vuvm/musterdatei_bestand/musterdatei_041222.txt",
                    required = true,
                    dataType = "string",
                    paramType = "query"
            )
    })
    public @ResponseBody ResponseEntity<String> format(@RequestParam("uri") URI uri,
                                                       @RequestParam(required = false) String type,
                                                       HttpServletRequest request) throws IOException {
        String content = readFrom(uri);
        return format(content, type, request);
    }

    private static String readFrom(@RequestParam("uri") URI uri) throws IOException {
        LogWatch watch = new LogWatch();
        LOG.info("Reading Datenpakete from {}...", uri);
        URLReader urlReader = new URLReader(uri.toURL());
        String content = urlReader.read();
        LOG.info("Reading Datenpakete from {} finished after {} with {} bytes.", uri, watch, content.length());
        return content;
    }

    /**
     * Formattiert das Datenpaket, das als Text reinkommt, in das gewuenschte
     * Format wie HTML, XML, JSON oder CSV.
     *
     * @param body    the body
     * @return erzeugtes Format als Text
     * @param request der urspruengliche Request (zur Format-Bestimmung)
     */
    @PostMapping(
            value = "/format",
            produces = { MediaType.TEXT_HTML_VALUE, MediaType.TEXT_XML_VALUE, MediaType.APPLICATION_JSON_VALUE, TEXT_CSV }
    )
    public @ResponseBody ResponseEntity<String> format(@RequestBody(required = false) String body,
                                       @RequestParam(required = false) String text,
                                       @RequestParam(required = false) String type,
                                       HttpServletRequest request) {
        String content = (StringUtils.isBlank(text)) ? body : text;
        return format(content, type, request);
    }

    /**
     * Die Umwandlung eines Datenpakets in das gewuenschte Datenformat wird
     * anhand des Accept-Headers (Content Negotiation) oder anhand des Suffixes
     * durchgefuehrt.
     *
     * @param body Datenpaket im GDV-Format
     * @param text alternativ kann das Datenpaket aus als Parameter reinkommen
     * @return Datenpaket
     * @throws IOException bei Netzproblemen
     */
    @PostMapping(
            value = "/Datenpaket", produces = {MediaType.TEXT_HTML_VALUE, MediaType.TEXT_XML_VALUE,
            MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE, MediaType.TEXT_PLAIN_VALUE, TEXT_CSV}
    )
    public @ResponseBody
    Datenpaket getDatenpaket(@RequestBody(required = false) String body, @RequestParam(required = false) String text)
            throws IOException {
        String content = (StringUtils.isBlank(text)) ? body : text;
        Datenpaket datenpaket = new Datenpaket();
        datenpaket.importFrom(content);
        return datenpaket;
    }

    /**
     * Laedt die gewuenschte Datei und formattiert die darin enthaltenen
     * Datenpakete. Da hierueber der Inhalt der Datei mit uebertragen wird,
     * wird dieser Service ueber POST angesprochen.
     *
     * @param file    gewuenschte Datei
     * @param request der urspruengliche Request (zur Format-Bestimmung)
     * @return erzeugtes Format als Text
     * @throws IOException the io exception
     */
    @PostMapping("/formatUploaded")
    public @ResponseBody ResponseEntity<String> format (
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String type,
            HttpServletRequest request) throws IOException {
        String text = readFrom(file);
        return format(text, type, request);
    }

    private String readFrom(@RequestParam("file") MultipartFile file) throws IOException {
        LogWatch watch = new LogWatch();
        LOG.info("Reading Datenpakete from {}...", file);
        String text = new String(file.getBytes());
        LOG.info("Reading Datenpakete from {} finished after {} with {} bytes.", file, watch, text.length());
        return text;
    }

    private ResponseEntity<String> format(String content, @RequestParam(required = false) String type, HttpServletRequest request) {
        LogWatch watch = new LogWatch();
        MimeType mimeType = toMimeType(type, request);
        LOG.info("Formatting Datenpakete of {} as {}...", Converter.getMemoryAsString(StringUtils.length(content)), mimeType);
        ResponseEntity<String> response = format(content, mimeType);
        LOG.info("Formatting Datenpakete as {} finished in {}.", mimeType, watch);
        return response;
    }

    private ResponseEntity<String> format(String content, MimeType mimeType) {
        LOG.debug(content);
        String response = service.format(content, mimeType);
        return createResponseEntity(response, (MediaType) mimeType);
    }

    private static ResponseEntity<String> createResponseEntity(String response, MediaType mimeType) {
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.setContentType(mimeType);
        return new ResponseEntity<>(response, responseHeaders, HttpStatus.CREATED);
    }

    private static MimeType toMimeType(String type, HttpServletRequest request) {
        if (StringUtils.isBlank(type)) {
            return toMimeTypes(request).get(0);
        } else {
            return toMimeType(type);
        }
    }

    private static List<MimeType> toMimeTypes(HttpServletRequest request) {
        Set<MimeType> mimeTypes = new LinkedHashSet<>();
        String format = StringUtils.substringAfterLast(request.getServletPath(), ".");
        if (StringUtils.isNotBlank(format)) {
            mimeTypes.add(toMimeType(format));
        }
        String[] accepted = request.getHeader("accept").split(",");
        for (String accept : accepted) {
            mimeTypes.add(toMimeType(accept));
        }
        mimeTypes.add(MimeTypeUtils.TEXT_PLAIN);
        return new ArrayList<>(mimeTypes);
    }

    private static MimeType toMimeType(String format) {
        switch (format.toLowerCase()) {
            case "html":
                return MediaType.TEXT_HTML;
            case "xml":
                return MediaType.TEXT_XML;
            case "csv":
                return MediaType.valueOf(TEXT_CSV);
            case "json":
                return MediaType.APPLICATION_JSON;
            case "plain":
                return MediaType.TEXT_PLAIN;
            default:
                try {
                    return MediaType.valueOf(format);
                } catch (InvalidMediaTypeException ex) {
                    LOG.info("Will use '{}' as MimeType for unknown format parameter '{}'.", MediaType.TEXT_PLAIN,
                            format);
                    LOG.debug("Details:", ex);
                    return MediaType.TEXT_PLAIN;
                }
        }
    }

    /**
     * Falsche Parameter oder falscher Input wurde angegeben - bad request.
     * Wir liefern hier die Fehlermeldung nicht als Entity, sondern als String
     * zurueck, damit wir auf den angeforderten MediaType reagieren koennen.
     * Spring kann hier nur den verwendeten ErrorDetail nur als JSON
     * zurueckgeben. Bei anderen MimeTypes kommt dann
     * <pre>
     * org.springframework.web.HttpMediaTypeNotAcceptableException: Could not find acceptable representation
     * </pre>
     *
     * @param ex Ursache
     * @return Antwort
     */
    @ResponseStatus(value = HttpStatus.BAD_REQUEST)
    @ExceptionHandler(IllegalArgumentException.class)
    public String handleException(IllegalArgumentException ex) {
        ErrorDetail errDetail = new ErrorDetail(request, HttpStatus.BAD_REQUEST, ex);
        LOG.info("Call of '{}' fails: {}", request.getRequestURI(), errDetail);
        return errDetail.toString(toMimeTypes(request));
    }

    /**
     * Normalerweise wird dieses Attribut von Spring injected. Aber z
     * Testzwecken koennen wir es hierueber explizit setzen.
     *
     * @param request einkommender HTTP-Request
     */
    protected void setRequest(HttpServletRequest request) {
        this.request = request;
    }

}

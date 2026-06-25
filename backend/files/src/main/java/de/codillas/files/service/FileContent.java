package de.codillas.files.service;

import org.springframework.core.io.Resource;

/** A downloaded file's content plus the headers needed to serve it. */
public record FileContent(Resource resource, String filename, String contentType) {}

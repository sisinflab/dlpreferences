package it.poliba.sisinflab.dlpreferences.except;

import java.nio.file.Path;

/**
 * Thrown when the XML input file to a CP-net cannot be parsed successfully.
 */
public class SpecFileParseException extends Exception {
    public SpecFileParseException(Path file, Throwable cause) {
        this(file.toString(), cause);
    }

    public SpecFileParseException(String filename, Throwable cause) {
        super(String.format("error while parsing '%s'", filename), cause);
    }

    public SpecFileParseException(Path file, String message) {
        this(file.toString(), message);
    }

    public SpecFileParseException(String filename, String message) {
        super(String.format("error while parsing '%s': %s", filename, message));
    }
}

package it.poliba.enasca.ontocpnets.except;

/**
 * Thrown when a {@link it.poliba.enasca.ontocpnets.NuSMVRunner} instance
 * tries to verify a malformed {@link it.unibg.nuseen.nusmvlanguage.nuSMV.NuSmvModel}.
 */
public class MalformedNuSMVModelException extends RuntimeException {
    public MalformedNuSMVModelException(Throwable cause) {
        super("Malformed NuSMV model", cause);
    }
}

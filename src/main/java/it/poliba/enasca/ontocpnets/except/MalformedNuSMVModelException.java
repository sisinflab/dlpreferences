package it.poliba.enasca.ontocpnets.except;

import it.poliba.enasca.ontocpnets.nusmv.NuSMVRunner;

/**
 * Thrown when a {@link NuSMVRunner} instance
 * tries to verify a malformed {@link it.unibg.nuseen.nusmvlanguage.nuSMV.NuSmvModel}.
 */
public class MalformedNuSMVModelException extends RuntimeException {
    public MalformedNuSMVModelException(Throwable cause) {
        super("Malformed NuSMV model", cause);
    }
}

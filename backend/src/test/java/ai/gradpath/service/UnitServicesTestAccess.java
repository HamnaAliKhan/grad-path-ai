package ai.gradpath.service;

import java.io.IOException;

/** Gives integration tests access to the sample resume PDF built in {@link UnitServicesTest}. */
public final class UnitServicesTestAccess {

    private UnitServicesTestAccess() { }

    public static byte[] resumePdf() throws IOException {
        return UnitServicesTest.pdf(UnitServicesTest.RESUME);
    }
}

package com.ndzela.admin.driver.domain;

import java.util.function.BiConsumer;
import java.util.function.Function;

/** The files kept for a driver; the URL slug names the kind. */
public enum DriverFileKind {
    PHOTO("photo", true, Driver::getPhotoFile, Driver::setPhotoFile),
    LICENCE("licence", false, Driver::getLicenceFile, Driver::setLicenceFile),
    ID("id", false, Driver::getIdFile, Driver::setIdFile),
    ADDRESS_PROOF("address-proof", false, Driver::getAddressProofFile, Driver::setAddressProofFile);

    private final String slug;
    private final boolean imageOnly;
    private final Function<Driver, String> getter;
    private final BiConsumer<Driver, String> setter;

    DriverFileKind(String slug, boolean imageOnly, Function<Driver, String> getter, BiConsumer<Driver, String> setter) {
        this.slug = slug;
        this.imageOnly = imageOnly;
        this.getter = getter;
        this.setter = setter;
    }

    public boolean imageOnly() { return imageOnly; }
    public String fileOf(Driver driver) { return getter.apply(driver); }
    public void set(Driver driver, String file) { setter.accept(driver, file); }

    public static DriverFileKind fromSlug(String slug) {
        for (DriverFileKind kind : values()) {
            if (kind.slug.equals(slug)) {
                return kind;
            }
        }
        return null;
    }
}

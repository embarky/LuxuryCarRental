package ch.unil.softarch.luxurycarrental.ui;

import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.convert.Converter;
import jakarta.faces.convert.ConverterException;
import jakarta.faces.convert.FacesConverter;
import jakarta.faces.application.FacesMessage;

import java.util.UUID;

/** Converts UUIDs in forms (e.g. the car type drop-down) automatically for every UUID property. */
@FacesConverter(forClass = UUID.class)
public class UuidConverter implements Converter<UUID> {

    @Override
    public UUID getAsObject(FacesContext context, UIComponent component, String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(value.trim());
        } catch (IllegalArgumentException e) {
            throw new ConverterException(new FacesMessage(FacesMessage.SEVERITY_ERROR, "Invalid identifier.", null));
        }
    }

    @Override
    public String getAsString(FacesContext context, UIComponent component, UUID value) {
        return value == null ? "" : value.toString();
    }
}

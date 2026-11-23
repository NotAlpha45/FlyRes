package com.flyres.domain.flight;

import java.time.LocalDate;

/**
 * What a user searches for. Airport and airline values are IATA codes. {@code date} is optional
 * (null means every upcoming flight on the route) and so is {@code airlineCode} (null or blank means
 * every airline). The control validates the values; this record only carries them.
 */
public record SearchCriteria(String originCode, String destinationCode, LocalDate date, String airlineCode) {
}

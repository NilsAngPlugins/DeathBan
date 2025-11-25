package dev.t0g3pii.deathban.util;

import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DurationParser {
	private static final Pattern TOKEN = Pattern.compile("(\\d+)([smhdw])", Pattern.CASE_INSENSITIVE);

	private DurationParser() {}

	public static Duration parseOrThrow(String input) {
		if (input == null || input.isBlank()) {
			throw new IllegalArgumentException("duration empty");
		}
		input = input.trim();
		if (input.matches("\\d+")) {
			long minutes = Long.parseLong(input);
			return Duration.ofMinutes(minutes);
		}
		Matcher m = TOKEN.matcher(input);
		long seconds = 0L;
		int found = 0;
		while (m.find()) {
			long value = Long.parseLong(m.group(1));
			char unit = Character.toLowerCase(m.group(2).charAt(0));
			switch (unit) {
				case 's': seconds += value; break;
				case 'm': seconds += value * 60L; break;
				case 'h': seconds += value * 3600L; break;
				case 'd': seconds += value * 86400L; break;
				case 'w': seconds += value * 604800L; break;
				default: throw new IllegalArgumentException("unknown unit: " + unit);
			}
			found++;
		}
		if (found == 0) {
			throw new IllegalArgumentException("invalid duration: " + input);
		}
		return Duration.ofSeconds(seconds);
	}
}

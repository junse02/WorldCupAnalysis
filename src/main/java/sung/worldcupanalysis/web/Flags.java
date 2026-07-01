package sung.worldcupanalysis.web;

import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Maps a team name to its national flag, so templates can show a flag image
 * next to the team even when the (API-Football) crest is unavailable — e.g. the
 * bundled sample data has no crests. Exposed to Thymeleaf as {@code @flags}.
 *
 * <p>Flags are served by <a href="https://flagcdn.com">flagcdn.com</a> keyed by
 * ISO 3166-1 alpha-2 codes (plus GB subdivisions for England/Scotland). Unknown
 * strings (bracket placeholders like "A조 2위" or "M74 승자") return an empty
 * URL, so templates render only real teams.
 */
@Component("flags")
public class Flags {

    private static final Map<String, String> CODES = Map.ofEntries(
            Map.entry("Mexico", "mx"),
            Map.entry("South Africa", "za"),
            Map.entry("South Korea", "kr"),
            Map.entry("Czechia", "cz"),
            Map.entry("Canada", "ca"),
            Map.entry("Bosnia and Herzegovina", "ba"),
            Map.entry("Qatar", "qa"),
            Map.entry("Switzerland", "ch"),
            Map.entry("Brazil", "br"),
            Map.entry("Morocco", "ma"),
            Map.entry("Haiti", "ht"),
            Map.entry("Scotland", "gb-sct"),
            Map.entry("United States", "us"),
            Map.entry("Paraguay", "py"),
            Map.entry("Australia", "au"),
            Map.entry("Türkiye", "tr"),
            Map.entry("Germany", "de"),
            Map.entry("Curaçao", "cw"),
            Map.entry("Ivory Coast", "ci"),
            Map.entry("Ecuador", "ec"),
            Map.entry("Netherlands", "nl"),
            Map.entry("Japan", "jp"),
            Map.entry("Sweden", "se"),
            Map.entry("Tunisia", "tn"),
            Map.entry("Belgium", "be"),
            Map.entry("Iran", "ir"),
            Map.entry("Egypt", "eg"),
            Map.entry("New Zealand", "nz"),
            Map.entry("Spain", "es"),
            Map.entry("Cape Verde", "cv"),
            Map.entry("Uruguay", "uy"),
            Map.entry("Saudi Arabia", "sa"),
            Map.entry("France", "fr"),
            Map.entry("Iraq", "iq"),
            Map.entry("Norway", "no"),
            Map.entry("Senegal", "sn"),
            Map.entry("Argentina", "ar"),
            Map.entry("Austria", "at"),
            Map.entry("Jordan", "jo"),
            Map.entry("Algeria", "dz"),
            Map.entry("Portugal", "pt"),
            Map.entry("Colombia", "co"),
            Map.entry("Uzbekistan", "uz"),
            Map.entry("DR Congo", "cd"),
            Map.entry("England", "gb-eng"),
            Map.entry("Ghana", "gh"),
            Map.entry("Panama", "pa"),
            Map.entry("Croatia", "hr"));

    /** ISO code for a team name, or {@code null} if not a known nation. */
    public String code(String teamName) {
        return teamName == null ? null : CODES.get(teamName.trim());
    }

    /** flagcdn SVG URL for a team, or {@code ""} if the name isn't a nation. */
    public String url(String teamName) {
        String code = code(teamName);
        return code == null ? "" : "https://flagcdn.com/" + code + ".svg";
    }
}

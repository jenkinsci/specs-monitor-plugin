package io.jenkins.plugins.specsmonitor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Reads which users are logged in to a node, from {@code query user} or {@code who}. */
public final class ConnectedUsers {

    private static final Pattern TOKEN = Pattern.compile("\\S+");

    private ConnectedUsers() {}

    /** A user with a session on the node. */
    public static final class Session implements Serializable {
        private static final long serialVersionUID = 1L;

        private final String user;
        private final String detail;

        Session(String user, String detail) {
            this.user = user;
            this.detail = detail;
        }

        public String getUser() {
            return user;
        }

        /** Session type or terminal and state, such as {@code rdp-tcp#0, Active}. */
        public String getDetail() {
            return detail;
        }
    }

    /** Lists the sessions on the machine this runs on. */
    static List<Session> collect(RemoteTarget.Family family) {
        switch (family) {
            case WINDOWS:
                return fromQueryUser(CommandRunner.tryRun("query", "user"));
            case LINUX:
            case MAC:
                return fromWho(CommandRunner.tryRun("who"));
            default:
                return new ArrayList<>();
        }
    }

    /**
     * Parses {@code query user}. Columns are located from the header, whose names depend on the
     * display language of Windows, so only the first four columns are used: user, session, ID, state.
     */
    static List<Session> fromQueryUser(String output) {
        List<Session> sessions = new ArrayList<>();
        if (output == null) {
            return sessions;
        }
        String[] lines = output.split("\\R");
        if (lines.length < 2) {
            return sessions;
        }
        List<Integer> starts = new ArrayList<>();
        Matcher m = TOKEN.matcher(lines[0]);
        while (m.find() && starts.size() < 5) {
            starts.add(m.start());
        }
        if (starts.size() < 5) {
            return sessions;
        }
        int sessionAt = starts.get(1);
        int idAt = starts.get(2);
        int stateAt = starts.get(3);
        int stateEnd = starts.get(4);
        for (int i = 1; i < lines.length; i++) {
            String line = lines[i];
            if (line.length() <= idAt || line.isBlank()) {
                continue;
            }
            // The current session is marked with ">" in front of the user name.
            String user = line.substring(0, sessionAt).replace('>', ' ').trim();
            String session = line.substring(sessionAt, idAt).trim();
            String state =
                    line.substring(stateAt, Math.min(stateEnd, line.length())).trim();
            if (user.isEmpty()) {
                continue;
            }
            String detail = session.isEmpty() ? state : session + ", " + state;
            sessions.add(new Session(user, detail));
        }
        return sessions;
    }

    /** Parses {@code who}, e.g. {@code jdoe pts/0 2026-10-27 09:00 (10.0.0.5)}. */
    static List<Session> fromWho(String output) {
        // A user logged in twice is listed once, with all terminals.
        Map<String, List<String>> terminals = new LinkedHashMap<>();
        if (output != null) {
            for (String line : output.split("\\R")) {
                String[] parts = line.trim().split("\\s+", 3);
                if (parts.length < 2 || parts[0].isEmpty()) {
                    continue;
                }
                String detail = parts.length > 2 ? parts[1] + ", since " + parts[2] : parts[1];
                terminals.computeIfAbsent(parts[0], k -> new ArrayList<>()).add(detail);
            }
        }
        List<Session> sessions = new ArrayList<>();
        terminals.forEach((user, details) -> sessions.add(new Session(user, String.join("; ", details))));
        return sessions;
    }
}

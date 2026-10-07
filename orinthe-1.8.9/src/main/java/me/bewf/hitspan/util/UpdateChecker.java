package me.bewf.hitspan.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class UpdateChecker {

    private static final ExecutorService EXEC = Executors.newSingleThreadExecutor();
    private static boolean ran = false;

    private UpdateChecker() {}

    public static void checkOnce(String projectId,
                                 String projectSlug,
                                 String displayName,
                                 String currentVersion,
                                 String mcVersion,
                                 String loader) {
        if (ran) return;
        ran = true;

        EXEC.submit(() -> {
            try {
                String latest = fetchBestLatestVersion(projectId, mcVersion, loader);
                if (latest == null) {
                    System.out.println("[" + displayName + "] Update check: no matching versions for " + mcVersion + " (" + loader + ")");
                    return;
                }

                if (!isNewer(latest, currentVersion)) {
                    System.out.println("[" + displayName + "] Update check: up to date (" + currentVersion + ")");
                    return;
                }

                String versionsUrl = "https://modrinth.com/mod/" + projectSlug + "/versions";

                Minecraft.getInstance().executeTask(java.util.concurrent.Executors.callable(() -> {
                    if (Minecraft.getInstance().player == null) return;
                    Chat.send("\u00A7b[" + displayName + "] \u00A7eA new update is available: \u00A76" + latest +
                            "\u00A7e (current \u00A76" + currentVersion + "\u00A7e)");
                    Chat.send("\u00A7d\u00A7lDownload: \u00A7r\u00A79" + versionsUrl);
                    NotificationManager.showUpdateNotificationWithConfigTip();
                }));

                System.out.println("[" + displayName + "] Update check: " + latest + " available (current " + currentVersion + ")");
            } catch (Throwable t) {
                System.err.println("[" + displayName + "] Update check failed: " + t);
            }
        });
    }

    private static String fetchBestLatestVersion(String projectId, String mcVersion, String loader) throws Exception {
        String gv = "[\"" + mcVersion + "\"]";
        String ld = "[\"" + loader + "\"]";

        String apiUrl =
                "https://api.modrinth.com/v2/project/" + projectId + "/version" +
                        "?limit=50" +
                        "&game_versions=" + URLEncoder.encode(gv, "UTF-8") +
                        "&loaders=" + URLEncoder.encode(ld, "UTF-8");

        HttpURLConnection con = (HttpURLConnection) new URL(apiUrl).openConnection();
        con.setRequestMethod("GET");
        con.setConnectTimeout(6000);
        con.setReadTimeout(6000);
        con.setRequestProperty("User-Agent", "HitSpanUpdateChecker");

        int code = con.getResponseCode();
        if (code < 200 || code >= 300) {
            System.out.println("[HitSpan] Update check HTTP " + code);
            return null;
        }

        try (BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);

            JsonElement parsed = new JsonParser().parse(sb.toString());
            if (!parsed.isJsonArray()) return null;

            JsonArray arr = parsed.getAsJsonArray();
            if (arr.size() == 0) return null;

            String best = null;
            int[] bestV = null;

            for (JsonElement el : arr) {
                if (!el.isJsonObject()) continue;
                JsonObject obj = el.getAsJsonObject();

                JsonElement vEl = obj.get("version_number");
                if (vEl == null) continue;

                String ver = vEl.getAsString();
                int[] pv = parseVersion(ver);

                if (best == null) {
                    best = ver;
                    bestV = pv;
                    continue;
                }

                if (compareVersion(pv, bestV) > 0) {
                    best = ver;
                    bestV = pv;
                }
            }

            return best;
        }
    }

    private static boolean isNewer(String latest, String current) {
        int[] a = parseVersion(latest);
        int[] b = parseVersion(current);
        return compareVersion(a, b) > 0;
    }

    private static int compareVersion(int[] a, int[] b) {
        for (int i = 0; i < 3; i++) {
            if (a[i] != b[i]) return Integer.compare(a[i], b[i]);
        }
        return 0;
    }

    private static int[] parseVersion(String v) {
        int[] out = new int[]{0, 0, 0};
        if (v == null) return out;

        String clean = v.trim();
        int dash = clean.indexOf('-');
        if (dash >= 0) clean = clean.substring(0, dash);

        String[] parts = clean.split("\\.");
        for (int i = 0; i < out.length && i < parts.length; i++) {
            try {
                String num = parts[i].replaceAll("[^0-9]", "");
                out[i] = num.isEmpty() ? 0 : Integer.parseInt(num);
            } catch (Throwable ignored) {
                out[i] = 0;
            }
        }
        return out;
    }
}

package dev.nilsang.deathban.discord;

import org.bukkit.configuration.file.FileConfiguration;

import javax.net.ssl.HttpsURLConnection;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DiscordNotifier {
	private volatile FileConfiguration discordCfg;

	public DiscordNotifier(FileConfiguration discordCfg) {
		this.discordCfg = discordCfg;
	}

	public void setConfig(FileConfiguration discordCfg) { this.discordCfg = discordCfg; }

	public boolean isEnabled() { return discordCfg.getBoolean("enabled", false); }

	public void send(String content, String username, String avatarUrl, Map<String, Object> embed) {
		String url = discordCfg.getString("webhookUrl", "");
		if (url == null || url.isBlank()) return;
		try {
			URL u = new URL(url);
			HttpsURLConnection conn = (HttpsURLConnection) u.openConnection();
			conn.setRequestMethod("POST");
			conn.setDoOutput(true);
			conn.setRequestProperty("Content-Type", "application/json");

			Map<String, Object> json = new HashMap<>();
			if (username != null && !username.isBlank()) json.put("username", username);
			if (avatarUrl != null && !avatarUrl.isBlank()) json.put("avatar_url", avatarUrl);
			if (content != null && !content.isBlank()) json.put("content", content);
			if (embed != null) json.put("embeds", new Object[]{ embed });

			String payload = Json.simple(json);
			try (OutputStream os = conn.getOutputStream()) {
				os.write(payload.getBytes(StandardCharsets.UTF_8));
			}
			conn.getInputStream().close();
			conn.disconnect();
		} catch (Exception ignored) {
		}
	}

	public Map<String, Object> buildEmbed(String title, String description, int color, String footer, String thumbnail, List<Map<String, Object>> fields) {
		Map<String, Object> embed = new HashMap<>();
		embed.put("title", title);
		embed.put("description", description);
		embed.put("color", color);
		if (footer != null && !footer.isBlank()) {
			Map<String, Object> f = new HashMap<>();
			f.put("text", footer);
			embed.put("footer", f);
		}
		if (thumbnail != null && !thumbnail.isBlank()) {
			Map<String, Object> t = new HashMap<>();
			t.put("url", thumbnail);
			embed.put("thumbnail", t);
		}
		if (fields != null && !fields.isEmpty()) {
			embed.put("fields", fields.toArray());
		}
		return embed;
	}

	// Minimal JSON builder ohne externe Abhängigkeit
	private static class Json {
		static String simple(Object value) {
			if (value == null) return "null";
			if (value instanceof String) return '"' + escape((String) value) + '"';
			if (value instanceof Number || value instanceof Boolean) return value.toString();
			if (value.getClass().isArray()) {
				Object[] arr = (Object[]) value;
				StringBuilder sb = new StringBuilder();
				sb.append('[');
				for (int i = 0; i < arr.length; i++) {
					if (i > 0) sb.append(',');
					sb.append(simple(arr[i]));
				}
				sb.append(']');
				return sb.toString();
			}
			if (value instanceof Map) {
				Map<?, ?> map = (Map<?, ?>) value;
				StringBuilder sb = new StringBuilder();
				sb.append('{');
				boolean first = true;
				for (Map.Entry<?, ?> e : map.entrySet()) {
					if (!first) sb.append(',');
					first = false;
					sb.append('"').append(escape(String.valueOf(e.getKey()))).append('"').append(':');
					sb.append(simple(e.getValue()));
				}
				sb.append('}');
				return sb.toString();
			}
			return '"' + escape(String.valueOf(value)) + '"';
		}

		static String escape(String s) {
			return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
		}
	}
}

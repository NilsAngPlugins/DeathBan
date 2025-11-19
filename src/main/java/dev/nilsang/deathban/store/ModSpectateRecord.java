package dev.nilsang.deathban.store;

public class ModSpectateRecord {
	public final long untilEpochSeconds;
	public final String playerName;
	public final String worldKey;
	public final int x;
	public final int y;
	public final int z;
	public final long deathEpochSeconds;

	public ModSpectateRecord(long untilEpochSeconds, String playerName, String worldKey, int x, int y, int z, long deathEpochSeconds) {
		this.untilEpochSeconds = untilEpochSeconds;
		this.playerName = playerName;
		this.worldKey = worldKey;
		this.x = x;
		this.y = y;
		this.z = z;
		this.deathEpochSeconds = deathEpochSeconds;
	}
}

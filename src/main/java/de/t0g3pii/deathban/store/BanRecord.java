package de.t0g3pii.deathban.store;

public class BanRecord {
	public final long untilEpochSeconds;
	public final String playerName;
	public final String worldKey; // normal, nether, the_end
	public final int x;
	public final int y;
	public final int z;
	public final long deathEpochSeconds;

	public BanRecord(long untilEpochSeconds, String playerName, String worldKey, int x, int y, int z, long deathEpochSeconds) {
		this.untilEpochSeconds = untilEpochSeconds;
		this.playerName = playerName;
		this.worldKey = worldKey;
		this.x = x;
		this.y = y;
		this.z = z;
		this.deathEpochSeconds = deathEpochSeconds;
	}
}

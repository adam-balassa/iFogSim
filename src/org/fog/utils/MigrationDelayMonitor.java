package org.fog.utils;

import fi.aalto.cs.extensions.Monitor;

public class MigrationDelayMonitor implements Monitor {
	
	private static double migrationDelay = 0.0;
	
	public static double getMigrationDelay() {
		return migrationDelay;
	}

	public static void setMigrationDelay(double migrationDelayReceived) {
		migrationDelay += migrationDelayReceived;
	}

	@Override
	public void clear() {
		migrationDelay = 0.0;
	}
}

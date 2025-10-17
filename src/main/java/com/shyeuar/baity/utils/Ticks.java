package com.shyeuar.baity.utils;

/*
 * This file is part of Baity, a Minecraft Fabric mod.
 * 
 * This file is based on Ticks utility from CaribouStonks by Siroz555,
 * which is licensed under LGPL-3.0.
 * 
 * Original source: https://github.com/Siroz555/CaribouStonks
 * 
 * Baity is licensed under AGPL-3.0.
 * 
 * Copyright (C) 2024 Baity Contributors
 * Copyright (C) 2024 Siroz555 (CaribouStonks)
 */

import java.util.concurrent.TimeUnit;

public final class Ticks {

	public static final int TICKS_PER_SECOND = 20;
	public static final int MILLISECONDS_PER_SECOND = 1000;
	public static final int MILLISECONDS_PER_TICK = MILLISECONDS_PER_SECOND / TICKS_PER_SECOND;

	private Ticks() {
	}

	/**
	 * Converts a duration in a certain unit of time to tick.
	 *
	 * @param duration the duration of time
	 * @param unit     the unit the duration is in
	 * @return the number of ticks which represent the duration
	 */
	public static int from(int duration, TimeUnit unit) {
		return (int) (unit.toMillis(duration) / MILLISECONDS_PER_TICK);
	}

	/**
	 * Converts ticks to a duration in a certain unit of time.
	 *
	 * @param ticks the number of ticks
	 * @param unit  the unit to return the duration in
	 * @return a duration value in the given unit, representing the number of ticks
	 */
	public static int to(int ticks, TimeUnit unit) {
		return (int) unit.convert((long) ticks * MILLISECONDS_PER_TICK, TimeUnit.MILLISECONDS);
	}
}


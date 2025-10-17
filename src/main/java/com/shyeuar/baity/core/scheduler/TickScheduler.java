package com.shyeuar.baity.core.scheduler;

/*
 * This file is part of Baity, a Minecraft Fabric mod.
 * 
 * This file is based on TickScheduler from CaribouStonks by Siroz555,
 * which is licensed under LGPL-3.0.
 * 
 * Original source: https://github.com/Siroz555/CaribouStonks
 * 
 * Baity is licensed under AGPL-3.0.
 * 
 * Copyright (C) 2024 Baity Contributors
 * Copyright (C) 2024 Siroz555 (CaribouStonks)
 */

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import it.unimi.dsi.fastutil.ints.AbstractInt2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import com.shyeuar.baity.utils.Ticks;

/**
 * The {@code TickScheduler} class is responsible for managing and scheduling tasks
 * to be executed in sync with a tick-based system.
 * Tasks can be scheduled to run once after a delay or repeatedly on a fixed interval.
 * <p>
 * This class uses a singleton pattern to ensure there is only one instance managing
 * tick-related tasks.
 * <p>
 * This implementation is based on CaribouStonks by Siroz555, licensed under LGPL-3.0.
 */
public final class TickScheduler {

	private static TickScheduler instance;

	private int currentTick = 0;
	private final AbstractInt2ObjectMap<List<Runnable>> tasks = new Int2ObjectOpenHashMap<>();
	private final AtomicInteger taskIdGenerator = new AtomicInteger(0);

	private TickScheduler() {
		ClientTickEvents.END_CLIENT_TICK.register(this::processTick);
	}

	/**
	 * Returns the singleton instance of the {@code TickScheduler} class.
	 *
	 * @return the singleton instance of {@code TickScheduler}
	 */
	public static TickScheduler getInstance() {
		return instance == null ? instance = new TickScheduler() : instance;
	}

	/**
	 * Attempts to cancel the task identified by the specified task ID. If a task with the given
	 * ID is found, it is marked as {@code canceled} and removed from the scheduler's task list.
	 *
	 * @param taskId the unique identifier of the task to be canceled
	 * @return {@code true} if the task was successfully canceled, {@code false} otherwise
	 */
	public boolean cancelTask(int taskId) {
		boolean found = false;
		for (List<Runnable> taskList : tasks.values()) {
			Iterator<Runnable> iterator = taskList.iterator();
			while (iterator.hasNext()) {
				Runnable task = iterator.next();
				if (task instanceof ScheduledTask scheduledTask && scheduledTask.id == taskId) {
					scheduledTask.setCancelled(true);
					iterator.remove();
					found = true;
				}
			}
		}

		return found;
	}

	/**
	 * Schedules a task to be executed after a specified delay in ticks.
	 * The task is added to the scheduler and will run once the delay has elapsed.
	 *
	 * @param task      the {@code Runnable} task to be executed; cannot be {@code null}
	 * @param delay     the delay in ticks before the task is executed; must be non-negative
	 * @param delayUnit the time unit of the delay parameter
	 */
	public void runLater(Runnable task, int delay, TimeUnit delayUnit) {
		runLater(task, Ticks.from(Math.max(0, delay), delayUnit));
	}

	/**
	 * Schedules a task to be executed after a specified delay in ticks.
	 * The task is added to the scheduler and will run once the delay has elapsed.
	 *
	 * @param task  the {@code Runnable} task to be executed; cannot be {@code null}
	 * @param delay the delay in ticks before the task is executed; must be non-negative
	 */
	public void runLater(Runnable task, int delay) {
		ScheduledTask scheduledTask = new ScheduledTask(task);
		scheduledTask.setId(taskIdGenerator.incrementAndGet());
		scheduleTask(scheduledTask, currentTick + Math.max(0, delay));
	}

	/**
	 * Schedules a task to be executed repeatedly at a fixed interval.
	 * Each task execution occurs on a repeating schedule, where the specified interval
	 * represents the number of ticks between consecutive executions of the task.
	 *
	 * @param task         the task to be executed repeatedly
	 * @param interval     the interval between successive task executions; must be greater than or equal to 1
	 * @param intervalUnit the time unit of the interval parameter
	 * @return the unique identifier of the scheduled task
	 */
	public int runRepeating(Runnable task, int interval, TimeUnit intervalUnit) {
		return runRepeating(task, Ticks.from(Math.max(1, interval), intervalUnit));
	}

	/**
	 * Schedules a task to be executed repeatedly at a fixed interval.
	 * Each task execution occurs on a repeating schedule, where the specified interval
	 * represents the number of ticks between consecutive executions.
	 *
	 * @param task     the task to be executed repeatedly
	 * @param interval the interval in ticks between successive task executions;
	 *                 must be greater than or equal to 1
	 * @return the unique identifier of the scheduled task
	 */
	public int runRepeating(Runnable task, int interval) {
		ScheduledTask scheduledTask = new ScheduledTask(task, Math.max(1, interval), TaskType.REPEATING);
		scheduledTask.setId(taskIdGenerator.incrementAndGet());
		return scheduleTask(scheduledTask, currentTick);
	}

	/**
	 * Schedules a task to be executed at a specific tick. If the provided tick already has
	 * tasks scheduled, the new task is added to the existing list of tasks for that tick.
	 * Otherwise, a new entry is created to hold the task at the specified tick.
	 *
	 * @param scheduledTask the task to be scheduled
	 * @param schedule      the tick at which the task should be executed
	 * @return the unique identifier of the scheduled task
	 */
	private int scheduleTask(ScheduledTask scheduledTask, int schedule) {
		if (tasks.containsKey(schedule)) {
			tasks.get(schedule).add(scheduledTask);
		} else {
			List<Runnable> list = new ArrayList<>();
			list.add(scheduledTask);
			tasks.put(schedule, list);
		}

		return scheduledTask.id;
	}

	/**
	 * Processes the current tick, executing any tasks scheduled for the current tick.
	 * Tasks that cannot be successfully executed are rescheduled for the next tick.
	 *
	 * @param client the Minecraft client instance used during the tick processing
	 */
	void processTick(MinecraftClient client) {
		if (tasks.containsKey(currentTick)) {
			List<Runnable> currentTickTasks = tasks.get(currentTick);
			//noinspection ForLoopReplaceableByForEach
			for (int i = 0; i < currentTickTasks.size(); i++) {
				Runnable task = currentTickTasks.get(i);
				if (!runTask(task)) {
					tasks.computeIfAbsent(currentTick + 1, key -> new ArrayList<>()).add(task);
				}
			}

			tasks.remove(currentTick);
		}

		currentTick += 1;
	}

	/**
	 * Executes the given {@code Runnable} task. If an exception or error occurs during execution,
	 * it is logged and the method returns {@code false}.
	 * Otherwise, it returns {@code true} to indicate successful execution.
	 *
	 * @param task the {@code Runnable} task to be executed
	 * @return {@code true} if the task was executed successfully, otherwise {@code false}
	 */
	private boolean runTask(Runnable task) {
		try {
			task.run();
		} catch (Throwable throwable) {
			if (task instanceof ScheduledTask scheduledTask) {
				System.err.println("[TickScheduler] Error while running task #" + scheduledTask.id + ": " + throwable.getMessage());
			}

			return false;
		}

		return true;
	}

	/**
	 * Represents the type of task in the scheduling system.
	 */
	private enum TaskType {
		SINGLE, REPEATING
	}

	/**
	 * Represents a scheduled task that can be executed either once or repeatedly
	 * based on the specified scheduling configuration.
	 */
	private static class ScheduledTask implements Runnable {

		private final Runnable task;
		private final int interval;
		private final TaskType taskType;
		private int id = -1;
		private boolean cancelled = false;

		ScheduledTask(Runnable task) {
			this(task, 0, TaskType.SINGLE);
		}

		ScheduledTask(Runnable task, int interval, TaskType taskType) {
			this.task = task;
			this.interval = interval;
			this.taskType = taskType;
		}

		public void setId(int id) {
			this.id = id;
		}

		public void setCancelled(boolean cancelled) {
			this.cancelled = cancelled;
		}

		@Override
		public void run() {
			if (!cancelled) {
				task.run();

				if (taskType == TaskType.REPEATING) {
					if (MinecraftClient.getInstance() != null && !RenderSystem.isOnRenderThread()) {
						MinecraftClient.getInstance().send(() -> instance.scheduleTask(this, instance.currentTick + interval));
					} else {
						instance.scheduleTask(this, instance.currentTick + interval);
					}
				}
			}
		}
	}
}

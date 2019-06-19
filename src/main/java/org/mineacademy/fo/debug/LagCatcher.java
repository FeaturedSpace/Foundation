package org.mineacademy.fo.debug;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.mineacademy.fo.Common;
import org.mineacademy.fo.MathUtil;
import org.mineacademy.fo.plugin.SimplePlugin;
import org.mineacademy.fo.settings.SimpleSettings;

/**
 * A simple yet effective way to calculate duration
 * between two points in code
 */
public final class LagCatcher {

	/**
	 * Stores the name of a code section and the initial time in ms when
	 * it was put there
	 */
	private static final HashMap<String, Long> timings = new HashMap<>();

	private LagCatcher() {
	}

	/**
	 * Puts the code section with the current ms time to the timings map
	 *
	 * @param section
	 */
	public static void start(String section) {
		if (SimpleSettings.LAG_THRESHOLD_MILLIS == 0)
			return;

		timings.put(section, System.currentTimeMillis());
	}

	/**
	 * Stops measuring time in a code section and print a console message
	 * when it took over the threshold set in {@link SimpleSettings}
	 *
	 * @param section
	 */
	public static void end(String section) {
		end(section, SimpleSettings.LAG_THRESHOLD_MILLIS);
	}

	/**
	 * Stops measuring time in a code section and print a console message
	 * when it took over the given threshold
	 *
	 * @param section
	 * @param thresholdMs
	 */
	public static void end(String section, int thresholdMs) {
		final double lag = finishAndCalculate(section);

		if (lag > thresholdMs) {
			if (SimplePlugin.hasInstance())
				Common.logNoPrefix("&3[&f" + SimplePlugin.getNamed() + " " + SimplePlugin.getVersion() + "&3] &7" + section + " took &f" + MathUtil.formatThreeDigits(lag) + " ms");
			else
				System.out.println("[LagCatcher] " + section + " took " + MathUtil.formatThreeDigits(lag) + " ms");
		}
	}

	/**
	 * Attempts to run the given code the given amount of cycles rapidly
	 * after each other, compounding the lag time together to see how long the
	 * execution takes when it is multiplied
	 *
	 * @param cycles
	 * @param name the lag section name
	 * @param code
	 */
	public static void testPerformance(int cycles, String name, Runnable code) {
		LagCatcher.start(name + "-whole");

		final List<Double> lagMap = new ArrayList<>();

		for (int i = 0; i < cycles; i++) {
			LagCatcher.start(name);
			code.run();
			lagMap.add(finishAndCalculate(name));
		}

		System.out.println("Test '" + name + "' took " + finishAndCalculate(name + "-whole") + " ms. Average " + MathUtil.average(lagMap) + " ms");
	}

	/**
	 * Calculates how long a section took (in ms) and removes it from the timings map
	 *
	 * @param section
	 * @return
	 */
	private static double finishAndCalculate(String section) {
		final Long time = timings.remove(section);

		return time == null ? 0 : System.currentTimeMillis() - time;
	}
}
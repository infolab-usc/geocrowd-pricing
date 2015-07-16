import java.util.Random;

import org.apache.commons.math3.distribution.ZipfDistribution;

/**
 * The third strategy is to consider the current distribution of user locations
 * instead of picture count distribution. Thus, we need to design the second
 * reward function based on user distribution. Intuitively, this function needs
 * to take the user-task distance into consideration, e.g., if time required for
 * all users to travel to a particular task location passes its deadline (i.e.,
 * the end time of the event), we adjust the reward of that task to zero.
 *
 */
public class PricingStrategy2 {

	public static void main(String[] args) throws Exception {
		PricingStrategy2 strat = new PricingStrategy2();
		double W = 1000;// total budget
		int k = 200; // max number of pics per location that we are willing to
						// pay for.
		int L = 20; // number of locations
		int uMin = 400;
		int uMax = 500;

		// factor f: even when the number of pics per location exceeds k, we
		// still want to pay
		// users a small amount to keep them engaged with the app. However, if
		// the number of pics
		// exceeds f*k, then we want to stop paying completely.
		double f = 1.0;

		// Flat rate per pic, aka delta in the word file.
		double flatRate = ((double) W) / (k * L);

		// the maximum factor m, which determines the maximum price per pic that
		// we are willing to pay
		// a user for the pic in order to increase the coverage. This applies
		// when some locations do not have
		// any pics, or have very few pics.
		double m = 5.0;

		System.out.println("Budget: " + W);
		System.out.println("flat rate: " + flatRate);

		/**
		 * Run multiple rounds and change f from 1.0 to 10
		 */
		for (int round = 0; round <= 100; round++) {
			double overBudget = 0;
			int rounds = 100;
			for (int i = 0; i < rounds; i++) {
				double spending = strat.estimateSpending(W, L, k, f, m, uMin,
						uMax);
				// double spending = strat.estimateSpending_skew(W, L, k, f, m,
				// uMin, uMax);
				// System.out.println(spending); //printing out the spending
				overBudget += spending - W;
			}
			System.out.println("f: " + f + ", Average over-budget: \t"
					+ overBudget / rounds);
			f = f + 0.1;
		}

	}

	/**
	 * Estimate the total of spending for an event by simulating the action of a
	 * user taking a pic at a location as a random var.
	 * 
	 * @param W
	 *            - budget
	 * @param L
	 *            - number of locations
	 * @param k
	 *            - the max number of well-paid pics for each location
	 * @param f
	 *            - the factor - after (f*k) pics has been collected in a
	 *            location, any more pics will not get paid for the location.
	 * @param m
	 *            - the value of (m*flatRate) determines the maximum amount of
	 *            money per pic we are willing
	 * @param uMin
	 *            - minimum number of users around the tasks
	 * @param uMax
	 *            - maximum number of users around the tasks to pay in order to
	 *            increase the coverage (the diversity of pics).
	 * @return the total spending.
	 */
	public double estimateSpending(double W, int L, int k, double f, double m,
			int uMin, int uMax) throws Exception {
		Random r = new Random();
		int limit = (int) (((double) k) * ((double) L) * f);// max number of
															// paid pics in all
															// locations
		int count = 0;
		double spending = 0;
		int[] picDistribution = new int[L];
		int[] userDistribution = new int[L];
		while (count < limit) {
			int loc = r.nextInt(L);
			picDistribution[loc] += 1;

			// generate userDistribution, with sum to a random number in the
			// range of uMin to uMax
			int totalUser = uMin + r.nextInt(uMax - uMin);
			generateUserDistribution(userDistribution, L, totalUser);

			// calculate the price, use the price schema
			spending += pricingSchema(W, picDistribution, userDistribution, k,
					f, loc, m);

			count++;
		}
		// printArray(picDistribution);
		// System.out.println(spending);
		return spending;
	}

	/**
	 * Similar to estimateSpending. However, instead of simple random function,
	 * we use zipf.
	 * 
	 * @param W
	 * @param L
	 * @param k
	 * @param f
	 * @param m
	 * @param uMin
	 * @param uMax
	 * @return
	 * @throws Exception
	 */
	public double estimateSpending_skew(double W, int L, int k, double f,
			double m, int uMin, int uMax) throws Exception {
		ZipfDistribution r = new ZipfDistribution(100, 1);
		Random rr = new Random();
		int limit = (int) (((double) k) * ((double) L) * f);// max number of
															// paid pics in all
															// locations
		int count = 0;
		double spending = 0;
		int[] picDistribution = new int[L];
		int[] userDistribution = new int[L];
		while (count < limit) {
			int loc = r.sample();
			loc = Math.min(L - 1, loc);
			picDistribution[loc] += 1;
			// generate userDistribution, with sum to a random number in the
			// range of 100 to 500
			int totalUser = uMin + rr.nextInt(uMax - uMin);
			generateUserDistribution(userDistribution, L, totalUser);
			// calculate the price
			spending += pricingSchema(W, picDistribution, userDistribution, k,
					f, loc, m);

			count++;
		}
		// printArray(picDistribution);
		// System.out.println(spending);
		return spending;
	}

	// generating random numbers for user distribution.
	private void generateUserDistribution(int[] userDistribution, int L, int sum) {
		int avgCount = Long.valueOf(Math.round(sum * 1.0 / L)).intValue();
		int offset = Long.valueOf(Math.round(0.5 * avgCount)).intValue();

		int cursum = 0;
		Random random = new Random(new Random().nextInt());
		for (int i = 0; i < L; i++) {
			int rand = random.nextInt(avgCount) + offset;
			if (cursum + rand > sum || i == L - 1) {
				rand = sum - cursum;
			}
			cursum += rand;
			userDistribution[i] = rand;
			if (cursum == sum) {
				break;
			}
		}
	}

	private double averageUserCountPerTask(int[] userDistribution) {
		double cnt = 0;
		for (int i = 0; i < userDistribution.length; i++) {
			cnt += userDistribution[i];
		}
		return cnt / userDistribution.length;
	}

	/**
	 * use huy's strategy first, then ding's strategy
	 * 
	 * @param W
	 * @param picDistribution
	 * @param userDistribution
	 * @param k
	 * @param f
	 * @param loc
	 * @param m
	 * @return
	 * @throws Exception
	 */
	private double pricingSchema(double W, int[] picDistribution,
			int[] userDistribution, int k, double f, int loc, double m)
			throws Exception {
		// share code for both 2nd strategy and 3rd strategy
		int len = picDistribution.length;
		int curr = picDistribution[loc];
		double flatRate = ((double) W) / (k * len);
		int total = getTotalPics(picDistribution);

		if (total <= k)
			return flatRate;
		if (curr > k) {
			if (curr > k * f)
				return 0;
			else
				return flatRate / 10;
		}

		// 2nd strategy
		double p1 = pricing(W, picDistribution, k, f, loc, m);

		// consider user count distribution
		double p2 = pricingUsers(p1, userDistribution, loc);

		return p2;
	}

	private double pricingUsers(double p, int[] userDistribution, int loc) {
		double avgUserPerTask = averageUserCountPerTask(userDistribution);
		double ratio = (avgUserPerTask + 0.0) / (userDistribution[loc]);
		return ratio * p;
	}

	/**
	 * Get the price of the next pic for a give location, given the current
	 * distribution of pics in all locations.
	 * 
	 * @param W
	 *            - budget
	 * @param picDistribution
	 *            - the current numbers of pics in the locations
	 * @param k
	 *            - the max number of well-paid pics for each location
	 * @param f
	 *            - after f*k pics have been collected at a location, no more
	 *            payment for pics in that location
	 * @param location
	 * @param m
	 *            - the value of (m*flatRate) determines the maximum amount of
	 *            money per pic we are willing to pay in order to increase the
	 *            coverage (diversity of pics).
	 * @return the price for an additional pic at location "loc".
	 */
	private double pricing(double W, int[] picDistribution, int k, double f,
			int loc, double m) throws Exception {
		int len = picDistribution.length;
		double flatRate = ((double) W) / (k * len);
		int total = getTotalPics(picDistribution);

		double[] probabilities1 = new double[len];
		double[] probabilities2 = new double[len];

		for (int i = 0; i < len; i++) {
			probabilities1[i] = ((double) picDistribution[i]) / total;
		}

		for (int i = 0; i < len; i++) {
			if (i == loc) {
				probabilities2[i] = ((double) (picDistribution[i] + 1))
						/ (total + 1);
			} else
				probabilities2[i] = ((double) picDistribution[i]) / (total + 1);
		}

		double H1 = 0, H2 = 0, D1 = 0, D2 = 0; // entropies and diversities
												// before and after adding a
												// pic.
		for (int i = 0; i < len; i++) {
			if (probabilities1[i] != 0)
				H1 += probabilities1[i] * Math.log(probabilities1[i]);
			if (probabilities2[i] != 0)
				H2 += probabilities2[i] * Math.log(probabilities2[i]);
		}
		H1 = (-1) * H1;
		H2 = (-1) * H2;
		D1 = Math.exp(H1);
		D2 = Math.exp(H2);

		double diff = D2 - D1;
		// System.out.println("50*diff="+50*diff);

		if (diff < 0)
			return flatRate / 1.1;
		if (50 * diff > m)
			return m * flatRate;
		else {
			if (50 * diff > 1)
				return 50 * diff * flatRate;
			else
				return flatRate + 50 * diff * flatRate;// to make sure the user
														// gets at least the
														// flat rate.
		}

	}

	private void printArray(int[] pics) {
		for (int x : pics) {
			System.out.print(x + "  ");
		}
		System.out.println();
	}

	private int getTotalPics(int[] picDistribution) {
		int count = 0;
		for (int num : picDistribution) {
			count += num;
		}
		return count;
	}

	/**
	 * Test the pricing() function
	 * 
	 * @throws Exception
	 */
	private static void testPricing() throws Exception {
		PricingStrategy2 strat = new PricingStrategy2();
		double W = 1000;
		int k = 30;
		double f = 3.0;
		double m = 5.0;

		int[] pics = { 13, 10, 19, 17, 110, 14, 1, 2, 10, 3, 15, 12, 8, 88, 12,
				25 };
		double flatRate = ((double) W) / (k * pics.length);

		System.out.println("flat rate: " + flatRate);

		for (int i = 0; i < pics.length; i++) {
			double price = strat.pricing(W, pics, k, f, i, m);
			System.out.println(price);
		}
	}

}

package ohm.softa.a11;

import ohm.softa.a11.openmensa.OpenMensaAPI;
import ohm.softa.a11.openmensa.OpenMensaAPIService;
import ohm.softa.a11.openmensa.model.Canteen;
import ohm.softa.a11.openmensa.model.Meal;
import ohm.softa.a11.openmensa.model.PageInfo;
import ohm.softa.a11.openmensa.model.State;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.IntStream;

/**
 * @author Peter Kurfer
 * Created on 12/16/17.
 */
public class App {
	private static final String OPEN_MENSA_DATE_FORMAT = "yyyy-MM-dd";

	private static final SimpleDateFormat dateFormat = new SimpleDateFormat(OPEN_MENSA_DATE_FORMAT, Locale.getDefault());
	private static final Scanner inputScanner = new Scanner(System.in);
	private static final OpenMensaAPI openMensaAPI = OpenMensaAPIService.getInstance().getOpenMensaAPI();
	private static final Calendar currentDate = Calendar.getInstance();
	private static int currentCanteenId = -1;

	public static void main(String[] args) {
		MenuSelection selection;
		/* loop while true to get back to the menu every time an action was performed */
		do {
			selection = menu();
			switch (selection) {
				case SHOW_CANTEENS:
					printCanteens();
					break;
				case SET_CANTEEN:
					readCanteen();
					break;
				case SHOW_MEALS:
					printMeals();
					break;
				case SET_DATE:
					readDate();
					break;
				case QUIT:
					System.exit(0);

			}
		} while (true);
	}

	private static void printCanteens() {
		System.out.print("Fetching canteens [");
		openMensaAPI.getCanteens()
			.thenApply(response -> {
				System.out.print("#");
				PageInfo pageInfo = PageInfo.extractFromResponse(response);
				List<Canteen> allCanteens;

				/* unwrapping the response body */
				if (response.body() == null) {
					/* fallback to empty list if response body was empty */
					allCanteens = new LinkedList<>();
				} else {
					allCanteens = response.body();
				}

				/* declare variable to be able to use `thenCombine` */
				CompletableFuture<List<Canteen>> remainingCanteensFuture = null;

				for (int i = 2; i <= pageInfo.getTotalCountOfPages(); i++) {
					System.out.print("#");
					/* if we're fetching the first page the future is null and has to be assigned */
					if (remainingCanteensFuture == null) {
						remainingCanteensFuture = openMensaAPI.getCanteens(i);
					} else {
						/* from the second page on the futures are combined
						 * to combine a future with another you have to provide a function to combine the results */
						remainingCanteensFuture = remainingCanteensFuture.thenCombine(openMensaAPI.getCanteens(i),
							ListUtil::mergeLists);
					}
				}


				try {
					/* collect all retrieved in one list */
					allCanteens.addAll(remainingCanteensFuture.get());
				} catch (InterruptedException | ExecutionException e) {
					e.printStackTrace();
				}
				System.out.println("]");
				/* sort the retrieved canteens by their ids and return them */
				allCanteens.sort(Comparator.comparing(Canteen::getId));
				return allCanteens;
			})
			.thenAccept(canteens -> {
				/* print all canteens to STDOUT */
				for (Canteen c : canteens) {
					System.out.println(c);
				}
			})
			.get(); /* block the thread by calling `get` to ensure that all results are retrieved when the method is completed */


	}

	private static MenuSelection printMeals() throws ExecutionException, InterruptedException {
		// TODO fetch all meals for the currently selected canteen
		// to avoid errors retrieve at first the state of the canteen and check if the canteen is opened at the selected day
		final String currentDay = dateFormat.format(currentDate.getTime());

		int id = 5;
		CompletableFuture<State> s;

		if (currentCanteenId <= 0)
		{
			System.out.println("No canteen selected.");
			return;
		}
		else{
			/* fetch the state of the canteen */
			openMensaAPI.getCanteenState(currentCanteenId, currentDay)
				.thenApply(state -> {
						if (state != null && !state.isClosed()) {
							try {
								return openMensaAPI.getMeals(currentCanteenId, currentDay).get();
							} catch (InterruptedException | ExecutionException e) {
							}
						} else {
							/* if canteen is not open - print a message and return */
							System.out.println(String.format("Seems like the canteen has closed on this date: %s",
								dateFormat.format(currentDate.getTime())));
						}
					return new LinkedList<Meal>();
				})

				// don't forget to check if a canteen was selected previously!
				.thenAccept(meals -> {
					/* print the retrieved meals to the STDOUT */
					for (Meal m : meals) {
						System.out.println(m);
					}
				})
				.get();
	}

	/**
	 * Utility method to select a canteen
	 */
	private static void readCanteen() {
		/* typical input reading pattern */
		boolean readCanteenId = false;
		do {
			try {
				System.out.println("Enter canteen id:");
				currentCanteenId = inputScanner.nextInt();
				readCanteenId = true;
			} catch (Exception e) {
				System.out.println("Sorry could not read the canteen id");
			}
		} while (!readCanteenId);
	}

	/**
	 * Utility method to read a date and update the calendar
	 */
	private static void readDate() {
		/* typical input reading pattern */
		boolean readDate = false;
		do {
			try {
				System.out.println("Pleae enter date in the format yyyy-mm-dd:");
				Date d = dateFormat.parse(inputScanner.next());
				currentDate.setTime(d);
				readDate = true;
			} catch (ParseException p) {
				System.out.println("Sorry, the entered date could not be parsed.");
			}
		} while (!readDate);

	}

	/**
	 * Utility method to print menu and read the user selection
	 *
	 * @return user selection as MenuSelection
	 */
	private static MenuSelection menu() {
		IntStream.range(0, 20).forEach(i -> System.out.print("#"));
		System.out.println();
		System.out.println("1) Show canteens");
		System.out.println("2) Set canteen");
		System.out.println("3) Show meals");
		System.out.println("4) Set date");
		System.out.println("5) Quit");
		IntStream.range(0, 20).forEach(i -> System.out.print("#"));
		System.out.println();

		switch (inputScanner.nextInt()) {
			case 1:
				return MenuSelection.SHOW_CANTEENS;
			case 2:
				return MenuSelection.SET_CANTEEN;
			case 3:
				return MenuSelection.SHOW_MEALS;
			case 4:
				return MenuSelection.SET_DATE;
			default:
				return MenuSelection.QUIT;
		}
	}
}

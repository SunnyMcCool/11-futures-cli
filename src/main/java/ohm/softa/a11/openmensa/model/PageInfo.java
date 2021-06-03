package ohm.softa.a11.openmensa.model;

import okhttp3.Headers;
import retrofit2.Response;

import java.util.List;

/**
 * Utility class to handle HTTP response header containing the pagination information
 * required to fetch all pages of an entity
 *
 * @author Peter Kurfer
 */

public final class PageInfo {

	private static final String TOTAL_PAGES_HEADER = "X-Total-Pages";
	private static final String TOTAL_ITEM_COUNT_HEADER = "X-Total-Count";
	private static final String ITEM_COUNT_PER_PAGE_HEADER = "X-Per-Page";
	private static final String CURRENT_PAGE_INDEX_HEADER = "X-Current-Page";

	private final int totalCountOfPages;
	private final int totalCountOfItems;
	private final int itemCountPerPage;
	private final int currentPageIndex;

	/**
	 * Default constructor
	 * only used by 'factory method' `extractFromReponse(...)`
	 */
	private PageInfo(int totalCountOfPages, int totalCountOfItems, int itemCountPerPage, int currentPageIndex) {
		this.totalCountOfPages = totalCountOfPages;
		this.totalCountOfItems = totalCountOfItems;
		this.itemCountPerPage = itemCountPerPage;
		this.currentPageIndex = currentPageIndex;
	}

	/**
	 * Factory method to create a PageInfo by parsing the headers of a response which includes the required information
	 *
	 * @param <T>         concrete response type
	 * @param apiResponse response object to parse
	 * @return PageInfo instance - may contain default fallback values see getters
	 */
	public static <T extends Response<?>> PageInfo extractFromResponse(Response<List<Canteen>> apiResponse) {
		Headers headers = apiResponse.headers();
		int totalPages = extractFromHeaders(headers, TOTAL_PAGES_HEADER, -1);
		int totalItemCount = extractFromHeaders(headers, TOTAL_ITEM_COUNT_HEADER, -1);
		int itemCountPerPage = extractFromHeaders(headers, ITEM_COUNT_PER_PAGE_HEADER, -1);
		int currentPageIndex = extractFromHeaders(headers, CURRENT_PAGE_INDEX_HEADER, -1);

		return new PageInfo(totalPages, totalItemCount, itemCountPerPage, currentPageIndex);
	}

	private static int extractFromHeaders(Headers headers, String headerName, int fallback) {
		String headerValue = headers.get(headerName);
		return headerValue == null ? fallback : Integer.parseInt(headerValue);
	}

	/**
	 * @return total count of pages or -1 if required header was not present
	 */
	public int getTotalCountOfPages() {
		return totalCountOfPages;
	}

	/**
	 * @return total item count or -1 if required header was not present
	 */
	public int getTotalCountOfItems() {
		return totalCountOfItems;
	}

	/**
	 * @return item count on every page or -1 if required header was not present
	 */
	public int getItemCountPerPage() {
		return itemCountPerPage;
	}

	/**
	 * @return current page index or -1 if required header was not present
	 */
	public int getCurrentPageIndex() {
		return currentPageIndex;
	}
}

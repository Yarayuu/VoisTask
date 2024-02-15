// TestData.java
public class TestData {
    private String searchQuery;
    private int totalPages;
    private int scrollBy;

    public TestData(String searchQuery, int totalPages, int scrollBy) {
        this.searchQuery = searchQuery;
        this.totalPages = totalPages;
        this.scrollBy = scrollBy;
    }

    public String getSearchQuery() {
        return searchQuery;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public int getScrollBy() {
        return scrollBy;
    }
}

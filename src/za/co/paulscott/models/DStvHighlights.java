package za.co.paulscott.models;

public class DStvHighlights {

	private String description;
	private String showtime;
	private String imageURL;
	private String author;

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getShowtime() {
		return showtime;
	}

	public void setShowtime(String showtime) {
		this.showtime = showtime;
	}

	public String getImageURL() {
		return imageURL;
	}

	public void setImageURL(String imageURL) {
		this.imageURL = imageURL;
	}

	public String getAuthor() {
		return author;
	}

	public void setAuthor(String author) {
		this.author = author;
	}

	public DStvHighlights(String description, String showtime, String imageURL,
			String author) {
		super();
		this.description = description;

		this.showtime = showtime;
		this.imageURL = imageURL;
		this.author = author;
	}

	@Override
	public String toString() {
		return "DStvHighlights [description=" + description + ", showtime="
				+ showtime + ", imageURL=" + imageURL + ", author=" + author
				+ "]";
	}

}

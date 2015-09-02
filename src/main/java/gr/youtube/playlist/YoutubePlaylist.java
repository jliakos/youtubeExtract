package gr.youtube.playlist;

import java.io.IOException;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.Playlist;
import com.google.api.services.youtube.model.PlaylistItem;
import com.google.api.services.youtube.model.PlaylistItemSnippet;
import com.google.api.services.youtube.model.PlaylistSnippet;
import com.google.api.services.youtube.model.PlaylistStatus;
import com.google.api.services.youtube.model.ResourceId;
import com.google.common.collect.Lists;

/**
 * Creates a new, private playlist in the authorized user's channel and add a
 * video to that new playlist.
 *
 * @author Jeremy Walker
 */
public class YoutubePlaylist {

	private static YouTube youtube;

	/**
	 * Authorize the user, create a playlist, and add an item to the playlist.
	 *
	 * @param args
	 *            command line args (not used).
	 * @throws IOException
	 */

	private void authorization() throws IOException {
		// This OAuth 2.0 access scope allows for full read/write access to the
		// authenticated user's account.
		List<String> scopes = Lists.newArrayList("https://www.googleapis.com/auth/youtube");

		// Authorize the request.
		Credential credential = Auth.authorize(scopes, "playlistupdates");

		// This object is used to make YouTube Data API requests.
		youtube = new YouTube.Builder(Auth.HTTP_TRANSPORT, Auth.JSON_FACTORY, credential)
				.setApplicationName("youtube-cmdline-playlistupdates-sample").build();

		// Create a new, private playlist in the authorized user's channel.

	}

	public void createPlaylist(String playListname, List<String> ids) throws IOException {
		authorization();
		String playListId = insertPlaylist(playListname);
		try {
			// If a valid playlist was created, add a video to that playlist.
			for (String videoid : ids) {
				insertPlaylistItem(playListId, videoid);
			}

		} catch (

		GoogleJsonResponseException e)

		{
			System.err.println(
					"There was a service error: " + e.getDetails().getCode() + " : " + e.getDetails().getMessage());
			// e.printStackTrace();
		} catch (

		IOException e)

		{
			System.err.println("IOException: " + e.getMessage());

		} catch (

		Throwable t)

		{
			System.err.println("Throwable: " + t.getMessage());
			t.printStackTrace();
		}

	}

	public static void main(String[] args) {
		YoutubePlaylist playlist = new YoutubePlaylist();

	}

	/**
	 * Create a playlist and add it to the authorized account.
	 */
	private static String insertPlaylist(String name) throws IOException {

		// This code constructs the playlist resource that is being inserted.
		// It defines the playlist's title, description, and privacy status.
		PlaylistSnippet playlistSnippet = new PlaylistSnippet();
		playlistSnippet.setTitle(StringUtils.isEmpty(name) ? "Test Playlist" : name);
		playlistSnippet.setDescription("A private playlist containing great videos");
		PlaylistStatus playlistStatus = new PlaylistStatus();
		playlistStatus.setPrivacyStatus("private");

		Playlist youTubePlaylist = new Playlist();
		youTubePlaylist.setSnippet(playlistSnippet);
		youTubePlaylist.setStatus(playlistStatus);

		// Call the API to insert the new playlist. In the API call, the first
		// argument identifies the resource parts that the API response should
		// contain, and the second argument is the playlist being inserted.
		//y/outube.playlists()
		YouTube.Playlists.Insert playlistInsertCommand = youtube.playlists().insert("snippet,status", youTubePlaylist);

		Playlist playlistInserted = playlistInsertCommand.execute();

		// Print data from the API response and return the new playlist's
		// unique playlist ID.
		System.out.println("New Playlist name: " + playlistInserted.getSnippet().getTitle());
		System.out.println(" - Privacy: " + playlistInserted.getStatus().getPrivacyStatus());
		System.out.println(" - Description: " + playlistInserted.getSnippet().getDescription());
		System.out.println(" - Posted: " + playlistInserted.getSnippet().getPublishedAt());
		System.out.println(" - Channel: " + playlistInserted.getSnippet().getChannelId() + "\n");
		return playlistInserted.getId();

	}

	/**
	 * Create a playlist item with the specified video ID and add it to the
	 * specified playlist.
	 *
	 * @param playlistId
	 *            assign to newly created playlistitem
	 * @param videoId
	 *            YouTube video id to add to playlistitem
	 */
	private static String insertPlaylistItem(String playlistId, String videoId) throws IOException {

		// Define a resourceId that identifies the video being added to the
		// playlist.
		ResourceId resourceId = new ResourceId();
		resourceId.setKind("youtube#video");
		resourceId.setVideoId(videoId);

		// Set fields included in the playlistItem resource's "snippet" part.
		PlaylistItemSnippet playlistItemSnippet = new PlaylistItemSnippet();
		playlistItemSnippet.setTitle("First video in the test playlist");
		playlistItemSnippet.setPlaylistId(playlistId);
		playlistItemSnippet.setResourceId(resourceId);

		// Create the playlistItem resource and set its snippet to the
		// object created above.
		PlaylistItem playlistItem = new PlaylistItem();
		playlistItem.setSnippet(playlistItemSnippet);

		// Call the API to add the playlist item to the specified playlist.
		// In the API call, the first argument identifies the resource parts
		// that the API response should contain, and the second argument is
		// the playlist item being inserted.
		YouTube.PlaylistItems.Insert playlistItemsInsertCommand = youtube.playlistItems()
				.insert("snippet,contentDetails", playlistItem);
		PlaylistItem returnedPlaylistItem = null;
		try {
			returnedPlaylistItem = playlistItemsInsertCommand.execute();
		} catch (IOException e) {
			System.err.println(videoId + " could not be inserted");

		}

		// Print data from the API response and return the new playlist
		// item's unique playlistItem ID.
		if (returnedPlaylistItem != null) {
			System.out.println("New PlaylistItem name: " + returnedPlaylistItem.getSnippet().getTitle());
			System.out.println(" - Video id: " + returnedPlaylistItem.getSnippet().getResourceId().getVideoId());
			System.out.println(" - Posted: " + returnedPlaylistItem.getSnippet().getPublishedAt());
			System.out.println(" - Channel: " + returnedPlaylistItem.getSnippet().getChannelId());
			return returnedPlaylistItem.getId();
		}
		return null;

	}
}
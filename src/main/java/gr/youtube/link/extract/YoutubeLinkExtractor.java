package gr.youtube.link.extract;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.UrlValidator;

import facebook4j.Facebook;
import facebook4j.FacebookException;
import facebook4j.FacebookFactory;
import facebook4j.Paging;
import facebook4j.Post;
import facebook4j.ResponseList;
import facebook4j.auth.AccessToken;
import gr.youtube.cli.CliArgs;
import gr.youtube.playlist.YoutubePlaylist;

public class YoutubeLinkExtractor {
	private static final String TEST_PLAYLIST = "Test Playlist";
	private static Integer MAX_LINKS = 100;
	private Pattern pattern;

	
	private final String YOUTUBE_PATTERN = "(http:|https:)?\\/\\/(www\\.)?(youtube.com|youtu.be)\\/(watch)?(\\?v=)?(\\S+)?";
	final Pattern VIDEO_ID = Pattern.compile(
			"(?<=watch\\?v=|/videos/|embed\\/|youtu.be\\/|\\/v\\/|\\/e\\/|watch\\?v%3D|watch\\?feature=player_embedded&v=|%2Fvideos%2F|embed%\u200C\u200B2F|youtu.be%2F|%2Fv%2F)[^#\\&\\?\\n]*");

	public YoutubeLinkExtractor() {
		pattern = Pattern.compile(YOUTUBE_PATTERN, Pattern.CASE_INSENSITIVE);

	}

	public CopyOnWriteArrayList<String> grabHTMLLinks(String html) throws UnsupportedEncodingException {

		CopyOnWriteArrayList<String> result = new CopyOnWriteArrayList<String>();

		Matcher matcherTag = pattern.matcher(html);

		while (matcherTag.find()) {
			result.add(URLDecoder.decode(matcherTag.group().replaceAll("\"", "").replaceAll("<img", "")
					.replaceAll(">", "").replaceAll("'", ""), "UTF-8"));

		}

		return result;

	}

	private CopyOnWriteArrayList<String> fetchYouTubeLinks(String group, Integer max)
			throws UnsupportedEncodingException, FacebookException {

		Facebook facebook = new FacebookFactory().getInstance();
		CopyOnWriteArrayList<String> htmlLinks = new CopyOnWriteArrayList<String>();
		facebook.setOAuthAppId("740976366014657", "d63eba362866f9b00ec710c5fd8999be");
		facebook.setOAuthPermissions("email");
		AccessToken accessToken = facebook.getOAuthAppAccessToken();

		facebook.setOAuthAccessToken(accessToken);

		ResponseList<Post> feed = facebook.getGroupFeed(group);
		Paging<Post> paging = null;
		do {
			feed.parallelStream().forEach(post -> {
				// for (Post post : feed) {
				if ((post.getMessage() != null)) {
					CopyOnWriteArrayList<String> grabHTMLLinks = null;
					try {
						grabHTMLLinks = grabHTMLLinks(post.getMessage());
					} catch (Exception e) {
						
						e.printStackTrace();
					}
					if (CollectionUtils.isNotEmpty(grabHTMLLinks)) {
						if (htmlLinks.size() + grabHTMLLinks.size() <= max) {
							htmlLinks.addAll(grabHTMLLinks);
							printList(htmlLinks);
						}
					}

				}

			});

			paging = feed.getPaging();
		} while ((paging != null) && (htmlLinks.size() < max) && ((feed = facebook.fetchNext(paging)) != null));
		return htmlLinks;
	}

	private void printList(CopyOnWriteArrayList<String> list) {
		
		list.stream().forEach(htmlLink -> {

			System.out.println("Found video...."  + "\n" + htmlLink);
			

		});
	}

	private String getVideoID(String url) {
		/*
		 * final String pattern ="?v="; int position =url.indexOf(pattern);
		 * 
		 * if(position !=-1 ) { return url.substring(position +
		 * pattern.length()); } return null;
		 */

		Matcher m = VIDEO_ID.matcher(url);
		if (m.find()) {
			return m.group();
		}
		return null;
	}

	private static void printUsage() {
		System.out.println(
				"********** USAGE -group facebookGroupName -url url to crawl -max max links to store default 100 --youtube -name playlistname");
	}

	public static void main(String[] args) throws IOException, URISyntaxException, FacebookException {
		if (ArrayUtils.isEmpty(args)) {
			printUsage();
			System.exit(1);
		}
		CliArgs cliArgs = new CliArgs(args);
		String group = cliArgs.switchValue("-group");
		String maxValue = cliArgs.switchValue("-max");
		String name = cliArgs.switchValue("-name");
		String url = cliArgs.switchValue("-url");
		String youtube = cliArgs.switchValue("--youtube");
		Integer max = MAX_LINKS;
		if (!StringUtils.isEmpty(maxValue)) {
			max = Integer.parseInt(maxValue);
		}
		if (StringUtils.isEmpty(name)) {
			name = TEST_PLAYLIST;
		}
		if (StringUtils.isEmpty(url) && StringUtils.isEmpty(group)) {
			printUsage();
			System.exit(1);
		}
		YoutubeLinkExtractor htmlLinkExtractor = new YoutubeLinkExtractor();
		CopyOnWriteArrayList<String> htmlLinks = new CopyOnWriteArrayList<>();
		if (StringUtils.isEmpty(url)) {
			System.out.println(
					"facebook group = " + group + " with max links = " + max + " with playlist name = " + name);

			System.out.println("Connecting to group " + group);
			htmlLinks = htmlLinkExtractor.fetchYouTubeLinks(group, max);
		} else {
			UrlValidator defaultValidator = new UrlValidator();
			if (defaultValidator.isValid(url)) {
				System.out.println(
						"connecting to url   = " + url + " with max links = " + max + " with playlist name = " + name);
				htmlLinks = htmlLinkExtractor.grabHTMLLinks(IOUtils.toString(new URL(url)));
				htmlLinkExtractor.printList(htmlLinks);
			} else {
				System.err.println("The specified url is not correct");
				System.exit(1);
			}
		}

		// String out = new Scanner(new
		// URL("http://www.google.com").openStream(),
		// "UTF-8").useDelimiter("\\A").next();
		/*
		 * List<String> htmlLinks = htmlLinkExtractor
		 * .grabHTMLLinks(IOUtils.toString(new
		 * URL("https://www.facebook.com/YOUcover")));
		 */

		if (CollectionUtils.isEmpty(htmlLinks)) {
			System.out.println("No youtube links found");
		} else {
			List<String> ids = new ArrayList<>();
			htmlLinks.parallelStream().forEach(link -> {
				String id = htmlLinkExtractor.getVideoID(link);
				if (StringUtils.isNotEmpty(id)) {
					ids.add(id);
				}

			});
			if (StringUtils.isNotEmpty(youtube)) {
				System.out.println("Uploading to youtube...");
				YoutubePlaylist playlist = new YoutubePlaylist();
				playlist.createPlaylist(name, ids);
			}
		}

	}

}
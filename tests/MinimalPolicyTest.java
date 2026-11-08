import com.feurstagram.extension.MinimalPolicy;
import java.net.URI;

public final class MinimalPolicyTest {
    private static int count;
    private static void check(boolean actual, boolean expected, String label) {
        count++;
        if (actual != expected) throw new AssertionError(label + ": got " + actual);
    }
    public static void main(String[] args) {
        String[] blocked = {
            "/api/v1/feed/timeline/", "/api/v1/feed/user/123/", "/api/v1/feed/reels_media/",
            "/api/v1/feed/injected_reels_media_www/", "/api/v1/clips/item/", "/api/v1/clips/home/",
            "/api/v1/stories/reel/", "/api/v1/highlights/123/highlights_tray/", "/api/v1/usertags/123/feed/",
            "/api/v1/discover/topical_explore/", "/api/v1/discover/explore_grid/",
            "/api/v1/fbsearch/serp/", "/api/v1/fbsearch/reels_search/", "/api/v1/media/123_456/info/",
            "/api/v1/media/123/comments/", "/api/v1/logging/client_event/", "/api/v1/stats/"
        };
        for (String path : blocked) check(MinimalPolicy.blocksRequest(URI.create("https://i.instagram.com" + path)), true, path);
        String[] allowed = {
            "/api/v1/direct_v2/inbox/", "/api/v1/direct_v2/threads/123/items/", "/api/v1/direct_v2/threads/broadcast/text/",
            "/api/v1/direct_v2/threads/broadcast/configure_photo/", "/api/v1/direct_v2/threads/123/seen/",
            "/api/v1/notes/get_notes/", "/api/v1/notes/create_note/", "/api/v1/users/search/",
            "/api/v1/fbsearch/topsearch_flat/", "/api/v1/users/123/info/", "/api/v1/friendships/123/followers/",
            "/api/v1/friendships/create/123/", "/api/v1/accounts/login/", "/api/v1/accounts/two_factor_login/",
            "/api/v1/accounts/edit_profile/", "/api/v1/news/inbox/", "/api/v1/push/register/",
            "/api/v1/media/configure_to_direct_thread/", "/rupload_igphoto/123", "/api/v1/fbupload/",
            "/api/v1/async_critical_notices/", "/api/v1/qe/sync/"
        };
        for (String path : allowed) check(MinimalPolicy.blocksRequest(URI.create("https://i.instagram.com" + path)), false, path);
        String[] media = {
            "https://www.instagram.com/p/ABC/", "https://instagram.com/reel/ABC/", "https://instagram.com/reels/ABC/",
            "https://instagram.com/stories/dean/123/", "https://instagram.com/stories/highlights/123/",
            "https://instagram.com/tv/ABC/", "https://instagram.com/share/reel/ABC", "https://instagram.com/dean/reels/",
            "instagram://media?id=123", "instagram://reel?media_id=123", "instagram://stories?user_id=123",
            "https://l.instagram.com/?u=https%3A%2F%2Fwww.instagram.com%2Fp%2FABC%2F",
            "https://www.instagram.com/%70/ABC/", "https://instagram.com/dean/../p/ABC/"
        };
        for (String link : media) check(MinimalPolicy.blocksLink(link), true, link);
        String[] social = {
            "https://instagram.com/dean/", "instagram://user?username=dean", "instagram://direct-inbox",
            "https://instagram.com/direct/t/123/", "instagram://news", "https://instagram.com/accounts/edit/",
            "https://example.com/p/ABC/", "https://instagram.com.evil.example/p/ABC/", "hello", ""
        };
        for (String link : social) check(MinimalPolicy.blocksLink(link), false, link);
        check(MinimalPolicy.blocksLink(null), false, "null link");
        check(MinimalPolicy.blocksRequest(null), false, "null URI");
        check(MinimalPolicy.blocksModule("clips_viewer_clips_tab"), true, "clips module");
        check(MinimalPolicy.blocksModule("direct_inbox"), false, "DM module");
        check(MinimalPolicy.blocksModule("newsfeed_you"), false, "notifications module");
        check(MinimalPolicy.blocksModule("edit_profile"), false, "account management");
        System.out.println("PASS " + count + " policy regression checks");
    }
}

#include "MediaRoutes.h"

#include <stdio.h>

int main(void) {
    const struct {
        const char *scheme;
        const char *host;
        const char *path;
        bool expected;
    } cases[] = {
        {"https", "instagram.com", "/p/abc/", true},
        {"https", "www.instagram.com", "/ReEL/abc/", true},
        {"HTTPS", "M.INSTAGRAM.COM.", "//Stories/example/123/", true},
        {"https", "instagram.com", "/tv/abc", true},
        {"https", "instagram.com", "/clips/abc", true},
        {"https", "instagram.com", "/share/reel/abc", true},
        {"https", "instagram.com", "/example/reels/", true},
        {"https", "instagram.com", "/example/tagged/", true},
        {"http", "instagram.com", "/reels", true},
        {"https", "ig.me", "/reel/example", true},
        {"instagram", "media", "", true},
        {"instagram", NULL, "/media", true},
        {"instagram", "story", "/", true},
        {"instagram", "camera", "", true},
        {"instagram", "reels", NULL, true},
        {"https", "instagram.com", "/", false},
        {"https", "instagram.com", "/accounts/login/", false},
        {"https", "instagram.com", "/accounts/reels/", false},
        {"https", "instagram.com", "/challenge/reels", false},
        {"https", "instagram.com", "/direct/inbox/", false},
        {"https", "instagram.com", "/direct/t/123", false},
        {"https", "instagram.com", "/explore/search/", false},
        {"https", "instagram.com", "/accounts/activity/", false},
        {"https", "instagram.com", "/example/", false},
        {"https", "instagram.com", "/example/followers/", false},
        {"https", "instagram.com", "/example/reels_fan/", false},
        {"https", "ig.me", "/m/example", false},
        {"https", "instagram.com.evil.example", "/p/123", false},
        {"https", "notinstagram.com", "/stories/123", false},
        {"https", "notig.me", "/p/123", false},
        {"https", "facebook.com", "/reel/123", false},
        {"file", "instagram.com", "/p/123", false},
        {"instagram", "user", "/example", false},
        {"instagram", "direct", "/t/123", false},
        {"instagram", "news", "", false},
        {"instagram", "accounts", "/activity", false},
        {NULL, "instagram.com", "/p/123", false},
        {"https", NULL, "/p/123", false},
        {"https", "instagram.com", NULL, false},
    };
    size_t count = sizeof(cases) / sizeof(cases[0]);
    for (size_t i = 0; i < count; i++) {
        if (IMLBlocksMediaRoute(cases[i].scheme, cases[i].host, cases[i].path) != cases[i].expected) {
            fprintf(stderr, "Media route case %zu failed\n", i + 1);
            return 1;
        }
    }
    printf("%zu media route checks passed\n", count);
    return 0;
}

#include "MediaRoutes.h"

#include <stddef.h>
#include <string.h>
#include <strings.h>

static bool hasDomain(const char *host, const char *domain) {
    size_t hostLength = strlen(host);
    if (hostLength && host[hostLength - 1] == '.') {
        hostLength--;
    }
    size_t domainLength = strlen(domain);
    if (hostLength < domainLength) {
        return false;
    }
    size_t offset = hostLength - domainLength;
    return (offset == 0 || host[offset - 1] == '.') &&
           strncasecmp(host + offset, domain, domainLength) == 0;
}

static const char *nextComponent(const char *path, size_t *length) {
    while (*path == '/') {
        path++;
    }
    *length = strcspn(path, "/");
    return path;
}

static bool equalsComponent(const char *component, size_t length, const char *value) {
    return length == strlen(value) && strncasecmp(component, value, length) == 0;
}

static bool isMediaComponent(const char *component, size_t length) {
    const char *const blocked[] = {"p", "reel", "reels", "clips", "story", "stories", "tv"};
    for (size_t i = 0; i < sizeof(blocked) / sizeof(blocked[0]); i++) {
        if (equalsComponent(component, length, blocked[i])) {
            return true;
        }
    }
    return false;
}

bool IMLBlocksMediaRoute(const char *scheme, const char *host, const char *path) {
    if (!scheme) {
        return false;
    }
    path = path ? path : "";
    size_t length;
    const char *first = nextComponent(path, &length);
    if (strcasecmp(scheme, "instagram") == 0) {
        if (host && *host) {
            size_t hostLength = strlen(host);
            return isMediaComponent(host, hostLength) ||
                   equalsComponent(host, hostLength, "media") ||
                   equalsComponent(host, hostLength, "camera");
        }
        return isMediaComponent(first, length) || equalsComponent(first, length, "media") ||
               equalsComponent(first, length, "camera");
    }
    if ((strcasecmp(scheme, "https") != 0 && strcasecmp(scheme, "http") != 0) ||
        !host || (!hasDomain(host, "instagram.com") && !hasDomain(host, "ig.me"))) {
        return false;
    }
    if (isMediaComponent(first, length)) {
        return true;
    }
    size_t secondLength;
    const char *second = nextComponent(first + length, &secondLength);
    if (equalsComponent(first, length, "share")) {
        return isMediaComponent(second, secondLength);
    }
    const char *const nonProfileRoots[] = {"accounts", "direct", "oauth", "challenge", "about", "api"};
    for (size_t i = 0; i < sizeof(nonProfileRoots) / sizeof(nonProfileRoots[0]); i++) {
        if (equalsComponent(first, length, nonProfileRoots[i])) {
            return false;
        }
    }
    return equalsComponent(second, secondLength, "reels") ||
           equalsComponent(second, secondLength, "tagged");
}

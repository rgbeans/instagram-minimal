#ifndef MINIMAL_MEDIA_ROUTES_H
#define MINIMAL_MEDIA_ROUTES_H

#include <stdbool.h>

// Pass the parsed scheme and host, and the decoded path. Each may be null.
bool IMLBlocksMediaRoute(const char *scheme, const char *host, const char *path);

#endif

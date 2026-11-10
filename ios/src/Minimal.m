#import <UIKit/UIKit.h>
#import <objc/runtime.h>
#include <string.h>

#import "MediaRoutes.h"

typedef IMP (^IMLReplacement)(IMP original);

// Check the method ABI before using private selectors, which may change between releases.
static BOOL IMLInstall(Class owner, SEL selector, const char *types, IMLReplacement makeReplacement) {
    if (owner == Nil) {
        return NO;
    }
    Method method = class_getInstanceMethod(owner, selector);
    if (method == NULL) {
        return NO;
    }
    const char *encoding = method_getTypeEncoding(method);
    NSMethodSignature *signature = [NSMethodSignature signatureWithObjCTypes:encoding];
    NSUInteger count = signature.numberOfArguments;
    if (count < 2 || strlen(types) != count - 1 || signature.methodReturnType[0] != types[0]) {
        return NO;
    }
    for (NSUInteger index = 2; index < count; index++) {
        if ([signature getArgumentTypeAtIndex:index][0] != types[index - 1]) {
            return NO;
        }
    }
    IMP replacement = makeReplacement(method_getImplementation(method));
    class_replaceMethod(owner, selector, replacement, encoding);
    return YES;
}

static BOOL IMLBlocksURL(id candidate) {
    NSURL *url;
    if ([candidate isKindOfClass:NSURL.class]) {
        url = candidate;
    } else if ([candidate isKindOfClass:NSString.class]) {
        url = [NSURL URLWithString:candidate];
    } else {
        return NO;
    }
    for (NSUInteger depth = 0; url != nil && depth < 4; depth++) {
        NSURLComponents *components = [NSURLComponents componentsWithURL:url resolvingAgainstBaseURL:YES];
        NSString *path = components.percentEncodedPath.stringByRemovingPercentEncoding;
        if (IMLBlocksMediaRoute(components.scheme.UTF8String, components.host.UTF8String, path.UTF8String)) {
            return YES;
        }
        if (![components.host.lowercaseString isEqualToString:@"l.instagram.com"] ||
            ![components.path isEqualToString:@"/"]) {
            return NO;
        }
        url = nil;
        for (NSURLQueryItem *item in components.queryItems) {
            if ([item.name isEqualToString:@"u"]) {
                url = [NSURL URLWithString:item.value ?: @""];
                break;
            }
        }
    }
    return NO;
}

static BOOL IMLIsKindOfNamedClass(id object, NSString *name) {
    Class cls = NSClassFromString(name);
    return cls != Nil && [object isKindOfClass:cls];
}

static BOOL IMLIsMediaViewer(UIViewController *controller) {
    for (NSString *name in @[@"IGSundialFeedViewController", @"IGContextualFeedViewController",
                             @"IGChainingFeedViewController", @"IGVideoFeedViewController",
                             @"IGSingleFeedViewController", @"IGSingleFeedViewController2",
                             @"IGStoryViewerViewController"]) {
        if (IMLIsKindOfNamedClass(controller, name)) {
            return YES;
        }
    }
    if ([controller isKindOfClass:UINavigationController.class]) {
        UIViewController *top = ((UINavigationController *)controller).topViewController;
        return top != nil && IMLIsMediaViewer(top);
    }
    return NO;
}

static void IMLInstallViewerHooks(void) {
    IMLInstall(UINavigationController.class, @selector(pushViewController:animated:), "v@B", ^IMP(IMP original) {
        return imp_implementationWithBlock(^(UINavigationController *self, UIViewController *controller, BOOL animated) {
            if (!IMLIsMediaViewer(controller)) {
                ((void (*)(id, SEL, UIViewController *, BOOL))original)(self, @selector(pushViewController:animated:),
                                                                        controller, animated);
            }
        });
    });
    SEL present = @selector(presentViewController:animated:completion:);
    IMLInstall(UIViewController.class, present, "v@B@", ^IMP(IMP original) {
        return imp_implementationWithBlock(^(UIViewController *self, UIViewController *controller, BOOL animated,
                                              void (^completion)(void)) {
            if (!IMLIsMediaViewer(controller)) {
                ((void (*)(id, SEL, UIViewController *, BOOL, id))original)(self, present, controller, animated, completion);
            }
        });
    });
}

static void IMLInstallLinkHooks(Class delegate) {
    SEL open = @selector(application:openURL:options:);
    IMLInstall(delegate, open, "B@@@", ^IMP(IMP original) {
        return imp_implementationWithBlock(^BOOL(id self, UIApplication *application, NSURL *url, NSDictionary *options) {
            return IMLBlocksURL(url) ||
                   ((BOOL (*)(id, SEL, UIApplication *, NSURL *, NSDictionary *))original)(self, open, application, url, options);
        });
    });
    SEL legacy = @selector(application:openURL:sourceApplication:annotation:);
    IMLInstall(delegate, legacy, "B@@@@", ^IMP(IMP original) {
        return imp_implementationWithBlock(^BOOL(id self, UIApplication *application, NSURL *url,
                                                NSString *source, id annotation) {
            return IMLBlocksURL(url) ||
                   ((BOOL (*)(id, SEL, UIApplication *, NSURL *, NSString *, id))original)(
                       self, legacy, application, url, source, annotation);
        });
    });
    SEL continueActivity = @selector(application:continueUserActivity:restorationHandler:);
    IMLInstall(delegate, continueActivity, "B@@@", ^IMP(IMP original) {
        return imp_implementationWithBlock(^BOOL(id self, UIApplication *application, NSUserActivity *activity, id restore) {
            return IMLBlocksURL(activity.webpageURL) ||
                   ((BOOL (*)(id, SEL, UIApplication *, NSUserActivity *, id))original)(self, continueActivity,
                                                                                      application, activity, restore);
        });
    });
    Class helper = object_getClass(NSClassFromString(@"IGURLHelper"));
    SEL route = NSSelectorFromString(@"openURL:controller:");
    IMLInstall(helper, route, "v@@", ^IMP(IMP original) {
        return imp_implementationWithBlock(^(id self, id url, id controller) {
            if (!IMLBlocksURL(url)) {
                ((void (*)(id, SEL, id, id))original)(self, route, url, controller);
            }
        });
    });
}

static BOOL IMLIsBlockedSurface(id surface) {
    if (!IMLIsKindOfNamedClass(surface, @"IGMainAppSurfaceIntent")) {
        return NO;
    }
    SEL tabName = NSSelectorFromString(@"tabStringFromSurfaceIntent");
    if (![surface respondsToSelector:tabName]) {
        return NO;
    }
    id name = ((id (*)(id, SEL))[surface methodForSelector:tabName])(surface, tabName);
    if ([name isEqual:@"CLIPS"] || [name isEqual:@"CREATE"]) {
        return YES;
    }
    // Some versions identify Create by its surface subtype instead of its name.
    Ivar subtype = class_getInstanceVariable([surface class], "_subtype");
    if (subtype != NULL) {
        id value = [surface valueForKey:@"_subtype"];
        return [value isKindOfClass:NSNumber.class] && [value unsignedIntegerValue] == 3;
    }
    return NO;
}

static NSArray *IMLVisibleSurfaces(NSArray *surfaces) {
    if (![surfaces isKindOfClass:NSArray.class]) {
        return surfaces;
    }
    NSMutableArray *visible = [NSMutableArray arrayWithCapacity:surfaces.count];
    for (id surface in surfaces) {
        if (!IMLIsBlockedSurface(surface)) {
            [visible addObject:surface];
        }
    }
    return visible.count ? visible : surfaces;
}

static void IMLInstallTabs(void) {
    SEL swiping = NSSelectorFromString(@"isTabSwipingEnabled");
    IMLInstall(NSClassFromString(@"_TtC18IGNavConfiguration18IGNavConfiguration"), swiping, "B",
               ^IMP(__unused IMP original) {
        return imp_implementationWithBlock(^BOOL(__unused id self) {
            return NO;
        });
    });
    Class tabController = NSClassFromString(@"IGTabBarController");
    SEL layout = NSSelectorFromString(@"_layoutTabBar");
    Ivar surfaces = tabController ? class_getInstanceVariable(tabController, "_tabBarSurfaces") : NULL;
    if (surfaces != NULL && ivar_getTypeEncoding(surfaces)[0] == '@') {
        IMLInstall(tabController, layout, "v", ^IMP(IMP original) {
            return imp_implementationWithBlock(^(id self) {
                NSArray *current = object_getIvar(self, surfaces);
                NSArray *visible = IMLVisibleSurfaces(current);
                if (![current isEqual:visible]) {
                    object_setIvar(self, surfaces, visible);
                }
                ((void (*)(id, SEL))original)(self, layout);
            });
        });
    }
    SEL button = NSSelectorFromString(@"_buttonForTabBarSurface:");
    IMLInstall(tabController, button, "@@", ^IMP(IMP original) {
        return imp_implementationWithBlock(^id(id self, id surface) {
            return IMLIsBlockedSurface(surface) ? nil : ((id (*)(id, SEL, id))original)(self, button, surface);
        });
    });
}

static void IMLInstallFeedAndSearch(void) {
    SEL list = NSSelectorFromString(@"objectsForListAdapter:");
    IMLInstall(NSClassFromString(@"IGMainFeedListAdapterDataSource"), list, "@@", ^IMP(__unused IMP original) {
        return imp_implementationWithBlock(^id(__unused id self, __unused id adapter) {
            return @[];
        });
    });
    IMLInstall(NSClassFromString(@"IGSearchMainTypeaheadListViewController"), list, "@@", ^IMP(IMP original) {
        return imp_implementationWithBlock(^id(id self, id adapter) {
            NSArray *rows = ((id (*)(id, SEL, id))original)(self, list, adapter);
            if (![rows isKindOfClass:NSArray.class]) {
                return rows;
            }
            NSMutableArray *filtered = [NSMutableArray arrayWithCapacity:rows.count];
            for (id row in rows) {
                if (!IMLIsKindOfNamedClass(row, @"IGMedia") && !IMLIsKindOfNamedClass(row, @"IGDiscoveryGridItem") &&
                    !IMLIsKindOfNamedClass(row, @"IGFeedItem")) {
                    [filtered addObject:row];
                }
            }
            return filtered;
        });
    });
    SEL load = @selector(viewDidLoad);
    IMLInstall(NSClassFromString(@"IGExploreGridViewController"), load, "v", ^IMP(IMP original) {
        return imp_implementationWithBlock(^(UIViewController *self) {
            ((void (*)(id, SEL))original)(self, load);
            self.view.hidden = YES;
            self.view.accessibilityElementsHidden = YES;
        });
    });
}

// Store the original state so a reused profile cell can become visible in a different controller.
static const char IMLSavedVisibilityKey;

static void IMLApplyViewBlock(UIView *view, BOOL block) {
    NSArray<NSNumber *> *state = objc_getAssociatedObject(view, &IMLSavedVisibilityKey);
    if (block) {
        if (state == nil) {
            state = @[@(view.hidden), @(view.userInteractionEnabled), @(view.accessibilityElementsHidden)];
            objc_setAssociatedObject(view, &IMLSavedVisibilityKey, state, OBJC_ASSOCIATION_RETAIN_NONATOMIC);
        }
        view.hidden = YES;
        view.userInteractionEnabled = NO;
        view.accessibilityElementsHidden = YES;
    } else if (state != nil) {
        view.hidden = state[0].boolValue;
        view.userInteractionEnabled = state[1].boolValue;
        view.accessibilityElementsHidden = state[2].boolValue;
        objc_setAssociatedObject(view, &IMLSavedVisibilityKey, nil, OBJC_ASSOCIATION_RETAIN_NONATOMIC);
    }
}

static BOOL IMLIsInProfile(UIView *view) {
    for (UIResponder *responder = view.nextResponder; responder != nil; responder = responder.nextResponder) {
        if (IMLIsKindOfNamedClass(responder, @"IGUserDetailViewController")) {
            return YES;
        }
    }
    return NO;
}

static void IMLInstallCellBlock(NSString *className, BOOL profileOnly) {
    Class cls = NSClassFromString(className);
    if (cls == Nil || ![cls isSubclassOfClass:UIView.class]) {
        return;
    }
    SEL layout = @selector(layoutSubviews);
    IMLInstall(cls, layout, "v", ^IMP(IMP original) {
        return imp_implementationWithBlock(^(UIView *view) {
            BOOL block = !profileOnly || IMLIsInProfile(view);
            if (!block) {
                IMLApplyViewBlock(view, NO);
            }
            ((void (*)(id, SEL))original)(view, layout);
            if (block) {
                IMLApplyViewBlock(view, YES);
            }
        });
    });
}

__attribute__((constructor))
static void IMLInitialize(void) {
    @autoreleasepool {
        NSString *executable = [NSBundle.mainBundle objectForInfoDictionaryKey:@"CFBundleExecutable"];
        if (![executable isEqualToString:@"Instagram"]) {
            return;
        }
        IMLInstallViewerHooks();
        IMLInstallLinkHooks(NSClassFromString(@"AppDelegate"));
        IMLInstallTabs();
        IMLInstallFeedAndSearch();
        IMLInstallCellBlock(@"IGDirectMediaReshareCell", NO);
        IMLInstallCellBlock(@"IGDirectReelShareCell", NO);
        IMLInstallCellBlock(@"IGPhotoGridCell", YES);
    }
}

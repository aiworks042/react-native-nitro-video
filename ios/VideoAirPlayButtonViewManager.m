#import <React/RCTViewManager.h>

@interface RCT_EXTERN_MODULE(VideoAirPlayButtonViewManager, RCTViewManager)

RCT_EXPORT_VIEW_PROPERTY(tint, UIColor)
RCT_EXPORT_VIEW_PROPERTY(activeTintColor, UIColor)
RCT_EXPORT_VIEW_PROPERTY(prioritizeVideoDevices, BOOL)
RCT_EXPORT_VIEW_PROPERTY(onBeginPresentingRoutes, RCTDirectEventBlock)
RCT_EXPORT_VIEW_PROPERTY(onEndPresentingRoutes, RCTDirectEventBlock)

@end

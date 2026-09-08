#import <React/RCTViewManager.h>

@interface RCT_EXTERN_MODULE(NitroVideoViewManager, RCTViewManager)

RCT_EXPORT_VIEW_PROPERTY(playerId, NSNumber)
RCT_EXPORT_VIEW_PROPERTY(nativeControls, BOOL)
RCT_EXPORT_VIEW_PROPERTY(contentFit, NSString)
RCT_EXPORT_VIEW_PROPERTY(allowsPictureInPicture, BOOL)
RCT_EXPORT_VIEW_PROPERTY(startsPictureInPictureAutomatically, BOOL)
RCT_EXPORT_VIEW_PROPERTY(requiresLinearPlayback, BOOL)
RCT_EXPORT_VIEW_PROPERTY(useExoShutter, BOOL)
RCT_EXPORT_VIEW_PROPERTY(controllerAutoShow, BOOL)

RCT_EXPORT_VIEW_PROPERTY(onPictureInPictureStart, RCTDirectEventBlock)
RCT_EXPORT_VIEW_PROPERTY(onPictureInPictureStop, RCTDirectEventBlock)
RCT_EXPORT_VIEW_PROPERTY(onFullscreenEnter, RCTDirectEventBlock)
RCT_EXPORT_VIEW_PROPERTY(onFullscreenExit, RCTDirectEventBlock)
RCT_EXPORT_VIEW_PROPERTY(onFirstFrameRender, RCTDirectEventBlock)

RCT_EXTERN_METHOD(enterFullscreen:(nonnull NSNumber *)reactTag)
RCT_EXTERN_METHOD(exitFullscreen:(nonnull NSNumber *)reactTag)
RCT_EXTERN_METHOD(startPictureInPicture:(nonnull NSNumber *)reactTag)
RCT_EXTERN_METHOD(stopPictureInPicture:(nonnull NSNumber *)reactTag)

@end

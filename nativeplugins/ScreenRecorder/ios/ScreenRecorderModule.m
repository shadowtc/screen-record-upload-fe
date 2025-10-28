//
//  ScreenRecorderModule.m
//  ScreenRecorder
//
//  iOS stub implementation - to be implemented with ReplayKit
//

#import "ScreenRecorderModule.h"

@implementation ScreenRecorderModule

UNI_EXPORT_METHOD(@selector(startRecord:callback:))
UNI_EXPORT_METHOD(@selector(stopRecord:))

- (void)startRecord:(NSDictionary *)options callback:(UniModuleKeepAliveCallback)callback {
    // TODO: Implement using ReplayKit (RPScreenRecorder)
    // This is a placeholder stub for future iOS implementation
    
    NSDictionary *result = @{
        @"success": @NO,
        @"message": @"Screen recording not yet implemented for iOS. Use ReplayKit in future implementation."
    };
    
    callback(result, YES);
}

- (void)stopRecord:(UniModuleKeepAliveCallback)callback {
    // TODO: Implement using ReplayKit (RPScreenRecorder)
    // This is a placeholder stub for future iOS implementation
    
    NSDictionary *result = @{
        @"success": @NO,
        @"message": @"Screen recording not yet implemented for iOS."
    };
    
    callback(result, YES);
}

@end

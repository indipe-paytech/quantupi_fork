#import "QuantupiPlugin.h"
#if __has_include(<quantupi/quantupi-Swift.h>)
#import <quantupi/quantupi-Swift.h>
#else
// Support project import fallback if the generated compatibility header
// is not copied when this plugin is created as a library.
// https://forums.swift.org/t/swift-static-libraries-dont-copy-generated-objective-c-header/19816
#import "quantupi-Swift.h"
#endif

@implementation QuantupiPlugin
+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
  [SwiftQuantupiPlugin registerWithRegistrar:registrar];
}
@end

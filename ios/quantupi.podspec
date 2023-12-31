#
# To learn more about a Podspec see http://guides.cocoapods.org/syntax/podspec.html.
# Run `pod lib lint quantupi.podspec` to validate before publishing.
#
Pod::Spec.new do |s|
  s.name             = 'quantupi'
  s.version          = '0.0.1'
  s.summary          = 'UPI Integration Package'
  s.description      = <<-DESC
UPI Integration Package
                       DESC
  s.homepage         = 'https://github.com/quantbugtechnology/quantpay.git'
  s.license          = { :file => '../LICENSE' }
  s.author           = { 'Quantbug' => 'info@quantbug.in' }
  s.source           = { :path => '.' }
  s.source_files = 'Classes/**/*'
  s.dependency 'Flutter'
  s.platform = :ios, '9.0'

  # Flutter.framework does not contain a i386 slice.
  s.pod_target_xcconfig = { 'DEFINES_MODULE' => 'YES', 'EXCLUDED_ARCHS[sdk=iphonesimulator*]' => 'i386' }
  s.swift_version = '5.0'
end

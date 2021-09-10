# coding: utf-8
require "json"

package = JSON.parse(File.read(File.join(__dir__, "package.json")))

Pod::Spec.new do |s|
  s.name              = "react-native-neteaseim"
  s.version           = package['version']
  s.summary           = "A React component for netease im."
  s.homepage          = "https://github.com/ybwdaisy"
  s.requires_arc      = true
  s.license           = "None"
  s.author            = {"ybwdaisy" => "ybw_daisy@163.com"}
  s.platform          = :ios , "8.0"
  s.source            = { :git => 'https://github.com/ybwdaisy/react-native-netease-im.git' }
  s.source_files      = "**/*.{h,m}"

  s.dependency 'React-Core'
  s.dependency "NIMSDK", "7.0.3"
  
end

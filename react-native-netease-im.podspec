require "json"

package = JSON.parse(File.read(File.join(__dir__, "package.json")))

Pod::Spec.new do |s|
  s.name         = "react-native-netease-im"
  s.version      = package["version"]
  s.summary      = package["description"]
  s.description  = <<-DESC
                  react-native-netease-im
                   DESC
  s.homepage     = "https://github.com/ybwdaisy/react-native-netease-im"
  s.license      = "MIT"
  s.authors      = { "ybwdaisy" => "ybwdaisy@gmail.com" }
  s.platforms    = { :ios => "9.0" }
  s.source       = { :git => "https://github.com/ybwdaisy/react-native-netease-im.git", :tag => "#{s.version}" }

  s.source_files = "ios/**/*.{h,c,m,swift}"
  s.requires_arc = true

  s.dependency "React"
  s.dependency "NIMSDK_LITE", "9.17.0"
end


import SwiftUI

@main
struct WxRegionApp: App {
    var body: some Scene {
        WindowGroup {
            Text("iOS 无法修改微信客户端资料。\n请在微信：我 → 头像 → 地区 中手动设置。")
                .multilineTextAlignment(.center)
                .padding()
        }
    }
}

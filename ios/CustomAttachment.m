//
//  CustomAttachment.m
//  react-native-netease-im
//
//  Created by ybwdaisy on 2024/8/28.
//

#import "CustomAttachment.h"

@implementation CustomAttachment

- (NSString *)encodeAttachment {

    NSDictionary *dict = [[NSDictionary alloc] init];

    NSString *type = @"unknown";
    if (self.type == CustomMessageTypeCustom) {
        type = @"custom";
    }

    [dict setValue:type forKey:@"type"];
    [dict setValue:self.data forKey:@"data"];

    NSData *jsonData = [NSJSONSerialization dataWithJSONObject:dict options:0 error:nil];
    NSString *content = nil;
    if (jsonData) {
        content = [[NSString alloc] initWithData:jsonData encoding:NSUTF8StringEncoding];
    }
    return content;
}

@end

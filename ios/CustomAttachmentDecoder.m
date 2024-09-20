//
//  CustomAttachmentDecoder.m
//  react-native-netease-im
//
//  Created by ybwdaisy on 2024/8/28.
//

#import "CustomAttachmentDecoder.h"

@implementation CustomAttachmentDecoder

- (nullable id<NIMCustomAttachment>)decodeAttachment:(nullable NSString *)content {
    NSData *data = [content dataUsingEncoding:NSUTF8StringEncoding];

    id<NIMCustomAttachment> attachment;

    if (data) {
        NSDictionary *dict = [NSJSONSerialization JSONObjectWithData:data options:0 error:nil];

        if ([dict isKindOfClass:[NSDictionary class]]) {
            NSString *type = [dict objectForKey:@"type"];
            NSString *data = [dict objectForKey:@"data"];

            CustomMessageType customType = CustomMessageTypeUnknown;
            if ([type isEqual:@"custom"]) {
                customType = CustomMessageTypeCustom;
            }

            CustomAttachment *customAttachment = [[CustomAttachment alloc] init];
            customAttachment.type = customType;
            customAttachment.data = data;

            attachment = customAttachment;
        }
    }

    return attachment;
}

@end

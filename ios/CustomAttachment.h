//
//  CustomAttachment.h
//  react-native-netease-im
//
//  Created by ybwdaisy on 2024/8/28.
//

#import <Foundation/Foundation.h>
#import <NIMSDK/NIMSDK.h>

typedef NS_ENUM(NSInteger, CustomMessageType)
{
    CustomMessageTypeCustom = 1,
    CustomMessageTypeUnknown = 100
};

@interface CustomAttachment : NSObject <NIMCustomAttachment>

@property (nonatomic, assign) CustomMessageType *type;
@property (nonatomic, strong) NSDictionary *data;

@end

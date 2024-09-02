import { NativeModules } from 'react-native';
const { RNNeteaseIm } = NativeModules;

class NimFriend {
  static isMyFriend() {
    return RNNeteaseIm.isMyFriend();
  }
}

export default NimFriend;

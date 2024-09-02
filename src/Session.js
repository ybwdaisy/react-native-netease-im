import { NativeModules } from 'react-native';
const { RNNeteaseIm } = NativeModules;

class NimSession {
  static login(accid, token) {
    return RNNeteaseIm.login(accid, token);
  }

  static logout() {
    return RNNeteaseIm.logout();
  }

  static startSession(sessionId, type = '0') {
    return RNNeteaseIm.startSession(sessionId, type);
  }

  static stopSession() {
    return RNNeteaseIm.stopSession();
  }

  static sendTextMessage(content) {
    return RNNeteaseIm.sendTextMessage(content);
  }

  static sendImageMessage(file, displayName) {
    return RNNeteaseIm.sendImageMessage(file, displayName);
  }

  static sendCustomMessage(attachment) {
    return RNNeteaseIm.sendCustomMessage(attachment);
  }

  static updateCustomMessage(messageId, attachment) {
    return RNNeteaseIm.updateCustomMessage(messageId, attachment);
  }

  static resendMessage(messageId) {
    return RNNeteaseIm.resendMessage(messageId);
  }

  static deleteMessage(messageId) {
    return RNNeteaseIm.deleteMessage(messageId);
  }

  static clearMessage() {
    return RNNeteaseIm.clearMessage();
  }

  static getTotalUnreadCount() {
    return RNNeteaseIm.getTotalUnreadCount();
  }

  static clearAllUnreadCount() {
    return RNNeteaseIm.clearAllUnreadCount();
  }

  static queryMessageListEx(messageId, limit) {
    return RNNeteaseIm.queryMessageListEx(messageId, limit)
  }

  static queryRecentContacts() {
    return RNNeteaseIm.queryRecentContacts();
  }

  static deleteRecentContact(sessionId) {
    return RNNeteaseIm.deleteRecentContact(sessionId);
  }

}

export default NimSession;
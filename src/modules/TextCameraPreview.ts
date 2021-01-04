import {requireNativeComponent, ViewStyle} from 'react-native';

interface TextCameraPreview {
  style: ViewStyle;
  onRecognize: Function;
}

const nativeComponent = requireNativeComponent<TextCameraPreview>(
  'EduTextCameraPreview',
);

export default nativeComponent;

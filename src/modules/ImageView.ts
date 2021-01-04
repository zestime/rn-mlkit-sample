import {requireNativeComponent, ViewStyle} from 'react-native';

interface ImageView {
  src: any;
  style: ViewStyle;
  borderRadius?: number;
  resizeMode?: 'cover' | 'contain' | 'stretch';
}

const nativeComponent = requireNativeComponent<ImageView>('KHImageView');

export default nativeComponent;
